package orca.controllers.xmlrpc;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import orca.controllers.xmlrpc.statuswatch.IStatusUpdateCallback;
import orca.controllers.xmlrpc.statuswatch.ReservationIDWithModifyIndex;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.ReservationMng;
import orca.shirako.common.ReservationID;
import orca.shirako.common.meta.UnitProperties;
import orca.util.PropList;

/**
 * Class that implements some of the modify logic, including enqueuing/dequeuing modify operations on a per-reservation
 * basis
 * 
 * @author ibaldin
 *
 */
public class ModifyHelper {

    static final Map<ReservationID, Queue<ModifyOperation>> modifyQueues = new HashMap<>();

    // lists known widely-used modify commands
    public enum ModifySubcommand {
        ADDIFACE("addiface"), REMOVEIFACE("removeiface"), SSH("ssh");

        private String name;

        ModifySubcommand(String n) {
            name = n;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Container class for a single modify operation
     * 
     * @author ibaldin
     *
     */
    private static class ModifyOperation {
        private ReservationIDWithModifyIndex resId;
        private String modifySubcommand;
        private Properties modifyProperties;

        public ModifyOperation(String res, int index, String sub, Properties props) {
            resId = new ReservationIDWithModifyIndex(new ReservationID(res.trim()), index);
            modifySubcommand = sub;
            modifyProperties = props;
        }

        public ModifyOperation(String res, String sub, Properties props) {
            resId = new ReservationIDWithModifyIndex(new ReservationID(res.trim()), 0);
            modifySubcommand = sub;
            modifyProperties = props;
        }

        public ModifyOperation(ReservationID res, int index, String sub, Properties props) {
            resId = new ReservationIDWithModifyIndex(res, index);
            modifySubcommand = sub;
            modifyProperties = props;
        }

        public ModifyOperation(ReservationID res, String sub, Properties props) {
            resId = new ReservationIDWithModifyIndex(res, 0);
            modifySubcommand = sub;
            modifyProperties = props;
        }

        ReservationIDWithModifyIndex get() {
            return resId;
        }

        String getSubcommand() {
            return modifySubcommand;
        }

        Properties getProperties() {
            return modifyProperties;
        }

        void overrideIndex(int i) {
            resId.overrideModifyIndex(i);
        }

        public String toString() {
            return modifySubcommand + "/" + resId.getReservationID() + "/" + resId.getModifyIndex();
        }
    }

    /**
     * A callback that operates on the queues and always assumes we modify and watch one reservation at a time.
     * 
     * @author ibaldin
     *
     */
    private static class ModifyQueueCallback implements IStatusUpdateCallback<ReservationIDWithModifyIndex> {

        private static ModifyQueueCallback cbInstance = new ModifyQueueCallback();

        public static ModifyQueueCallback getInstance() {
            return cbInstance;
        }

        public void success(List<ReservationIDWithModifyIndex> ok, List<ReservationID> actOn)
                throws StatusCallbackException {

            checkModifyQueue(ok.get(0));
        }

        public void failure(List<ReservationIDWithModifyIndex> failed, List<ReservationIDWithModifyIndex> ok,
                List<ReservationID> actOn) throws StatusCallbackException {

            checkModifyQueue(failed.get(0));
        }

        private void checkModifyQueue(ReservationIDWithModifyIndex okOrFailed) {

            synchronized (modifyQueues) {
                // dequeue a modify operation
                Queue<ModifyOperation> resQueue = modifyQueues.get(okOrFailed.getReservationID());

                if (resQueue == null)
                    throw new RuntimeException(
                            "checkModifyQueue(): no queue found for " + okOrFailed + ", skipping processing");

                // remove from the top of the queue
                ModifyOperation mop = resQueue.poll();

                if (mop == null) {
                    throw new RuntimeException(
                            "checkModifyQueue: no modify operation found at top of the queue, proceeding");
                }

                if (!mop.get().equals(okOrFailed)) {
                    // bad thing happened - we dequeued a modify operation
                    // that doesn't match expected reservation id
                    throw new RuntimeException("checkModifyQueue dequeued reservation " + mop.get()
                            + " which doesn't match expected " + okOrFailed);
                }

                // launch another modify with same callback, if available
                mop = resQueue.peek();

                if (mop != null) {
                    Integer modIndex = modifySliver(okOrFailed.getReservationID(), mop.getSubcommand(),
                            mop.getProperties());
                    mop.overrideIndex(modIndex);

                    XmlrpcOrcaState.getSUT().addModifyStatusWatch(Collections.singletonList(mop.get()), null,
                            cbInstance);
                } else {
                    modifyQueues.remove(okOrFailed.getReservationID());
                }
            }
        }
    }

    /**
     * Enqueue a modify operation on this reservation id and execute directly, if possible
     * 
     * @param res res
     * @param modifySubcommand modifySubcommand
     * @param modifyProperties modifyProperties
     */
    public static void enqueueModify(String res, String modifySubcommand, Properties modifyProperties) {

        Queue<ModifyOperation> l = null;

        ReservationID resId = new ReservationID(res.trim());
        synchronized (modifyQueues) {

            if (!modifyQueues.containsKey(resId)) {
                l = new LinkedList<ModifyOperation>();
                modifyQueues.put(resId, l);
            } else
                l = modifyQueues.get(resId);

            ModifyOperation mop = new ModifyOperation(resId, 0, modifySubcommand, modifyProperties);
            l.add(mop);

            // launch modify if this is the first operation
            // otherwise the callback will launch it
            if (l.size() == 1) {
                // System.out.println("About to modify
                // sliver:"+resId+"subcommand:"+modifySubcommand+";Properties:"+modifyProperties);
                Integer modIndex = modifySliver(resId, modifySubcommand, modifyProperties);
                mop.overrideIndex(modIndex);

                XmlrpcOrcaState.getSUT().addModifyStatusWatch(Collections.singletonList(mop.get()), null,
                        ModifyQueueCallback.getInstance());
            }
        }
    }

    /**
     * Enqueue a modify operation on this reservation id and execute directly, if possible
     * 
     * @param res res
     * @param modifySubcommand modifySubcommand
     * @param modifyPropertiesList modifyPropertiesList
     */
    public static void enqueueModify(String res, String modifySubcommand, List<Map<String, ?>> modifyPropertiesList) {
        try {
            // here we break up the semantics of different subcommands

            Properties modifyProperties = convertListMapsToProperties(modifySubcommand, modifyPropertiesList);

            enqueueModify(res, modifySubcommand, modifyProperties);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Unable to modify sliver reservation " + res + " due to " + e);
        }
    }

    /**
     * Conversion between map of strings and properties object
     * 
     * @param m m
     * @return properties
     */
    public static Properties fromMap(Map<String, String> m) {
        Properties p = new Properties();

        for (Map.Entry<String, String> e : m.entrySet()) {
            p.setProperty(e.getKey(), e.getValue());
        }
        return p;
    }

    /**
     * Conversion between map of strings and properties object
     * 
     * @param p p
     * @return map of strings
     */
    public static Map<String, String> fromProperties(Properties p) {
        Map<String, String> m = new HashMap<String, String>();

        for (Map.Entry<Object, Object> e : p.entrySet()) {
            m.put((String) e.getKey(), (String) e.getValue());
        }
        return m;
    }

    /**
     * Conversion between properties and map of strings, remove keyPrefix from keynames before inserting into map
     * 
     * @param p p
     * @param keyPrefix keyPrefix
     * @return map of strings
     */
    public static Map<String, String> fromProperties(Properties p, String keyPrefix) {
        Map<String, String> m = new HashMap<String, String>();

        for (Map.Entry<Object, Object> e : p.entrySet()) {
            String key = (String) e.getKey();
            m.put(key.replace((CharSequence) keyPrefix, (CharSequence) ""), (String) e.getValue());
        }
        return m;
    }

    /**
     * Converts list of maps to properties according to modify subcommand
     * 
     * @param morifyPropertiesList modifyPropertiesList
     * @param modifySubcommand modifySubcommand
     * @throws Exception in case of error
     * @return Properties
     */
    @SuppressWarnings("unchecked")
    private static Properties convertListMapsToProperties(String modifySubcommand,
            List<Map<String, ?>> modifyPropertiesList) throws Exception {
        // here we break up the semantics of different subcommands

        Properties modifyProperties = new Properties();
        boolean implementedSubcommand = false;
        //
        // add more subcommands here. make sure to set implementedSubcommand to true.
        //
        if ("ssh".equalsIgnoreCase(modifySubcommand)) {
            implementedSubcommand = true;
            modifyProperties.putAll(ReservationConverter.generateSSHProperties(modifyPropertiesList));
        } else {
            implementedSubcommand = true;
            // collect properties from first list entry map
            if (modifyPropertiesList.size() == 0)
                throw new RuntimeException(
                        "Subcommand " + modifySubcommand + " requires a list maps of size one or more");

            modifyProperties = fromMap((Map<String, String>) modifyPropertiesList.get(0));
        }

        if (!implementedSubcommand)
            throw new RuntimeException("Modify subcommand " + modifySubcommand + " is not implemented");

        return modifyProperties;
    }

    /**
     * Same as modifySliver, but acquires and releases it's own SM instance. Note that it is safer to call enqueueModify
     * 
     * @param res res
     * @param modifySubcommand modifySubcommand
     * @param modifyPropertiesList modifyPropertiesList
     * @return - index of modify operation
     */
    public static Integer modifySliver(ReservationID res, String modifySubcommand,
            List<Map<String, ?>> modifyPropertiesList) {
        IOrcaServiceManager sm = null;

        try {
            sm = XmlrpcOrcaState.getInstance().getSM();
            return modifySliver(sm, res, modifySubcommand, modifyPropertiesList);
        } catch (Exception e) {
            throw new RuntimeException("Unable to modify sliver reservation: " + e);
        } finally {
            if (sm != null)
                XmlrpcOrcaState.getInstance().returnSM(sm);
        }
    }

    /**
     * Same as modifySliver, but acquires and releases it's own SM instance. It is safer to call enqueueModify
     * 
     * @param res res
     * @param modifySubcommand modifySubcommand
     * @param modifyProperties modifyProperties
     * @return integer
     */
    public static Integer modifySliver(ReservationID res, String modifySubcommand, Properties modifyProperties) {
        IOrcaServiceManager sm = null;

        try {
            sm = XmlrpcOrcaState.getInstance().getSM();
            return modifySliver(sm, res, modifySubcommand, modifyProperties);
        } catch (Exception e) {
            throw new RuntimeException("Unable to modify sliver reservation: " + e);
        } finally {
            if (sm != null)
                XmlrpcOrcaState.getInstance().returnSM(sm);
        }
    }

    /**
     * Modify reservation based on reservation id (or null). Note that it is safer to call enqueueModify.
     * 
     * @param sm sm
     * @param res res
     * @param modifySubcommand modifySubcommand
     * @param modifyPropertiesList modifyProperties
     * @return 0 on failure or index of modify operation
     */
    public static Integer modifySliver(IOrcaServiceManager sm, ReservationID res, String modifySubcommand,
            List<Map<String, ?>> modifyPropertiesList) {
        try {
            // here we break up the semantics of different subcommands

            Properties modifyProperties = convertListMapsToProperties(modifySubcommand, modifyPropertiesList);

            return modifySliver(sm, res, modifySubcommand, modifyProperties);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Unable to modify sliver reservation " + res + " due to " + e);
        }
    }

    /**
     * Modify reservation based on reservation id (or null). Note that it is safer to call enqueueModify. Uses
     * Properties instead of List&lt;Map&lt;String,?&gt;&gt;
     * 
     * @param sm sm
     * @param res res
     * @param modifySubcommand modifySubcommand
     * @param modifyProperties modifyProperties
     * @return 0 on failure or index of modify operation
     */
    public static Integer modifySliver(IOrcaServiceManager sm, ReservationID res, String modifySubcommand,
            Properties modifyProperties) {
        try {
            ReservationMng rm = sm.getReservation(res);
            if (rm == null)
                throw new RuntimeException("modifySliver(): Unable to find reservation " + res);

            // FIXME: should we enable this check? /ib 11/2/2016
            // if (rm.getState() != OrcaConstants.ReservationStateActive)
            // throw new RuntimeException("modifySliver(): unable to modify reservation " + res + " that is not active
            // (" + rm.getState() + ")");

            PropertiesMng psmng = rm.getConfigurationProperties();
            if (psmng == null)
                throw new RuntimeException(
                        "modifySliver(): unable to get configuration properties for reservation " + res);

            Properties cp = OrcaConverter.fill(psmng);
            int index = PropList.highestPropIndex(cp, UnitProperties.ModifySubcommandPrefix) + 1;

            // prepend all property names with modify.x.
            PropList.renamePropertyNames(modifyProperties, UnitProperties.ModifyPrefix + index + ".");

            // add the subcommand as a property after everything
            modifyProperties.put(UnitProperties.ModifySubcommandPrefix + index,
                    UnitProperties.ModifyPrefix + modifySubcommand);

            sm.modifyReservation(res, modifyProperties);

            return index;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Unable to modify sliver reservation " + res + " due to " + e);
        }
    }
}
