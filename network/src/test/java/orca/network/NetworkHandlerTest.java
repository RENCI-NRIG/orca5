package orca.network;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import junit.framework.TestCase;
import net.jwhoisserver.utils.InetNetworkException;
import orca.ndl.LayerConstant;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.SwitchingAction;
import orca.util.PropList;

public class NetworkHandlerTest extends TestCase {

    public static final String rootURL = "http://geni-orca.renci.org/owl/";
    String requestFileUNCRenci, requestFileDukeRenci, requestFileRenciDuke, requestFileDukeUNC, substrateFileName;
    String request1, request2, request3;
    NetworkHandler handler;

    public NetworkHandlerTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();

        requestFileUNCRenci = "orca/network/request-6509.rdf"; // UNC - RENCI
        requestFileDukeRenci = "orca/network/request-6509-1.rdf"; // Duke - RENCI
        requestFileRenciDuke = "orca/network/reques/t-6509-2.rdf"; // RENCI-Duke
        requestFileDukeUNC = "orca/network/request-6509-3.rdf"; // Duke - UNC
        substrateFileName = "orca/network/ben-dell.rdf";
    }

    public void print(LinkedList<Device> list) {
        for (Device d : list) {
            System.out.println(
                    "===> Device name=" + d.getName() + "(" + d.getUri() + ")" + " action count=" + d.getActionCount());
            LinkedList<SwitchingAction> actions = d.getActionList();
            for (SwitchingAction a : actions) {
                System.out.print("Action=" + a.getDefaultAction() + " Label=" + a.getLabel_ID());

                LinkedList<Interface> ifs = a.getSwitchingInterface();
                for (Interface iff : ifs) {
                    System.out.print(" Interface=" + iff.getName());
                }

                System.out.println();

            }
        }
    }

    public void print2(String site, LinkedList<Device> list, Properties p) {
        for (Device d : list) {
            String name = d.getType().toLowerCase().trim();
            if (!name.equals("server")) {
                LinkedList<SwitchingAction> actions = d.getActionList();
                int actionCount = actions == null ? 0 : actions.size();

                int anum = 0;

                for (int i = 0; i < actionCount; i++) {
                    SwitchingAction a = actions.get(i);
                    if (a.getDefaultAction() == LayerConstant.Action.Temporary.toString()) {
                        continue;
                    }
                    if (a.getDefaultAction() == "VLANtag") {
                        PropList.setProperty(p, "vlan.tag", (int) a.getLabel_ID());
                        PropList.setProperty(p, "vlan.bandwidth", a.getBw());
                        System.out.println("vlan.tag=" + a.getLabel_ID() + ";vlan.bandwidth=" + a.getBw());
                    } // ignore label id otherwise

                    LinkedList<Interface> ifs = a.getSwitchingInterface();
                    /*
                     * System.out.println(site+":"+name+":"+ifs.size()); for(int j=0;j<ifs.size();j++){
                     * System.out.println(ifs.get(j).resource+"\n"); }
                     */
                    if (ifs.size() != 2) {
                        System.out.println("OOOOOOOOPs! Can only handle two interfaces");
                    }

                    // unc.polatis.action.1.sport=
                    // unc.polatis.action.1.dport=

                    PropList.setProperty(p, site + "." + name + ".action." + (anum + 1) + ".sport",
                            ifs.get(0).getName());
                    PropList.setProperty(p, site + "." + name + ".action." + (anum + 1) + ".dport",
                            ifs.get(1).getName());
                    System.out.println(site + "." + name + ".action." + (anum + 1) + ".sport=" + ifs.get(0).getName());
                    System.out.println(site + "." + name + ".action." + (anum + 1) + ".dport=" + ifs.get(1).getName());
                    anum++;
                }

                // unc.polatis.actions=2
                PropList.setProperty(p, site + "." + name + ".actions", anum);
                System.out.println(site + "." + name + ".actions=" + anum);

                String alist = "";
                for (int i = 0; i < anum; i++) {
                    alist += Integer.toString(i + 1) + " ";
                }
                alist = alist.trim();

                PropList.setProperty(p, site + "." + name + ".actionslist", alist);
                System.out.println(site + "." + name + ".actionslist=" + alist);
            }
        }
    }

    public void print(NetworkConnection con) {
        if (con == null) {
            System.out.println("No connection being set up");
            return;
        }
        LinkedList<?> list = con.getConnection();
        LinkedList<Device> unc = new LinkedList<Device>();
        LinkedList<Device> renci = new LinkedList<Device>();
        LinkedList<Device> duke = new LinkedList<Device>();

        for (Object o : list) {
            if (o instanceof Device) {
                Device d = (Device) o;
                String uri = d.getUri();
                if (uri.indexOf("#Renci") != -1) {
                    // this is a renci device
                    renci.add(d);
                } else if (uri.indexOf("#UNC") != -1) {
                    // this a UNC device
                    unc.add(d);
                } else if (uri.indexOf("#Duke") != -1) {
                    // this a UNC device
                    duke.add(d);
                } else {
                    throw new RuntimeException("Device is from an unknown site: " + uri);
                }
            }
        }

        Properties p = new Properties();
        print2("unc", unc, p);
        print2("renci", renci, p);
        print2("duke", duke, p);
        System.out.println(p.toString());

    }

    public void testHandleMapping() throws IOException, InetNetworkException {
        handler = new NetworkHandler(substrateFileName);

        // handler.getMapper().getOntModel().write(System.out);

        handler.handleMapping(requestFileDukeUNC);

        request1 = handler.getCurrentRequestURI();

        NetworkConnection con = handler.getLastConnection();

        print(con);

        // handler.releaseReservation(request1);

        handler.handleMapping(requestFileDukeRenci);

        request2 = handler.getCurrentRequestURI();

        NetworkConnection con2 = handler.getLastConnection();
        print(con2);

        handler.handleMapping(requestFileUNCRenci);

        request3 = handler.getCurrentRequestURI();

        NetworkConnection con3 = handler.getLastConnection();
        print(con3);

        handler.releaseReservation(request2);

        handler.releaseReservation(request3);

        handler.releaseReservation(request1);

        // model.write(System.out);
    }

}
