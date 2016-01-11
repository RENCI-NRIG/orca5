package orca.plugins.ben.control;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Properties;

import orca.embed.cloudembed.MultiPointNetworkHandler;
import orca.embed.cloudembed.NetworkHandler;
import orca.ndl.LayerConstant;
import orca.ndl.NdlCommons;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.SwitchingAction;
import orca.shirako.common.meta.UnitProperties;
import orca.util.PropList;
import orca.util.persistence.PersistenceException;
import orca.util.persistence.PersistenceUtils;

import org.apache.log4j.Logger;

public class NlrNdlPropertiesConverter extends BenNdlPropertiesConverter {

	public static Properties convert(NetworkConnection con, NetworkHandler h,Logger logger)
    {
		logger.debug("Invoking NlrNdlPropertiesConverter.convert()");
		
		MultiPointNetworkHandler handler;
		
		try {
			handler = (MultiPointNetworkHandler)h;
		} catch(ClassCastException cce) {
			logger.error("Passed in handler " + h + " is not of type MultiPointNetworkHandler");
			return null;
		}
		
    	if(con==null){
        	logger.debug("No connection being generated in DD/NLR:");
        	return null;
        }

    	logger.debug("Start to convert DD/NLR properties");
    	Properties p = new Properties();
    	
    	LinkedList<NetworkElement> list = (LinkedList<NetworkElement>)con.getConnection();
    	String  site = null; //get rid of "/vlan" etc. added to the network domain name to be consistent to the handlers.
    	
    	int numDevice=0;
    	String numDeviceStr=null;
    	LinkedList <Device> config_device_list = new LinkedList <Device>();
    	logger.debug("Num device in nlr:"+list.size());
    	for(NetworkElement nd:list){
    		Device d= (Device) nd;
    		site = getDomainName(d);
    		if(d.isAllocatable()==false)
    			continue;
    		LinkedList<SwitchingAction> actions = ((Device) d).getActionList();
    		if(actions==null)
    			continue;
    		logger.debug("mp:"+d.getName()+"action="+actions.size());
    		config_device_list.add(d);
    		numDevice++;
    		if(numDevice==1)
    			numDeviceStr=String.valueOf(numDevice);
    		else
    			numDeviceStr = numDeviceStr+","+String.valueOf(numDevice);
    	}
    	logger.debug("Num configured device in nlr:"+numDevice);
    	PropList.setProperty(p, UnitProperties.UnitDeviceNum, numDeviceStr);
    	numDevice=0;
    	int label=0;
    	for(NetworkElement nd:config_device_list){
    		Device d= (Device) nd;
    		String type = d.getType();
    		String name = d.getName();	
    		logger.debug("d.name="+name);
            int lastIndex=name.lastIndexOf("/");
            if(lastIndex>0)
            	name=name.substring(lastIndex+1);
            if (name == null)
               	continue;
            numDevice++;
            site=name;
            PropList.setProperty(p, "unit.device."+numDevice, site);	
            if(d.getResource().getProperty(NdlCommons.hostName_p)!=null)
            	PropList.setProperty(p, "unit.device."+numDevice+".hostname",d.getResource().getProperty(NdlCommons.hostName_p).getString());
            if(d.getResource().getProperty(NdlCommons.topologyManagementIP)!=null)
            	PropList.setProperty(p, "unit.device."+numDevice+".managementip",d.getResource().getProperty(NdlCommons.topologyManagementIP).getString());
			
            int numAction = 0;
            String numActionStr=null; 
            LinkedList<SwitchingAction> actions = ((Device) d).getActionList();
            logger.debug("numAction="+actions.size());
            for (SwitchingAction a : actions) {
                logger.debug("Action=" + a.getDefaultAction());
                if (a.getDefaultAction() == LayerConstant.Action.Temporary.toString())
                    continue;
                numAction++;
                if(numAction==1)
            		numActionStr=String.valueOf(numAction);
            	else
            		numActionStr = numActionStr+","+String.valueOf(numAction);
            }
            
            String numActionProp = UnitProperties.UnitActionNum + "." +site;
            PropList.setProperty(p, numActionProp, numActionStr);            

            boolean isMulticast = false;
            if( (d.getCastType()!=null) && (d.getCastType().equalsIgnoreCase(NdlCommons.multicast))){
        		isMulticast = true;
        	}   
          
            logger.debug("d.name="+d.getName()+";numAction="+numAction+";isMulticast="+isMulticast);
            HashMap<Interface,String> exchange_intf_list = new HashMap<Interface,String>();
            int exchange_tag=0;
            long exchange_bw=0;
            numAction = 0;
            for (SwitchingAction a : actions) {
            	if (a.getDefaultAction() == LayerConstant.Action.Temporary.toString())
                   	continue;
            	numAction++;	
            	logger.debug("vlan="+a.getLabel_ID());
            	//ports
            	LinkedList<Interface> ifs = a.getClientInterface();
            	if(isMulticast){
            		if(a.getLabel_ID()<=0){
            			if(ifs.get(0).getLabel()!=null)
            				label = ifs.get(0).getLabel().label.intValue();
            		}else
            			label=(int) a.getLabel_ID();
            		logger.debug("Exchange:"+ifs.get(0).getURI()+";tag="+a.getLabel_ID());
            		exchange_intf_list.put(ifs.get(0),String.valueOf((int)a.getLabel_ID()));
            		if(numAction==1){
            			exchange_tag=label;
            			exchange_bw=a.getBw();
            			PropList.setProperty(p, UnitProperties.UnitVlanTag, exchange_tag);
            		}
            	}else{
            		if (a.getDefaultAction().equals(LayerConstant.Action.VLANtag.toString()) ||
            				a.getDefaultAction().equals(LayerConstant.Action.Delete.toString() )) {
            			if(numAction==1)
            				PropList.setProperty(p, UnitProperties.UnitVlanTag, (int) a.getLabel_ID());
            			PropList.setProperty(p, UnitProperties.UnitVlanTag+"."+numAction+"."+site, (int) a.getLabel_ID());
            			PropList.setProperty(p, UnitProperties.UnitBandwidth+"."+numAction+"."+site, a.getBw());
            			PropList.setProperty(p, UnitProperties.UnitBandwidth+".burst."+numAction+"."+site, (long) a.getBw()/8);
            		}
            		
            		if (ifs.size() != 2) {
            			logger.debug("Not 2 interfaces:"+ifs.size()+" ;d.name="+d.getName()+" ;d.url="+d.getURI()+" ;d.type="+type);
            				throw new RuntimeException("Can only handle two interfaces");
            		}

            		// ignore the dummy interfaces Yufeng adds to represent request endpoints
            		if (ifs.get(0).getName() == null && ifs.get(1).getName() == null) {
            			logger.debug("No interface name for configuration:"+ifs.get(0).getName()+";"+ifs.get(1).getName());
            			continue;
            		}

            		String ports=ifs.get(0).getName();
            		String ports2=ifs.get(1).getName();
            		if(ports==null){
            			if(ports2!=null){
            				ports=ports2;
            			}
            		}
            		else{
            			if(ports2!=null){
            				ports=ports.concat(",").concat(ports2);
            			}	
            		}
            		PropList.setProperty(p, site + ".action." + numAction + ".ports", ports);
            		logger.debug(site  + ".action." + numAction + ".ports=" + ports);

            		PropList.setProperty(p, site  + ".action." + numAction + ".sport", ifs.get(0).getName());
            		PropList.setProperty(p, site  + ".action." + numAction + ".dport", ifs.get(1).getName());
            		logger.debug(site + ".action." + numAction + ".sport=" + ifs.get(0).getName());
            		logger.debug(site + ".action." + numAction + ".dport=" + ifs.get(1).getName());
            	}
            }	
            if(isMulticast){
            	label=0;
            	numAction=0;
            	logger.debug("Exchange size="+exchange_intf_list.size());
            	for(Entry<Interface, String> entry:exchange_intf_list.entrySet()){
            		numAction++;
            		Interface intf = entry.getKey();
            		label = Integer.valueOf(entry.getValue());
            		logger.debug(site+":"+intf.getName()+";label="+label+"e_tag="+exchange_tag+"bw="+exchange_bw);
                    PropList.setProperty(p, UnitProperties.UnitVlanTag+"."+numAction+"."+site, label);
                    //if(label!=exchange_tag)
                    PropList.setProperty(p, UnitProperties.UnitVlanTag+".swap."+numAction+"."+site, exchange_tag);
                    PropList.setProperty(p, UnitProperties.UnitBandwidth+"."+numAction+"."+site, exchange_bw);
                    PropList.setProperty(p, UnitProperties.UnitBandwidth+".burst."+numAction+"."+site, (long) exchange_bw/8);
            		PropList.setProperty(p, site  + ".action." + numAction + ".ports", intf.getName());
            			
                	logger.debug(site + ".action." + numAction + ".ports=" + intf.getName()+":tag="+label+":swap="+exchange_tag+":bandwidth="+exchange_bw);
            	}
            }
    	}
    	
        try {
        	Properties pp = PersistenceUtils.save(con);
        	PropList.setProperty(p, BenNdlControl.PropertyConnectionRecoveryProperty, pp);
        	PropList.setProperty(p, BenNdlControl.PropertyAssignedLabelRecovery, handler.saveLocalLabels());
        } catch (Exception pee) {
        	logger.error("Unable to save connection " + con + " onto reservation due to persistence exception: " + pee);
        }
    	
    	//logger.debug("PROPERTIES: " + p.toString());
        return p;
    }
}
