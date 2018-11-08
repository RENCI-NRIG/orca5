/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.proxies.soapaxis2.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import orca.shirako.api.ISlice;
import orca.shirako.common.SliceID;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.util.ResourceData;
import orca.util.ResourceType;

/**
 * This class contains utility methods to translate to/from slices and
 * soap.beans objects. These methods are used by the various proxies to
 * serialize and de-serialize data across the wire. <br>
 * <br>
 * It is extremely important to keep the code up to date with changes to the
 * following objects: Slice, Term, UpdateData, ResourceSet, ResourceData.
 */
public class Translate {
    /**
     * The direction constants specify the direction of a request. They are used
     * to determine what properties should pass from one actor to another.
     */
    public static final int DirectionAgent = 1;
    public static final int DirectionAuthority = 2;
    public static final int DirectionReturn = 3;

    /**
     * Translates a soap.beans.Properties to java.util.Properties
     * @param beanProperties beanProperties
     * @return java.util.Properties
     */
    public static java.util.Properties translate(orca.shirako.proxies.soapaxis2.beans.Properties beanProperties) {
        java.util.Properties properties = new java.util.Properties();

        if (beanProperties != null) {
            orca.shirako.proxies.soapaxis2.beans.Property[] beanPropertyArray = beanProperties.getProperty();

            if (beanPropertyArray != null) {
                for (int i = 0; i < beanPropertyArray.length; i++) {
                    properties.setProperty(beanPropertyArray[i].getName(), beanPropertyArray[i].getValue());
                }
            }
        }

        return properties;
    }

    /**
     * Translates a java.util.Properties to a soap.beans.Properties
     * @param properties properties
     * @return orca.shirako.proxies.soapaxis2.beans.Properties
     */
    public static orca.shirako.proxies.soapaxis2.beans.Properties translate(java.util.Properties properties) {
        orca.shirako.proxies.soapaxis2.beans.Properties result = new orca.shirako.proxies.soapaxis2.beans.Properties();
        if (properties != null) {
            Set<?> set = properties.entrySet();
            Iterator<?> iter = set.iterator();
            orca.shirako.proxies.soapaxis2.beans.Property[] beanPropertyArray = new orca.shirako.proxies.soapaxis2.beans.Property[set.size()];

            int i = 0;

            while (iter.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
                orca.shirako.proxies.soapaxis2.beans.Property temp = new orca.shirako.proxies.soapaxis2.beans.Property();
                temp.setName((String) entry.getKey());
                temp.setValue((String) entry.getValue());
                beanPropertyArray[i++] = temp;
            }

            result.setProperty(beanPropertyArray);
        } else {
            result.setProperty(new orca.shirako.proxies.soapaxis2.beans.Property[0]);
        }
        return result;
    }

    /**
     * Translates a <code>slices.ResourceData</code> to a
     * <code>soap.beans.ResourceData</code>
     * @param resourceData The <code>slices.ResourceData</code> object
     * @param direction The direction of the call
     * @return orca.shirako.proxies.soapaxis2.beans.ResourceData
     */
    public static orca.shirako.proxies.soapaxis2.beans.ResourceData translate(orca.shirako.util.ResourceData resourceData, int direction) {
        if (resourceData != null) {
            java.util.Properties properties = null;
            orca.shirako.proxies.soapaxis2.beans.ResourceData beanResourceData = new orca.shirako.proxies.soapaxis2.beans.ResourceData();

            switch (direction) {
                case DirectionAgent:
                    properties = resourceData.getRequestProperties();

                    if (properties != null) {
                        beanResourceData.setRequestProperties(translate(properties));
                    }

                    break;

                case DirectionAuthority:
                    properties = resourceData.getConfigurationProperties();

                    if (properties != null) {
                        beanResourceData.setConfigurationProperties(translate(properties));
                    }

                    break;

                case DirectionReturn:
                    properties = resourceData.getResourceProperties();

                    if (properties != null) {
                        beanResourceData.setResourceProperties(translate(properties));
                    }

                    break;

                default:
                    return null;
            }

            return beanResourceData;
        }

        return null;
    }

    /**
     * Translates a <code>soap.beans.ResourceData</code> to a
     * <code>slices.ResourceData</code>.
     * @param beanResourceData The <code>slices.ResourceData</code> object
     * @return beanResourceData
     */
    public static orca.shirako.util.ResourceData translate(orca.shirako.proxies.soapaxis2.beans.ResourceData beanResourceData) {
        if (beanResourceData != null) {
            orca.shirako.proxies.soapaxis2.beans.Properties properties = null;
            orca.shirako.util.ResourceData resourceData = new ResourceData();

            properties = beanResourceData.getConfigurationProperties();

            if (properties != null) {
                ResourceData.mergeProperties(translate(properties), resourceData.getConfigurationProperties());
            }

            properties = beanResourceData.getRequestProperties();

            if (properties != null) {
                ResourceData.mergeProperties(translate(properties), resourceData.getRequestProperties());
            }

            properties = beanResourceData.getResourceProperties();

            if (properties != null) {
                ResourceData.mergeProperties(translate(properties), resourceData.getResourceProperties());
            }

            return resourceData;
        }

        return null;
    }

    /**
     * Translates a soap.beans.Slice to slices.Slice
     * @param beanSlice beanSlice
     * @return ISlice
     */
    public static ISlice translate(orca.shirako.proxies.soapaxis2.beans.Slice beanSlice) {
        if (beanSlice != null) {
            if (beanSlice.getGuid() == null) {
                throw new RuntimeException("Missing guid");
            }

            SliceID id = new SliceID(beanSlice.getGuid());
            ISlice slice = SliceFactory.getInstance().create(id, beanSlice.getSliceName());

            return slice;
        } else {
            return null;
        }
    }

    /**
     * Translates a slices.Slice to soap.beans.Slice
     * @param slice slice
     * @return beanSlice
     */
    public static orca.shirako.proxies.soapaxis2.beans.Slice translate(ISlice slice) {
        orca.shirako.proxies.soapaxis2.beans.Slice result = new orca.shirako.proxies.soapaxis2.beans.Slice();
        result.setSliceName(slice.getName());
        result.setGuid(slice.getSliceID().toString());
        return result;
    }

    /**
     * Translates a soap.beans.UpdateData to slices.UpdateData
     * @param beanUpdateData beanUpdateData
     * @return orca.shirako.util.UpdateData
     */
    public static orca.shirako.util.UpdateData translate(orca.shirako.proxies.soapaxis2.beans.UpdateData beanUpdateData) {
        if (beanUpdateData.getFailed()) {
            return new orca.shirako.util.UpdateData(beanUpdateData.getMessage());
        } else {
            return new orca.shirako.util.UpdateData();
        }
    }

    /**
     * Translates a slices.UpdateData to soap.beans.updateData
     * @param updateData updateData
     * @return beanUpdateData
     */
    public static orca.shirako.proxies.soapaxis2.beans.UpdateData translate(orca.shirako.util.UpdateData updateData) {
        orca.shirako.proxies.soapaxis2.beans.UpdateData result = new orca.shirako.proxies.soapaxis2.beans.UpdateData();
        result.setMessage(updateData.getMessage());
        result.setFailed(updateData.failed());
        return result;
    }

    /**
     * Translates a soap.beans.Term to slices.Term
     * @param beanTerm beanTerm
     * @return term
     */
    public static orca.shirako.time.Term translate(orca.shirako.proxies.soapaxis2.beans.Term beanTerm) {
        orca.shirako.time.Term term = new orca.shirako.time.Term();

        if (beanTerm.getStartTime() > 0) {
            term.setStartTime(new java.util.Date(beanTerm.getStartTime()));
        }

        if (beanTerm.getEndTime() > 0) {
            term.setEndTime(new java.util.Date(beanTerm.getEndTime()));
        }

        if (beanTerm.getNewStartTime() > 0) {
            term.setNewStartTime(new java.util.Date(beanTerm.getNewStartTime()));
        }

        return term;
    }

    /**
     * Translates slices.Term to soap.beans.Term
     * @param term term
     * @return beanTerm
     */
    public static orca.shirako.proxies.soapaxis2.beans.Term translate(orca.shirako.time.Term term) {
        orca.shirako.proxies.soapaxis2.beans.Term beanTerm = new orca.shirako.proxies.soapaxis2.beans.Term();

        if (term.getStartTime() != null) {
            beanTerm.setStartTime(term.getStartTime().getTime());
        } else {
            beanTerm.setStartTime(0);
        }

        if (term.getEndTime() != null) {
            beanTerm.setEndTime(term.getEndTime().getTime());
        } else {
            beanTerm.setEndTime(0);
        }

        if (term.getNewStartTime() != null) {
            beanTerm.setNewStartTime(term.getNewStartTime().getTime());
        } else {
            beanTerm.setNewStartTime(0);
        }

        return beanTerm;
    }

    public static orca.shirako.kernel.ResourceSet translate(orca.shirako.proxies.soapaxis2.beans.ResourceSet beanResourceSet) {
        orca.shirako.util.ResourceData resourceData = translate(beanResourceSet.getResourceData());
        orca.shirako.kernel.ResourceSet resourceSet = new orca.shirako.kernel.ResourceSet(beanResourceSet.getUnits(), new ResourceType(beanResourceSet.getType()), resourceData);

        return resourceSet;
    }

    public static orca.shirako.proxies.soapaxis2.beans.ResourceSet translate(orca.shirako.kernel.ResourceSet resourceSet, int direction) {
        orca.shirako.proxies.soapaxis2.beans.ResourceSet beanResourceSet = new orca.shirako.proxies.soapaxis2.beans.ResourceSet();
        beanResourceSet.setType(resourceSet.getType().getType());
        beanResourceSet.setUnits(resourceSet.getUnits());
        beanResourceSet.setResourceData(translate(resourceSet.getResourceData(), direction));

        return beanResourceSet;
    }
}
