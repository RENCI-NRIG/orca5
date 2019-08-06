/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.container;

import java.util.Hashtable;
import java.util.Properties;

import orca.shirako.common.delegation.SharpResourceTicketFactory;
import orca.shirako.common.delegation.SimpleResourceTicketFactory;
import orca.shirako.container.api.IOrcaConfiguration;
import orca.util.ID;
import orca.util.PropList;

/**
 * <code>ContainerConfiguration</code> holds configuration information for the
 * shirako container. It consists of a properties list and an object table.
 * Properties and object can be registered and retrieved using a given key. Some
 * configuration properties are populated from the container configuration file.
 * Other properties are added at runtime as the system executes.
 * @author aydan
 */
public class OrcaConfiguration implements IOrcaConfiguration
{
    /**
     * Are we running under emulation: true|false.
     */
    public static final String Emulation = "emulation";

    /**
     * Are we using comet
     */
    public static final String CometHost = "comethost";

    /**
     * CA Cert for Comet
     */
    public static final String CometCaCert = "comet.cacert";

    /**
     * Client Cert KeyStore for Comet
     */
    public static final String CometClientKeyStore = "comet.clientkeystore";

    /**
     * Client Cert KeyStore Password for Comet
     */
    public static final String CometClientKeyStorePwd = "comet.clientkeystorepwd";

    /**
     * Client Cert for Comet
     */
    public static final String CometClientCert = "comet.clientcert";

    /**
     * Client Cert key for Comet
     */
    public static final String CometClientKey = "comet.clientkey";

    /**
     * Slice Name Regex
     */
    public static final String SliceNameRegex = "orca.slicename.regex";

    /**
     * Are we using secure communication with the node agent service: true|false.
     */
    public static final String PropertySecureCommunication = "secure.communication";

    /**
     * Are we using secure communication between actors (for protocols that support one): true|false.
     */
    public static final String PropertySecureActorCommunication = "secure.communication.actors";
    /**
     * Axis2 client repository
     */
    public static final String Axis2Repository = "axis2.repository";

    /**
     * Node agent service URI.
     */
    public static final String NaUri = "na.uri";

    /**
     * Node agent service protocol.
     */
    public static final String NaProtocol = "na.protocol";

    /**
     * Node agent service port number.
     */
    public static final String NaPort = "na.port";
    /**
     * Ticket factory class.
     */
    public static final String TicketFactoryClass = "ticket.factory.class";
    /**
     * Container GUID.
     */
    public static final String PropertyContainerGuid = "container.guid";
    public static final String PropertyTimeStartTime = "time.startTime";
    public static final String PropertyTimeCycleMillis = "time.cycleMillis";
    public static final String PropertyTimeManual = "time.manual";
        
    /**
     * Properties list.
     */
    protected Properties properties;
    
    /**
     * Objects table.
     */
    protected Hashtable<String, Object> objects;

    /**
     * If true, the system is running under emulation.
     */
    protected boolean emulation = false;
    protected boolean secureCommunication = true;
    protected boolean secureActorCommunication = true;
    protected String axis2ClientRepository;
    protected String nodeAgentPortNumber = "6";
    protected String nodeAgentProtocol = "http";
    protected String nodeAgentUri = "/axis2/services/NodeAgentService";
    protected String cometHost;
    protected String cometCaCert;
    protected String cometClientCertKeyStore;
    protected String cometClientCertKeyStorePwd;
    protected String cometClientCert;
    protected String cometClientKey;
    protected String sliceNameRegex;


    /**
     * Class name for the ticket factory implementation.
     */
    protected String ticketFactoryClassName = SimpleResourceTicketFactory.class.getCanonicalName();
    protected ID containerGuid = null;
    protected long timeStart = -1;
    protected long cycleMillis = 1000;
    protected boolean manualTime = false;
    
    /**
     * Creates a new instance.
     * @param p properties list
     */
    public OrcaConfiguration(Properties p)
    {
    	this.properties = new Properties();
        this.objects = new Hashtable<String, Object>();

        PropList.mergeProperties(getDefaults(), properties);
        PropList.mergeProperties(p,  properties);
        populate();
    }

    private Properties getDefaults() {
    	Properties p = new Properties();
    	
    	p.setProperty(PropertyTimeStartTime, "-1");
    	p.setProperty(PropertyTimeCycleMillis, "1000");
    	p.setProperty(PropertyTimeManual, "false");
    	

    	p.setProperty(PropertyContainerManagerClass, OrcaContainer.class.getName());
    	p.setProperty(TicketFactoryClass, SharpResourceTicketFactory.class.getName());
    	
    	return p;
    }
    
    public String getAxis2ClientRepository()
    {
        return axis2ClientRepository;
    }

    public String getNodeAgentPortNumber()
    {
        return nodeAgentPortNumber;
    }

    public String getNodeAgentProtocol()
    {
        return nodeAgentProtocol;
    }

    public String getNodeAgentServiceUrl(String address)
    {
        return nodeAgentProtocol + "://" + address + ":" + nodeAgentPortNumber + nodeAgentUri;
    }

    public String getNodeAgentUri()
    {
        return nodeAgentUri;
    }

    public String getCometHost()
    {
        return cometHost;
    }

    public String getCometCaCert()
    {
        return cometCaCert;
    }

    public String getCometClientCertKeyStore()
    {
        return cometClientCertKeyStore;
    }

    public String getCometClientCertKeyStorePwd()
    {
        return cometClientCertKeyStorePwd;
    }

    public String getCometClientCert()
    {
        return cometClientCert;
    }

    public String getCometClientKey()
    {
        return cometClientKey;
    }

    public String getSliceNameRegex() 
    { 
        return sliceNameRegex; 
    }


    /**
     * Returns the object stored under the given key.
     * @param key key
     * @return object stored under the given key
     */
    public synchronized Object getObject(String key)
    {
        Object result = null;

        if (key != null) {
            result = objects.get(key);
        }

        return result;
    }

    /**
     * Returns the value of the property with the given key.
     * @param key key
     * @return value of the property with the given key
     */
    public synchronized String getProperty(String key)
    {
        String result = null;

        if (key != null) {
            result = properties.getProperty(key);
        }

        return result;
    }

    public boolean isEmulation()
    {
        return emulation;
    }


    public boolean isSecureCommunication()
    {
        return secureCommunication;
    }

    public boolean isSecureActorCommunication()
    {
    	return secureCommunication;
    }
    
    protected void populate()
    {
        try {
            if (properties.containsKey(Emulation)) {
                boolean temp = PropList.getBooleanProperty(properties, Emulation);
                emulation = temp;
            }
        } catch (Exception e) {
            Globals.Log.error(e);
            Globals.Log.warn("could not read value for emulation property. Setting emulation to false");
            emulation = false;
        }

        try {
            if (properties.containsKey(PropertySecureCommunication)) {
                boolean temp = PropList.getBooleanProperty(properties, PropertySecureCommunication);
                secureCommunication = temp;
            }
        } catch (Exception e) {
            Globals.Log.error(e);
            Globals.Log.warn("could not read value for secure.communication property. Setting secureCommunication to true");
            secureCommunication = true;
        }

        try {
            if (properties.containsKey(PropertySecureActorCommunication)) {
                boolean temp = PropList.getBooleanProperty(properties, PropertySecureActorCommunication);
                secureActorCommunication = temp;
            }
        } catch (Exception e) {
            Globals.Log.error(e);
            Globals.Log.warn("could not read value for secure.communication.actor property. Setting secureActorCommunication to true");
            secureActorCommunication = true;
        }

        if (properties.containsKey(OrcaConfiguration.Axis2Repository)) {
            String dummy = properties.getProperty(OrcaConfiguration.Axis2Repository);

            if (dummy.startsWith("/")) {
                axis2ClientRepository = dummy;
            } else {
                axis2ClientRepository = Globals.HomeDirectory + "/" + dummy;
            }
        } else {
            axis2ClientRepository = Globals.HomeDirectory + "/axis2repository";
        }

        Globals.Log.info("axis2ClientRepo=" + axis2ClientRepository);
        
        if (properties.containsKey(OrcaConfiguration.NaPort)) {
            nodeAgentPortNumber = properties.getProperty(OrcaConfiguration.NaPort);
        }

        if (properties.containsKey(OrcaConfiguration.NaProtocol)) {
            nodeAgentProtocol = properties.getProperty(OrcaConfiguration.NaProtocol);
        }

        if (properties.containsKey(OrcaConfiguration.NaUri)) {
            nodeAgentUri = properties.getProperty(OrcaConfiguration.NaUri);
        }

        if (properties.containsKey(OrcaConfiguration.CometHost)) {
            cometHost = properties.getProperty(OrcaConfiguration.CometHost);
        }

        if (properties.containsKey(OrcaConfiguration.CometCaCert)) {
            cometCaCert = properties.getProperty(OrcaConfiguration.CometCaCert);
        }

        if (properties.containsKey(OrcaConfiguration.CometClientKeyStore)) {
            cometClientCertKeyStore = properties.getProperty(OrcaConfiguration.CometClientKeyStore);
        }

        if (properties.containsKey(OrcaConfiguration.CometClientKeyStorePwd)) {
            cometClientCertKeyStorePwd = properties.getProperty(OrcaConfiguration.CometClientKeyStorePwd);
        }

        if (properties.containsKey(OrcaConfiguration.CometClientCert)) {
            cometClientCert = properties.getProperty(OrcaConfiguration.CometClientCert);
        }

        if (properties.containsKey(OrcaConfiguration.CometClientKey)) {
            cometClientKey = properties.getProperty(OrcaConfiguration.CometClientKey);
        }

        if(properties.contains(OrcaConfiguration.SliceNameRegex)) {
            sliceNameRegex = properties.getProperty(OrcaConfiguration.SliceNameRegex);
        }

        if (properties.containsKey(TicketFactoryClass)){
        	ticketFactoryClassName = properties.getProperty(TicketFactoryClass); 
        }
        Globals.Log.debug("Ticket factory class: " + ticketFactoryClassName);
        
        String temp = properties.getProperty(PropertyContainerGuid);
        if (temp != null) {
            temp = temp.trim();
            if (temp.length() == 0){
                temp = null;
            }
        }
        
        if (temp == null) {
            containerGuid = new ID();
        } else {
            containerGuid = new ID(temp);
        }
        
        processTime();
    }
    
    protected void processTime() {
        try {
            if (properties.containsKey(PropertyTimeStartTime)) {
                timeStart = PropList.getLongProperty(properties, PropertyTimeStartTime);
            }
        } catch (Exception e) {
            Globals.Log.warn("could not process " + PropertyTimeStartTime);
            Globals.Log.error(e);
            timeStart = -1;
        }
        
        try {
            if (properties.containsKey(PropertyTimeCycleMillis)) {
                cycleMillis = PropList.getLongProperty(properties, PropertyTimeCycleMillis);
            }
        } catch (Exception e) {
            Globals.Log.error(e);
            Globals.Log.warn("could not process " + PropertyTimeCycleMillis);
            cycleMillis = 1000;
        }
           
        try {
            if (properties.containsKey(PropertyTimeManual)){
                manualTime = PropList.getBooleanProperty(properties, PropertyTimeManual);
            }
        } catch (Exception e){
            Globals.Log.error(e);
            Globals.Log.warn("could not process " + PropertyTimeManual);
            manualTime = false;
        }
    }
    
    /**
     * Stores the object under the given key.
     * @param key key
     * @param value value
     */
    public synchronized void setObect(String key, Object value)
    {
        if ((key != null) && (value != null)) {
            objects.put(key, value);
        }
    }

    /**
     * Sets the value of the property with the given key. If the property is
     * already set, it will be overwritten.
     * @param key key
     * @param value value
     */
    public synchronized void setProperty(String key, String value)
    {
        if ((key != null) && (value != null)) {
            properties.setProperty(key, value);
        }
    }

    /**
     * Returns the ticket factory class name.
     * @return the ticket factory class name
     */
    public String getTicketFactoryClassName()
    {
    	return ticketFactoryClassName;    	
    }
    
    public ID getContainerGUID() {
        return containerGuid;
    }
    
    public long getTimeStart() {
        return timeStart;
    }
    
    public long getCycleMillis() {
        return cycleMillis;
    }
    
    public boolean getManualTime() {
        return manualTime;
    }
}
