package orca.handlers.xcat.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import orca.shirako.common.meta.UnitProperties;

import org.apache.tools.ant.BuildException;

/**
 * Similar to EC2 .INI file generator, this generates a bash
 * script that can be executed by a baremetal node upon boot
 * to configure its interfaces
 * @author ibaldin
 *
 */
public class XCatGenerateBashFileTask extends XCatGenerateBashFileBaseTask {

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

				bashInterfaceCommands(out, hosteth, vlan, null);
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

				bashInterfaceCommands(out, hosteth, tag, ip);
			}
			out.println();
		}   
	}

	/**
	 * Get storage volume indices
	 * @return
	 */
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
	

	/**
	 * Get all interface indices
	 * @return
	 */
	protected Integer[] getEths() {
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

	protected void generateStorage(PrintWriter out) throws Exception {
		System.out.println("Processing storage section");

		out.println("#");
		out.println("# generated iSCSI storage script");
		out.println("#");

		out.println("sleep 20");

		//set initiator id
		String temp = getProject().getProperty(UnitProperties.UnitISCSIInitiatorIQN);
		if (temp != null) {
			out.println("echo InitiatorName=" + temp + " >  " + INITIATOR_FILE);
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

			bashStorageCommands(out, i, store_type, target_ip, target_port, target_lun, 
					target_chap_user, target_chap_secret, target_should_attach, 
					fs_type, fs_options, fs_should_format, fs_mount_point);

		}
	}

	public void execute() throws BuildException {
		try {
			super.execute();
			if (file == null) {
				throw new Exception("Missing file parameter");
			}
			PrintWriter out = new PrintWriter(new FileWriter(new File(file)));

			generateGlobal(out);
			generateHostname(out);
			generateInterfaces(out); 
			generateStorage(out);
			generateInstanceConfig(out, getProject().getProperty(UnitProperties.UnitInstanceConfig));
			out.close();
		} catch (BuildException e) {
			throw e;
		} catch (Exception e) {
			throw new BuildException("An error occurred: " + e.getMessage(), e);
		}
	}


}
