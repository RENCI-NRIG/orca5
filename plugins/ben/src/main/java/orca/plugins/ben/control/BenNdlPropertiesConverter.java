package orca.plugins.ben.control;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

import orca.embed.cloudembed.NetworkHandler;
import orca.ndl.LayerConstant;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.SwitchingAction;
import orca.shirako.common.meta.UnitProperties;
import orca.util.PropList;
import orca.util.persistence.PersistenceException;
import orca.util.persistence.PersistenceUtils;

import org.apache.log4j.Logger;

/**
 * This class is responsible for converting a NetworkConnection class to a set
 * of properties required by the BenNdlHandler.
 * @author aydan
 */
public class BenNdlPropertiesConverter
{    
	public static final String ActionVlanTag = "VLANtag";
    public static final String Server = "Server";

    public static final String UriSeparator = "#";
    public static final String UriSuffix = "/";

    /**
     * Returns a normalized domain name.
     * @param d
     * @return
     */
    public static String getDomainName(Device d)
    {
        String temp = d.getURI();
        int index = temp.indexOf(UriSeparator);
        if (index >= 0) {
            int index2 = temp.indexOf(UriSuffix, index);
            if (index2 >= 0) {
            		String rType = null;
            		if( (d.getResourceType()==null) || (d.getResourceType().getResourceType()==null)){
            			rType=null;
            		}
            		else{
            			String [] type = d.getResourceType().getResourceType().split("\\#");
            			if(type.length==1)
            				rType=null;
            			else
            				rType=type[1];
            		}
            		if(rType==null){
                        return temp.substring(index + 1, index2).toLowerCase();
            		}
            		else if (rType.equalsIgnoreCase("VM") | rType.equalsIgnoreCase("Testbed")){
            			return temp.substring(index + 1, index2).toLowerCase();
            		}
            		else {
            			return temp.substring(index + 1, index2).toLowerCase().concat("/").concat(rType.toLowerCase());
            		}
            }

        }
        return null;
    }
    //
    private static void convert(String site, LinkedList<Device> list, Properties p, Logger logger)
    {
    	site = site.split("/")[0]; //get rid of "/vlan" etc. added to the network domain name to be consistent to the handlers.
    	
        for (Device d : list) {
            String type = d.getType();
            String name = d.getName();	
            int lastIndex=name.lastIndexOf("/");
            if(lastIndex>0)
            	name=name.substring(lastIndex+1);
            if (name == null) {
                continue;
            }
            
            name = name.toLowerCase().trim();
            if (!type.equals("vm")) {
                LinkedList<SwitchingAction> actions = d.getActionList();

                int anum = 0;

                for (int i = 0; i < actions.size(); i++) {
                    SwitchingAction a = actions.get(i);
                    logger.debug("Action=" + a.getDefaultAction());
                    if (a.getDefaultAction() == LayerConstant.Action.Temporary.toString()) {
                        continue;
                    }
                    if ((a.getDefaultAction().equals(LayerConstant.Action.VLANtag.toString())) || (name.equals("6509"))) {
                        PropList.setProperty(p, UnitProperties.UnitVlanTag, (int) a.getLabel_ID());
                        PropList.setProperty(p, UnitProperties.UnitBandwidth, a.getBw());
                    } // ignore label id otherwise

                    LinkedList<Interface> ifs = a.getClientInterface();
                    if (ifs.size() != 2) {
                        logger.debug("Not 2 interfaces:"+ifs.size()+" ;d.name="+d.getName()+" ;d.url="+d.getURI()+" ;d.type="+type);
			throw new RuntimeException("Can only handle two interfaces");
                    }

                    // ignore the dummy interfaces Yufeng adds to represent request endpoints
                    if (ifs.get(0).getName() == null && ifs.get(1).getName() == null) {
                        continue;
                    }
                    // unc.polatis.action.1.sport=
                    // unc.polatis.action.1.dport=

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
                    PropList.setProperty(p, site + "." + name + ".action." + (anum + 1) + ".ports", ports);
                    logger.debug(site + "." + name + ".action." + (anum + 1) + ".ports=" + ports);

                    PropList.setProperty(p, site + "." + name + ".action." + (anum + 1) + ".sport", ifs.get(0).getName());
                    PropList.setProperty(p, site + "." + name + ".action." + (anum + 1) + ".dport", ifs.get(1).getName());
                    logger.debug(site + "." + name + ".action." + (anum + 1) + ".sport=" + ifs.get(0).getName());
                    logger.debug(site + "." + name + ".action." + (anum + 1) + ".dport=" + ifs.get(1).getName());
                    anum++;
                }

                // unc.polatis.actions=2
                PropList.setProperty(p, site + "." + name + ".actions", anum);
                logger.debug(site + "." + name + ".actions=" + anum);

                String alist = "";
                for (int i = 0; i < anum; i++) {
                    alist += Integer.toString(i + 1) + " ";
                }
                alist = alist.trim();

                PropList.setProperty(p, site + "." + name + ".actionslist", alist);
                logger.debug(site + "." + name + ".actionslist=" + alist);
            }
        }
    }

    public static Properties convert(NetworkConnection con, NetworkHandler handler, Logger logger)
    {
		logger.debug("Invoking BenNdlPropertiesConverter.convert()");
        if(con==null){
        	logger.debug("No connection being generated in BEN:");
        	return null;
        }
        LinkedList<?> list = con.getConnection();
        HashMap<String, LinkedList<Device>> map = new HashMap<String, LinkedList<Device>>();
        
        for (Object o : list) {
            if (o instanceof Device) {
                Device d = (Device) o;
                String domain = getDomainName(d);
                LinkedList<Device> dlist = map.get(domain);
                if (dlist == null) {
                    dlist = new LinkedList<Device>();
                    map.put(domain, dlist);
                }
                dlist.add(d);
            }
        }

        Properties p = new Properties();
        for (String key : map.keySet()) {
            LinkedList<Device> l = map.get(key);
            convert(key, l, p, logger);
        }
        
        // recovery support
        try {
        	PropList.setProperty(p, BenNdlControl.PropertyConnectionRecoveryProperty, PersistenceUtils.save(con));
        	PropList.setProperty(p, BenNdlControl.PropertyAssignedLabelRecovery, handler.saveLocalLabels());
        } catch (Exception pee) {
        	logger.error("Unable to save connection " + con + " onto reservation due to persistence exception: " + pee);
        }
        
        logger.debug("PROPERTIES: " + p.toString());
        return p;
    }
    
    public static Properties convert(String portList, Logger logger){
    	Properties p = new Properties();
    	
       	logger.debug("Port List: " + portList);
       	PropList.setProperty(p,UnitProperties.UnitPortList,portList);
    	return p;
    }
    
}
