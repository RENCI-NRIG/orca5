package orca.boot.inventory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import orca.embed.policyhelpers.GenericNDLPoolPropertyExtractor;
import orca.embed.workflow.Domain;
import orca.ndl.DomainResourceType;
import orca.ndl.NdlException;
import orca.policy.core.AuthorityCalendarPolicy;
import orca.policy.core.IResourceControl;
import orca.policy.core.ResourceControl;
import orca.shirako.common.ConfigurationException;
import orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.common.meta.ResourcePoolAttributeType;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.container.DistributedRemoteRegistryCache;
import orca.shirako.container.Globals;
import orca.shirako.container.OrcaContainer;
import orca.shirako.container.RemoteRegistryCache;
import orca.shirako.registry.ActorRegistry;
import orca.util.CompressEncode;
import orca.util.PropList;

public class NdlResourcePoolFactory extends ResourcePoolFactory {
    public static final String PropertySubstrateFile = "substrate.file";

    public static final String PropertyRegistryUrl = "registry.url";
    public static final String PropertyRegistryMethod = "registry.method";

    // the static hashmap of controls to ndl property extractors
    protected static final HashMap<String, String> hmControlToNDLExtractor = new HashMap<String, String>() {
        {
            // Controls for which we are extracting properties
            put("orca.plugins.ben.control.NdlVLANControl",
                    "orca.embed.policyhelpers.VlanPolicyNDLPoolPropertyExtractor");
            put("orca.plugins.ben.control.NdlInterfaceVLANControl",
                    "orca.embed.policyhelpers.VlanPolicyNDLPoolPropertyExtractor");
            put("orca.policy.core.SimpleVMControl", "orca.embed.policyhelpers.VMPolicyNDLPoolPropertyExtractor");
            put("orca.policy.core.VlanControl", "orca.embed.policyhelpers.VlanPolicyNDLPoolPropertyExtractor");
            put("orca.policy.core.LUNControl", "orca.embed.policyhelpers.LUNPolicyNDLPoolPropertyExtractor");
        }
    };

    protected String getAbstractNdl(String resourceType) throws ConfigurationException {

        String abstractModel = " ";
        String fullModel = " ";
        // obtain the substrate file path from the resource pool descriptor
        String substrateFile = desc.getPoolProperties().getProperty(PropertySubstrateFile);
        if ((substrateFile == null) || (substrateFile.length() == 0))
            throw new ConfigurationException("NDL Substrate file not specified");
        // Globals.Log.debug("substrateFile (full rdf) = " + substrateFile);
        Globals.Log.debug("NdlResourcePoolFactory: substrate.file = " + substrateFile);
        try {
            Domain d = new Domain(substrateFile);
            abstractModel = d.delegateDomainModelToString(resourceType);
            fullModel = d.delegateFullModelToString();
        } catch (IOException ex) {
            throw new ConfigurationException("Problem reading full NDL substrate file", ex);
        } catch (NdlException ee) {
            throw new ConfigurationException("Problem creating NDL model", ee);
        }

        // register the abstract ndl model and the full ndl model to the registry
        // Claris: hack to avoid serialization of proxy,f ullmodel and abstractmodel
        if (Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryClass) != null) {
            if (Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryClass)
                    .contains("Distributed")) {
                DistributedRemoteRegistryCache.registerNDLToRegistry(proxy, fullModel, abstractModel);
            } else {
                RemoteRegistryCache.registerNDLToRegistry(proxy, fullModel, abstractModel);
            }
        } else {
            RemoteRegistryCache.registerNDLToRegistry(proxy, fullModel, abstractModel);
        }
        return abstractModel;
    }

    @Override
    protected void updateDescriptor() throws ConfigurationException {
        super.updateDescriptor();
        // calculate the abstract domain model
        // Globals.Log.debug("desc.getResourceTypeLabel() = " + desc.getResourceTypeLabel());
        // Globals.Log.debug("desc.getResourceType() = " + desc.getResourceType().toString());
        String abstractModel = getAbstractNdl(desc.getResourceType().getType());
        // create a new pool attribute to hold the abstract domain
        // in this way the abstract domain will be sent to brokers and service managers when
        // they query the broker about its inventory.
        ResourcePoolAttributeDescriptor attr = new ResourcePoolAttributeDescriptor();
        attr.setType(ResourcePoolAttributeType.NDL);
        attr.setKey(ResourceProperties.ResourceNdlAbstractDomain);
        attr.setValue(CompressEncode.compressEncode(abstractModel));
        attr.setLabel("Abstract Domain NDL");
        desc.addAttribute(attr);
        // Create a new pool attribute called resource.domain based on type name and filename
        // NOTE: filename is converted to lower case when it is made part of domain name
        attr = desc.getAttribute(ResourceProperties.ResourceDomain);
        if (attr != null) {
            Globals.Log.warn(
                    "NdlResourcePoolFactory: Attribute resource.domain found in config.xml file, usually it is better to let ORCA figure it out (i.e. omit it)");
        } else {
            attr = new ResourcePoolAttributeDescriptor();
            attr.setType(ResourcePoolAttributeType.STRING);
            attr.setKey(ResourceProperties.ResourceDomain);
            // At this point substrate file has been checked, so should exist
            attr.setValue(DomainResourceType.generateDomainName(
                    desc.getPoolProperties().getProperty(PropertySubstrateFile), desc.getResourceType().getType()));
            attr.setLabel("NdlResourcePoolFactory: Name of the domain of this resource");
            desc.addAttribute(attr);
            Globals.Log.info("NdlResourcePoolFactory: Added " + attr.getKey() + " attribute " + attr.getValue());
        }

        // Extract all ResourceControls of the current actor and find the ResourceControl for the current resource type

        AuthorityCalendarPolicy pol = (AuthorityCalendarPolicy) ((ActorRegistry.getActor(proxy.getName())).getPolicy());

        Hashtable<IResourceControl, List<String>> htControlTypes = pol.getControlTypes();

        Enumeration e = htControlTypes.keys();
        while (e.hasMoreElements()) {
            ResourceControl rc = (ResourceControl) e.nextElement();
            Globals.Log
                    .info("NdlResourcePoolFactory: Current resource control class name = " + rc.getClass().getName());
            List<String> lResTypes = (List<String>) htControlTypes.get(rc);
            // Iterate through the list of resource types for a given control
            for (Iterator it = lResTypes.iterator(); it.hasNext();) {
                String currType = (String) it.next();
                Globals.Log.debug("NdlResourcePoolFactory: current resource Type =" + currType);
                Globals.Log.debug("NdlResourcePoolFactory: pool resource Type =" + desc.getResourceType().getType());
                if (currType.equalsIgnoreCase(desc.getResourceType().getType())) {
                    // We now know the ResourceControl for the resource type for this pool
                    Globals.Log
                            .debug("NdlResourcePoolFactory: Calling populateAttributesAndProperties for resource Type = "
                                    + desc.getResourceType().getType());
                    populateAttributesAndProperties(desc, rc);
                }
            }
        }

    }

    private void populateAttributesAndProperties(ResourcePoolDescriptor desc, ResourceControl rc) {

        try {

            Globals.Log.info("NdlResourcePoolFactory: Resource control class name = " + rc.getClass().getName());
            // Extract the class name for NDL property extractor from the static map
            String ndlExtractorClassName = hmControlToNDLExtractor.get(rc.getClass().getName());

            if (ndlExtractorClassName == null) {
                // Point to the default genericndlpoolpropertyextractor class
                ndlExtractorClassName = "orca.embed.policyhelpers.GenericNDLPoolPropertyExtractor";
            }

            Class c = Class.forName(ndlExtractorClassName);

            try {

                Constructor<?> cons = c.getConstructor(String.class);
                GenericNDLPoolPropertyExtractor ndlPropExtractor = (GenericNDLPoolPropertyExtractor) cons
                        .newInstance(getAbstractNdl(desc.getResourceType().getType()));
                Properties poolProp = ndlPropExtractor.getPoolProperties();
                if (poolProp == null) {
                    Globals.Log
                            .debug("NdlResourcePoolFactory: No pool properties extracted from ndl for resource type: "
                                    + desc.getResourceType().getType());
                } else {
                    Globals.Log.debug(
                            "NdlResourcePoolFactory: Merging properties extracted from ndl with existing pool properties");
                    Globals.Log.debug("NdlResourcePoolFactory: Existing pool properties:" + desc.getPoolProperties());
                    Globals.Log.debug("NdlResourcePoolFactory: Pool properties extracted from NDL: " + poolProp);
                    PropList.mergeProperties(poolProp, desc.getPoolProperties());
                    Globals.Log
                            .debug("NdlResourcePoolFactory: Merged final pool properties:" + desc.getPoolProperties());
                }

                List<ResourcePoolAttributeDescriptor> lpoolAttr = ndlPropExtractor.getPoolAttributes();
                if (lpoolAttr == null || lpoolAttr.size() == 0) {
                    Globals.Log
                            .debug("NdlResourcePoolFactory: No pool attributes extracted from ndl for resource type: "
                                    + desc.getResourceType().getType());
                } else {
                    for (Iterator it = lpoolAttr.iterator(); it.hasNext();) {
                        Globals.Log.debug(
                                "NdlResourcePoolFactory: Adding attributed extracted from ndl with existing pool attributes");
                        ResourcePoolAttributeDescriptor currAttr = (ResourcePoolAttributeDescriptor) it.next();
                        Globals.Log.debug("NdlResourcePoolFactory: Adding attr: " + currAttr);
                        desc.addAttribute(currAttr);
                    }
                }

            } catch (ConfigurationException ex) {
                Globals.Log.warn("Configuration Exception while extracting properties/attr from ndl" + ex);
            } catch (IllegalArgumentException ex) {
                Globals.Log
                        .warn("Illegal argument to constructor while creating an instance of ndlpropertyextractor class"
                                + ex);
            } catch (InvocationTargetException ex) {
                Globals.Log.warn("Invocation target exception during reflection" + ex);
            } catch (NoSuchMethodException ex) {
                Globals.Log.warn("No such method exception during reflection" + ex);
            } catch (SecurityException ex) {
                Globals.Log.warn("Security Exception while extracting properties/attr from ndl" + ex);
            }

        } catch (ClassNotFoundException ex) {
            Globals.Log.warn("NDL property extractor class not found" + ex);
        } catch (InstantiationException ex) {
            Globals.Log.warn("Could not instantiate NDL property extractor class" + ex);
        } catch (IllegalAccessException ex) {
            Globals.Log.warn("Illegal access to NDL property extractor class" + ex);
        }
    }
}
