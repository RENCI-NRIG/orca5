package orca.handlers.xcat.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import orca.shirako.common.meta.UnitProperties;
import orca.shirako.plugins.config.OrcaAntTask;

import org.apache.tools.ant.BuildException;

/**
 * Similar to EC2 .INI file generator, this generates a bash
 * script that can be executed by a baremetal node upon boot
 * to configure its interfaces
 * @author ibaldin
 *
 */
public class XCatGenerateBashFileTask extends OrcaAntTask {
    private static final String END_OF_USER_SCRIPT = "EndOfUserScript";
	private static final String END_OF_SCRIPT = "EndOfScript";
	protected String file;
    public static String USER_SCRIPT_NAME="/tmp/user-script";
    public static String USER_SCRIPT_LOG_NAME="/tmp/user-script-log";
    private Map<String, String> networkMap = new HashMap<String, String>();
    
	// converting to netmask
	private static final String[] netmaskConverter = {
		"128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0", "255.0.0.0",
		"255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0", "255.252.0.0", "255.254.0.0", "255.255.0.0",
		"255.255.128.0", "255.255.192.0", "255.255.224.0", "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
		"255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254", "255.255.255.255"
	};
	
	protected static String netmaskIntToString(int nm) {
		if ((nm > 32) || (nm < 1)) 
			return "255.255.255.0";
		else
			return netmaskConverter[nm - 1];
	}
    
    protected void generateGlobal(PrintWriter out) throws Exception {
    	out.println("#!/bin/bash");
    	
        String temp = getProject().getProperty(UnitProperties.UnitActorID);
        if (temp != null) {
            out.println("# actor_id=" + temp);
        }
        temp = getProject().getProperty(UnitProperties.UnitSliceID);
        if (temp != null) {
            out.println("# slice_id=" + temp);
        }
        temp = getProject().getProperty(UnitProperties.UnitReservationID);
        if (temp != null) {
            out.println("# reservation_id=" + temp);
        }
        temp = getProject().getProperty(UnitProperties.UnitID);
        if (temp != null) {
            out.println("# unit_id=" + temp);
        }
        temp = getProject().getProperty(UnitProperties.UnitISCSIInitiatorIQN);
    	if (temp != null) {
    	     out.println("# iscsi_initiator_iqn=" + temp);
    	 } else {
    	     out.println("# iscsi_initiator_iqn= Not Specified" );
    	 }
    }
    
    protected Integer[] getEths(){
        HashSet<Integer> set = new HashSet<Integer>();
        Hashtable<?, ?> h = project.getProperties();

        Iterator<?> i = h.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            String key = (String) entry.getKey();
            //System.out.println("property: " + key + " = " + entry.getValue().toString());
            if (key.startsWith(UnitProperties.UnitEthPrefix)) {
                key = key.substring(UnitProperties.UnitEthPrefix.length());
                int index = key.indexOf('.');
                if (index > 0){
                    key = key.substring(0, index);
                    Integer eth = new Integer(Integer.parseInt(key));
                    set.add(eth);
                }
            }
        }
        
        Integer[] list = new Integer[set.size()];               
        
        int index = 0;
        for (Integer eth : set) {
            list[index++] = eth;            
        }
        
        Arrays.sort(list);
        return list;        
    }
    
    protected String bashInterfaceCommands(String iface, String tag, String ip) {
    	// bash commands to create a tagged or untagged interface and configure it with given IP
    	String ret = "";
    	
    	if ((iface == null) || (tag == null))
    		return null;
    	
    	ret += "# configuration for interface " + iface + " on tag " + tag + " with ip " + ip + "\n";
    	
    	// load 8021q module
    	ret += "# load 8021q\n";
    	ret += "modprobe 8021q\n";
    	// bring up the parent interface
    	ret += "# bring up parent interface\n";
    	ret += "ifconfig " + iface + " up\n";
    	
    	String ifname = iface;
    	if (tag != null) {
        	// we want them to look like eth1.4
    		ret += "vconfig set_name_type DEV_PLUS_VID_NO_PAD\n";
    		ret += "vconfig add " + iface + " " + tag + "\n";
    		ifname = iface + "." + tag;
    	}
    	
    	if (ip == null)
    		return ret;
    	
    	String[] ipmask = ip.split("/");
    	if ((ipmask.length != 2) || (ip.contains(";"))){
    		System.out.println("IP address " + ip + " does not match expected format ip/netmask");
    		return null;
    	}
    	
    	int nmVal = Integer.parseInt(ipmask[1]);
    	String realMask = netmaskIntToString(nmVal);
    	
    	ret += "ifconfig " + ifname + " " + ipmask[0] + " netmask " + realMask + " up\n";
    	return ret;
    }
    
    protected void generateInterfaces(PrintWriter out) throws Exception {        
        Integer[] eths = getEths();
        
        if (eths.length == 0) {
            System.out.println("No interface-specific configuration specified. Checking for unit.vlan.tag");
            String vlan = getProject().getProperty(UnitProperties.UnitVlanTag);
            if (vlan == null) {
                System.out.println("No global unit.vlan.tag specified either");                
            } else {
                System.out.println("Found unit.vlan.tag=" + vlan);
                String hostNet = getProject().getProperty(UnitProperties.UnitVlanHostEth);
                if (hostNet == null) {
                	System.out.println("No global unit.vlan.hosteth specified, skipping");
                	return;
                }
                String hosteth = networkMap.get(hostNet.trim());
                if (hosteth == null) {
                	System.out.println("Unable to find an interface mapping for host interface network " + hostNet);
                	return;
                }
                hosteth = hosteth.trim();
                out.println("#");
                out.println(" interface configuration section");
                out.println("#");
                
                out.println(bashInterfaceCommands(hosteth, vlan, null));
            }
            return;
        }
            
        out.println("#");
        out.println("# interface configuration section");
        out.println("#");
        
        for (int i = 0; i < eths.length; i++) {
            Integer eth = eths[i];
            // see if this is a physical or a vlan attachment
            String mode = getProject().getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthModeSuffix);
            if (mode == null)
            	mode = "vlan";
            
            // see what physical interface on the host we need to attach to (eth0 if unspecified)
            String hostNet = getProject().getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitHostEthSuffix);
            if (hostNet == null) {
            	System.out.println("Eth" + eth.toString() + " is missing hosteth. Ignoring");
            	continue;
            }
            String hosteth = networkMap.get(hostNet.trim());
            if (hosteth == null) {
            	System.out.println("Unable to find an interface mapping for host interface network " + hostNet);
            	return;
            }
            hosteth = hosteth.trim();
       		String ip = getProject().getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthIPSuffix);
    		if (ip == null) {
    			System.out.println("Eth" + eth.toString() + " does not specify an IP.");
    		}

            if (mode.equals("vlan")) {
            	// attaching to vlan tag
            	String tag = getProject().getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthVlanSuffix);
            	if (tag == null) {
            		System.out.println("Eth" + eth.toString() + " is missing vlan tag. Ignoring.");
            		continue;
            	}
            	if (ip != null) {
            		System.out.println("Configuring eth" + eth.toString() + " on "+ hosteth + " vlan=" + tag + ", ip=" + ip);
            	} else {
            		System.out.println("Configuring eth"  + eth.toString() + " on " + hosteth + " vlan=" + tag + ", ip=[no ip]");
            	}

            	out.print(bashInterfaceCommands(hosteth, tag, ip));
            }
    		out.println();
        }   
    }
    
    protected Integer[] getStores(){
        HashSet<Integer> set = new HashSet<Integer>();
        Hashtable<?, ?> h = project.getProperties();

        Iterator<?> i = h.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            String key = (String) entry.getKey();
            if (key.startsWith(UnitProperties.UnitStoragePrefix)) {
                key = key.substring(UnitProperties.UnitStoragePrefix.length());
                int index = key.indexOf('.');
                if (index > 0){
                    key = key.substring(0, index);
                    Integer store = new Integer(Integer.parseInt(key));
                    set.add(store);
                }
            }
        }

        Integer[] list = new Integer[set.size()];

        int index = 0;
        for (Integer store : set) {
            list[index++] = store;
        }

        Arrays.sort(list);
        return list;
    }
    
    protected void generateStorage(PrintWriter out) throws Exception {
    	System.out.println("Processing storage section");

    	out.println("#");
    	out.println("# generated iSCSI storage script");
    	out.println("#");
    	
    	out.println("sleep 20");

    	//set initiator id
    	String temp = getProject().getProperty(UnitProperties.UnitISCSIInitiatorIQN);
    	if (temp != null) {
    	     out.println("echo InitiatorName=" + temp + " >  /etc/iscsi/initiatorname.iscsi");
    	 } else {
    	     out.println("# Error no iscsi_initiator_iqn, skipping storage" );
    	     return;
    	 }
    	    	
    	//restart iscsi
    	out.println("/etc/init.d/iscsid force-start");
    	
    	out.println("sleep 5");
    	
		out.println("");

        Integer[] stores = getStores();

        for (int i = 0; i < stores.length; i++) {
            Integer store = stores[i];

            String store_type = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitStoreTypeSuffix);
            if (store_type == null) {
                System.out.println("Store " + store.toString() + " is missing store type. Ignoring");
                continue;
            }
	    
	    //for now we only know about iscsi 
	    if (store_type.compareTo("iscsi") != 0){
		System.out.println("Unknown storage type " + store_type + " for " + store.toString() + ".  Ignoring");
                continue;
	    }

	    String target_ip = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetIPSuffix);
            if (target_ip == null) {
                System.out.println("Store " + store.toString() + " does not specify a target IP. Ignoring");
                continue;
            }

            String target_port = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetPortSuffix);
            if (target_port == null) {
                System.out.println("Store " + store.toString() + " does not specify a target port. Ignoring");
                continue;
            }

	    String target_lun = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetLunSuffix);
            if (target_lun == null) {
                System.out.println("Store " + store.toString() + " does not specify a target lun. Assuming 0");
                target_lun = "0";
            }


	    String target_should_attach = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetShouldAttachSuffix);
            if (target_should_attach == null) {
		System.out.println("Store " + store.toString() + " does not specify if it should be attached. Assuming no.");
		target_should_attach = "no";
            }

	    String target_chap_user = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetChapUserSuffix);
            if (target_chap_user == null) {
                System.out.println("Store " + store.toString() + " does not specify chap user name. Assuming empty string.");
		target_chap_user ="";
	    }

	    String target_chap_secret = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetChapSecretSuffix);
            if (target_chap_secret == null) {
		System.out.println("Store " + store.toString() + " does not specify chap secret. Assuming empty string.");
                target_chap_secret ="";
            }


            String fs_type = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitFSTypeSuffix);
            if (fs_type == null) {
		System.out.println("Store " + store.toString() + " does not specify file system type. Assuming empty string.");
                fs_type ="";
            }


	    String fs_options = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitFSOptionsSuffix);
            if (fs_options == null) {
                System.out.println("Store " + store.toString() + " does not specify file system options. Assuming empty string.");
                fs_options ="";
            }


            String fs_should_format = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitFSShouldFormatSuffix);
            if (fs_should_format == null) {
                System.out.println("Store " + store.toString() + " does not specify if the file system should be formatted. Assuming no.");
                fs_should_format ="no";
            }

            String fs_mount_point = getProject().getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitFSMountPointSuffix);
            if (fs_mount_point == null) {
                System.out.println("Store " + store.toString() + " does not specify file system mount point. Assuming empty string (i.e. file system will not be mounted).");
                fs_mount_point ="";
            }



	    out.print("#dev" + i + "=");
	    out.print(store_type); 
	    out.print(":" + target_ip);
	    out.print(":" + target_port);
	    out.print(":" + target_lun);
            out.print(":" + target_chap_user);
            out.print(":" + target_chap_secret);
	    out.print(":" + target_should_attach);
            out.print(":" + fs_type);
            out.print(":" + fs_options);
            out.print(":" + fs_should_format);
            out.print(":" + fs_mount_point);

            out.println();
            
            
            if(target_should_attach.equalsIgnoreCase("yes")){
            	//discover
            	//out.println("OUTPUT=`/sbin/iscsiadm --mode discovery --type  sendtargets --portal " + target_ip + "`");
            	//out.println("TARGET=`echo ${OUTPUT} | grep '^" + target_ip + "' | awk '{ print $2}'`");
            	out.println("TARGET=`/sbin/iscsiadm --mode discovery --type  sendtargets --portal " + target_ip + " | grep '^" + target_ip + "' | awk '{ print $2}'`");
            	
            	out.println("/sbin/iscsiadm --mode node --targetname $TARGET --portal " + target_ip + ":" + target_port + " --op=update --name node.session.auth.authmethod --value=CHAP");
            	out.println("/sbin/iscsiadm --mode node --targetname $TARGET --portal " + target_ip + ":" + target_port + " --op=update --name node.session.auth.username --value=" + target_chap_user);
            	out.println("/sbin/iscsiadm --mode node --targetname $TARGET --portal " + target_ip + ":" + target_port + " --op=update --name node.session.auth.password --value=" + target_chap_secret);
            	out.println("/sbin/iscsiadm --mode node --targetname $TARGET --portal " + target_ip + ":" + target_port + " --login");
            	out.println("sleep 10");
            	
            	if(fs_should_format.equalsIgnoreCase("yes")){
            		//format 
            		out.println("DEVICE='/dev/disk/by-path/ip-" + target_ip + ":" + target_port + "-iscsi-'${TARGET}'-lun-" + target_lun + "'");
            		out.println("/sbin/mkfs -t " + fs_type + " " + fs_options + " $DEVICE");
            
            		//mount
            		out.println("mkdir " + fs_mount_point);
            		out.println("mount -t " + fs_type + " $DEVICE " + fs_mount_point);
            
            	}
            }

        }
    }

    
    protected void generateInstanceConfig(PrintWriter out) throws Exception {
        System.out.println("Processing instanceConfig section");
        String config = getProject().getProperty(UnitProperties.UnitInstanceConfig);
        if (config == null) {
            return;
        }
        
        out.println("#");
        out.println("# user-generated post-boot configuration");
        out.println("#");
        out.println("(");
        out.println("cat << " + END_OF_SCRIPT);
        out.println(config.replaceAll(END_OF_SCRIPT, END_OF_USER_SCRIPT));
        out.println(END_OF_SCRIPT);
        out.println(") > " + USER_SCRIPT_NAME);
        out.println("# Execute the script " + USER_SCRIPT_NAME);
        out.println("chmod ug+x " + USER_SCRIPT_NAME);
        out.println(USER_SCRIPT_NAME + " 2> " + USER_SCRIPT_LOG_NAME);
    }
    
    public void execute() throws BuildException {
        try {
            super.execute();
            if (file == null) {
                throw new Exception("Missing file parameter");
            }
            PrintWriter out = new PrintWriter(new FileWriter(new File(file)));
            
            generateGlobal(out);
            generateInterfaces(out); 
            generateStorage(out);
            generateInstanceConfig(out);
            out.close();
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }
    
    public void setFile(String file) {
        this.file = file;
    }
    
    public void setInterfaceMap(String map) {
    	if (map == null)
    		return;
    	// map is netname:ifname, netname:ifname
    	String[] entries = map.split(",");
    	if (entries.length == 0)
    		return;
    	for (String entry: entries) {
    		String[] mapEntry = entry.split(":");
    		if (mapEntry.length == 2)
    			networkMap.put(mapEntry[0], mapEntry[1]);
    	}
    	
    }

}
