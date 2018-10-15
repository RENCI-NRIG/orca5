package net.exogeni.orca.controllers.xmlrpc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Properties;

import net.exogeni.orca.ndl.LayerConstant;
import net.exogeni.orca.ndl.elements.Device;
import net.exogeni.orca.ndl.elements.Interface;
import net.exogeni.orca.ndl.elements.NetworkConnection;
import net.exogeni.orca.ndl.elements.SwitchingAction;
import net.exogeni.orca.shirako.common.meta.UnitProperties;
import net.exogeni.orca.util.PropList;

/**
 * This class is responsible for converting a NetworkConnection class to a set of properties required by the
 * BenNdlHandler.
 * 
 * @author aydan
 */
public class PropertiesConverter {
    public static final String ActionVlanTag = "VLANtag";
    public static final String Server = "server";

    public static final String UriSeparator = "#";
    public static final String UriSuffix = "/";

    /**
     * Returns a normalized domain name.
     * 
     * @param d d
     * @return normalized domain name
     */
    public static String getDomainName(Device d) {
        // System.out.println("getDomainName(): d.getResourceType(): " + d.getResourceType().toString() + " |
        // d.getUri(): " + d.getUri() );
        String temp = d.getURI();
        int index = temp.indexOf(UriSeparator);
        if (index >= 0) {
            int index2 = temp.indexOf(UriSuffix, index);
            if (index2 >= 0) {
                String rType = null;
                if ((d.getResourceType() == null) || (d.getResourceType().getResourceType() == null)) {
                    rType = null;
                } else {
                    String[] type = d.getResourceType().getResourceType().split("\\#");
                    if (type.length == 1)
                        rType = null;
                    else
                        rType = type[1];
                }
                // System.out.println("getDomainName(): rType: " + rType);
                if (rType == null) {
                    return temp.substring(index + 1, index2).toLowerCase();
                } else if (rType.equalsIgnoreCase("VM") || rType.equalsIgnoreCase("Testbed")) {
                    return temp.substring(index + 1, index2).toLowerCase();
                } else {
                    return temp.substring(index + 1, index2).toLowerCase().concat("/").concat(rType.toLowerCase());
                }
            }

        }
        return null;
    }

    //
    private static void convert(String site, LinkedList<Device> list, Properties p) {
        site = site.split("/")[0];
        for (Device d : list) {
            String name = d.getName();
            if (name == null) {
                continue;
            }
            name = name.toLowerCase().trim();
            if (!name.equals(Server)) {
                LinkedList<SwitchingAction> actions = d.getActionList();

                int anum = 0;

                for (int i = 0; i < actions.size(); i++) {
                    SwitchingAction a = actions.get(i);
                    // System.out.println("Action=" + a.getDefaultAction());
                    if (Objects.equals(a.getDefaultAction(), LayerConstant.Action.Temporary.toString())) {
                        continue;
                    }
                    if ((a.getDefaultAction().equals(LayerConstant.Action.VLANtag.toString()))
                            || (name.equals("6509"))) {
                        PropList.setProperty(p, UnitProperties.UnitVlanTag, (int) a.getLabel_ID());
                        PropList.setProperty(p, UnitProperties.UnitBandwidth, a.getBw());
                    } // ignore label id otherwise

                    LinkedList<Interface> ifs = a.getClientInterface();
                    if (ifs.size() != 2) {
                        throw new RuntimeException("Can only handle two interfaces");
                    }

                    // ignore the dummy interfaces Yufeng adds to represent request endpoints
                    if (ifs.get(0).getName() == null && ifs.get(1).getName() == null) {
                        continue;
                    }
                    // unc.polatis.action.1.sport=
                    // unc.polatis.action.1.dport=

                    String ports = ifs.get(0).getName();
                    String ports2 = ifs.get(1).getName();
                    if (ports == null) {
                        if (ports2 != null) {
                            ports = ports2;
                        }
                    } else {
                        if (ports2 != null) {
                            ports = ports.concat(",").concat(ports2);
                        }
                    }
                    PropList.setProperty(p, site + "." + name + ".action." + (anum + 1) + ".ports", ports);
                    // System.out.println(site + "." + name + ".action." + (anum + 1) + ".ports=" + ports);

                    PropList.setProperty(p, site + "." + name + ".action." + (anum + 1) + ".sport",
                            ifs.get(0).getName());
                    PropList.setProperty(p, site + "." + name + ".action." + (anum + 1) + ".dport",
                            ifs.get(1).getName());
                    // System.out.println(site + "." + name + ".action." + (anum + 1) + ".sport=" +
                    // ifs.get(0).getName());
                    // System.out.println(site + "." + name + ".action." + (anum + 1) + ".dport=" +
                    // ifs.get(1).getName());
                    anum++;
                }

                // unc.polatis.actions=2
                PropList.setProperty(p, site + "." + name + ".actions", anum);
                // System.out.println(site + "." + name + ".actions=" + anum);

                String alist = "";
                StringBuilder alistSB = new StringBuilder();
                for (int i = 0; i < anum; i++) {
                    alistSB.append(Integer.toString(i + 1));
                    alistSB.append(" ");
                }
                alist = alistSB.toString();
                alist = alist.trim();

                PropList.setProperty(p, site + "." + name + ".actionslist", alist);
                // System.out.println(site + "." + name + ".actionslist=" + alist);
            }
        }
    }

    public static Properties convert(NetworkConnection con) {
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
            convert(key, l, p);
        }

        // System.out.println("PROPERTIES: " + p.toString());
        return p;
    }

    public static Properties convert(String portList) {
        Properties p = new Properties();

        // System.out.println("Port List: " + portList);
        PropList.setProperty(p, UnitProperties.UnitPortList, portList);
        return p;
    }

}
