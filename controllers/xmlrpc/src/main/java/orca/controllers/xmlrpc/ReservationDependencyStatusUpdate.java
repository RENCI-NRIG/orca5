package orca.controllers.xmlrpc;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import orca.controllers.OrcaController;
import orca.controllers.xmlrpc.statuswatch.IStatusUpdateCallback;
import orca.embed.policyhelpers.StringProcessor;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConverter;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.UnitMng;
import orca.shirako.common.ReservationID;
import orca.shirako.common.meta.UnitProperties;

public class ReservationDependencyStatusUpdate implements IStatusUpdateCallback<ReservationID> {

    ReservationMng reservation = null;

    @Override
    public void success(List<ReservationID> ok, List<ReservationID> actOn) throws StatusCallbackException {

        final Logger logger = OrcaController.getLogger(this.getClass().getSimpleName());

        String parent_prefix = UnitProperties.UnitEthPrefix;
        String host_interface = null;
        String reservation_id = null;
        if (reservation != null)
            reservation_id = reservation.getReservationID();
        if (actOn == null || !(reservation_id.equals(actOn.get(0).toString()))) {
            logger.debug("Empty modify...." + actOn + "; reservation=" + reservation_id);
            return;
        }

        IOrcaServiceManager sm = null;
        try {
            sm = XmlrpcOrcaState.getInstance().getSM();
            logger.debug("SUCCESS ON MODIFY WATCH OF " + ok);
            // ok-parents
            // actOn-to be modified reservation
            String modifySubcommand = ModifyHelper.ModifySubcommand.ADDIFACE.getName();

            // use the queueing version to avoid collisions with modified performed by the controller itself
            Properties local = OrcaConverter.fill(reservation.getLocalProperties());
            String p_str = local.getProperty(ReservationConverter.PropertyNumNewParentReservations);
            String e_p_str = local.getProperty(ReservationConverter.PropertyNumExistParentReservations);
            String num_interface = local.getProperty(ReservationConverter.PropertyParentNumInterface);
            String num_storage = local.getProperty(ReservationConverter.PropertyParentNumStorage);
            int num_interface_int = 0, num_storage_int = 0;
            if (num_interface != null)
                num_interface_int = Integer.valueOf(num_interface);
            if (num_storage != null)
                num_storage_int = Integer.valueOf(num_storage);

            Properties pr_local = null, pr_u = null;
            int p = 0;
            String p_r_id = null, isNetwork = null, isLun = null;
            ReservationMng p_r = null;
            List<String> r_id_list = new LinkedList<String>();
            String unit_tag = null, unit_parent_url = null;

            if (p_str != null) {
                p = Integer.valueOf(p_str);
                logger.debug("Number of parent reservations:" + p + ";num_interface=" + num_interface_int
                        + ";num_storage=" + num_storage_int);

                for (int i = 0; i < p; i++) {
                    String key = ReservationConverter.PropertyNewParent + String.valueOf(i);
                    p_r_id = local.getProperty(key);
                    r_id_list.add(p_r_id);
                }
            }
            if (e_p_str != null) {
                p = Integer.valueOf(e_p_str);
                logger.debug("Number of existing parent reservations:" + p + ";num_interface=" + num_interface_int
                        + ";num_storage=" + num_storage_int);
                for (int i = 0; i < p; i++) {
                    String key = ReservationConverter.PropertyExistParent + String.valueOf(i);
                    p_r_id = local.getProperty(key);
                    r_id_list.add(p_r_id);
                }
            }
            for (String r_id : r_id_list) {
                // Creating modifyProperties here to ensure modifyProperties contains data for only one reservation
                Properties modifyProperties = new Properties();
                if (r_id != null) {
                    p_r = sm.getReservation(new ReservationID(r_id));
                    pr_local = OrcaConverter.fill(p_r.getLocalProperties());
                    if (p_r != null) {
                        isNetwork = pr_local.getProperty(ReservationConverter.PropertyIsNetwork);
                        isLun = pr_local.getProperty(ReservationConverter.PropertyIsLUN);

                        if (isNetwork != null && isNetwork.equals("1")) { // Parent is a networking reservation
                            // seems it's possible for this to not produce a tag. Doing this in a loop with a fixed
                            // number of repetitions
                            int n = 10;
                            // while ((n-->0) && (unit_tag == null)) {
                            List<UnitMng> un = sm.getUnits(new ReservationID(p_r.getReservationID()));
                            if (un != null) {
                                for (UnitMng u : un) {
                                    pr_u = OrcaConverter.fill(u.getProperties());
                                    if (pr_u.getProperty(UnitProperties.UnitVlanTag) != null)
                                        unit_tag = pr_u.getProperty(UnitProperties.UnitVlanTag);
                                    if (pr_u.getProperty(UnitProperties.UnitVlanUrl) != null)
                                        unit_parent_url = pr_u.getProperty(UnitProperties.UnitVlanUrl);

                                }
                            }
                            // if (unit_tag == null)
                            // Thread.sleep(1000L);
                            // }
                            logger.debug("parent r_id=" + r_id + ";unit tag:" + unit_tag + ";unit_parent_url:"
                                    + unit_parent_url + ";n=" + n);
                            if (unit_tag != null) {
                                host_interface = StringProcessor.getHostInterface(local, unit_parent_url);
                                if (host_interface == null) {
                                    logger.debug("Unable to find the parent interface index:unit_tag=" + unit_tag);
                                    continue;
                                }

                                Properties ethModifyProperties = OrcaXmlrpcHandler.getNetworkEthModifyProperties(
                                        parent_prefix, host_interface, unit_tag, local, logger);
                                modifyProperties.putAll(ethModifyProperties);

                                // Save unit.number.interface used later to build local interface properties in ReservationConverter.java
                                logger.debug("KOMAL1 num_interface=" + num_interface + " num_interface_int=" + num_interface_int);
                                modifyProperties.setProperty(ReservationConverter.PropertyParentNumInterface,String.valueOf(num_interface));
                                logger.debug("modifycommand:" + modifySubcommand + ":properties:"
                                        + modifyProperties.toString());
                                ModifyHelper.enqueueModify(reservation_id.toString(), modifySubcommand,
                                        modifyProperties);
                            } else { // no need to go futher
                                logger.debug("Parent did not return the unit tag:" + pr_u);
                                continue;
                            }
                        }
                        // parent is lun
                        if (isLun != null && isLun.equals("1")) { // Parent is a storage reservation
                            // seems it's possible for this to not produce a tag. Doing this in a loop with a fixed
                            // number of repetitions
                            int n = 10;
                            // while ((n-->0) && (unit_tag == null)) {
                            List<UnitMng> un = sm.getUnits(new ReservationID(p_r.getReservationID()));
                            if (un != null) {
                                for (UnitMng u : un) {
                                    pr_u = OrcaConverter.fill(u.getProperties());
                                    if (pr_u.getProperty(UnitProperties.UnitLUNTag) != null)
                                        unit_tag = pr_u.getProperty(UnitProperties.UnitLUNTag);
                                }
                            }
                            // if (unit_tag == null)
                            // Thread.sleep(1000L);
                            // }

                            String parent_url = StringProcessor.getParentURL(local, pr_local);
                            logger.debug("isLun=" + isLun + ";parent unit lun tag:" + unit_tag + ";r_id=" + r_id
                                    + ";p_r_id=" + p_r.getReservationID() + ";parent_url=" + parent_url);
                            if (unit_tag != null) {
                                modifyProperties.setProperty(UnitProperties.UnitTargetLun, unit_tag);
                                if (parent_url != null)
                                    modifyProperties.setProperty(UnitProperties.UnitEthParentUrl, parent_url);
                                host_interface = StringProcessor.getHostInterface(local, p_r);
                                if (host_interface == null) {
                                    logger.debug("Unable to find the parent interace index:unit_tag=" + unit_tag);
                                    continue;
                                }
                                String parent_tag_name = parent_prefix.concat(host_interface)
                                        .concat(UnitProperties.UnitEthVlanSuffix);

                                if (local.getProperty(parent_tag_name) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitEthVlan,
                                            local.getProperty(parent_tag_name));

                                Properties ethModifyProperties = OrcaXmlrpcHandler.getEthModifyProperties(parent_prefix,
                                        host_interface, local, logger);
                                modifyProperties.putAll(ethModifyProperties);

                                String storagePrefix = UnitProperties.UnitStoragePrefix
                                        + String.valueOf(num_storage_int);
                                // String storageTargetPrefix = storagePrefix + ".target";
                                // String storageFSPrefix = storagePrefix + ".fs";

                                // Other properties
                                if (local.getProperty(UnitProperties.UnitISCSIInitiatorIQN) != null)
                                    modifyProperties.setProperty(UnitProperties.ISCSIInitiatorIQN,
                                            local.getProperty(UnitProperties.UnitISCSIInitiatorIQN));

                                if (local.getProperty(storagePrefix + UnitProperties.UnitTargetIPSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitTargetIP,
                                            local.getProperty(storagePrefix + UnitProperties.UnitTargetIPSuffix));

                                if (local.getProperty(storagePrefix + UnitProperties.UnitTargetLunGuid) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitTargetLunGuid,
                                            local.getProperty(storagePrefix + UnitProperties.UnitTargetLunGuid));

                                if (local.getProperty(storagePrefix + UnitProperties.UnitFSTypeSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitFSType,
                                            local.getProperty(storagePrefix + UnitProperties.UnitFSTypeSuffix));
                                else
                                    modifyProperties.setProperty(UnitProperties.UnitFSType,
                                            UnitProperties.DefaultFSType);

                                if (local.getProperty(storagePrefix + UnitProperties.UnitFSOptionsSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitFSOptions,
                                            local.getProperty(storagePrefix + UnitProperties.UnitFSOptionsSuffix));
                                else
                                    modifyProperties.setProperty(UnitProperties.UnitFSOptions,
                                            UnitProperties.DefaultFSOptions);

                                if (local.getProperty(storagePrefix + UnitProperties.UnitFSMountPointSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitFSMountPoint,
                                            local.getProperty(storagePrefix + UnitProperties.UnitFSMountPointSuffix));
                                else
                                    modifyProperties.setProperty(UnitProperties.UnitFSMountPoint,
                                            UnitProperties.DefaultMountPoint + String.valueOf(host_interface));

                                if (local.getProperty(storagePrefix + UnitProperties.UnitFSShouldFormatSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitFSShouldFormat,
                                            local.getProperty(storagePrefix + UnitProperties.UnitFSShouldFormatSuffix));
                                else
                                    modifyProperties.setProperty(UnitProperties.UnitFSShouldFormat,
                                            ReservationConverter.SUDO_NO);

                                if (local.getProperty(storagePrefix + UnitProperties.UnitTargetChapUserSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitTargetChapUser,
                                            local.getProperty(storagePrefix + UnitProperties.UnitTargetChapUserSuffix));
                                if (local
                                        .getProperty(storagePrefix + UnitProperties.UnitTargetChapSecretSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitTargetChapSecret, local
                                            .getProperty(storagePrefix + UnitProperties.UnitTargetChapSecretSuffix));

                                if (local.getProperty(storagePrefix + UnitProperties.UnitStoreTypeSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitStoreType,
                                            local.getProperty(storagePrefix + UnitProperties.UnitStoreTypeSuffix));
                                else
                                    modifyProperties.setProperty(UnitProperties.UnitStoreType,
                                            UnitProperties.DefaultBlockDeviceType);
                                if (local.getProperty(storagePrefix + UnitProperties.UnitTargetPortSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitTargetPort,
                                            local.getProperty(storagePrefix + UnitProperties.UnitTargetPortSuffix));
                                else
                                    modifyProperties.setProperty(UnitProperties.UnitTargetPort,
                                            UnitProperties.DefaultISCSIPort);

                                if (local.getProperty(
                                        storagePrefix + UnitProperties.UnitTargetShouldAttachSuffix) != null)
                                    modifyProperties.setProperty(UnitProperties.UnitTargetShouldAttach, local
                                            .getProperty(storagePrefix + UnitProperties.UnitTargetShouldAttachSuffix));
                                else
                                    modifyProperties.setProperty(UnitProperties.UnitTargetShouldAttach,
                                            ReservationConverter.SUDO_YES);

                                num_storage_int--;
                                logger.debug("modifycommand:" + modifySubcommand + ":properties:"
                                        + modifyProperties.toString());
                                ModifyHelper.enqueueModify(reservation_id.toString(), modifySubcommand,
                                        modifyProperties);
                            } else { // no need to go futher
                                logger.debug("Parent did not return the unit lun tag:" + pr_u);
                                continue;
                            }
                        }
                    }
                }
            }

            logger.debug("KOMAL2 num_interface=" + num_interface + " num_interface_int=" + num_interface_int);
            local.setProperty(ReservationConverter.PropertyParentNumInterface, String.valueOf(num_interface));
            reservation.setLocalProperties(OrcaConverter.merge(local, reservation.getLocalProperties()));

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (sm != null)
                XmlrpcOrcaState.getInstance().returnSM(sm);
        }

    }

    @Override
    public void failure(List<ReservationID> failed, List<ReservationID> ok, List<ReservationID> actOn)
            throws StatusCallbackException {
        final Logger logger = OrcaController.getLogger(this.getClass().getSimpleName());
        logger.debug("FAILURE ON MODIFY WATCH OF " + failed);
    }

    public ReservationMng getReservation() {
        return reservation;
    }

    public void setReservation(ReservationMng reservation) {
        this.reservation = reservation;
    }
}
