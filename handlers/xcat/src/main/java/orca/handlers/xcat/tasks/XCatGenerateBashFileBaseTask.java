package orca.handlers.xcat.tasks;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;

import orca.shirako.common.meta.UnitProperties;
import orca.shirako.plugins.config.OrcaAntTask;

public class XCatGenerateBashFileBaseTask extends OrcaAntTask {
	protected String file;
	protected static final String END_OF_USER_SCRIPT = "EndOfUserScript";
	protected static final String END_OF_SCRIPT = "EndOfScript";

	public static String USER_SCRIPT_NAME="/tmp/user-script";
	public static String USER_SCRIPT_LOG_NAME="/tmp/user-script-log";
	protected Map<String, String> networkMap = new HashMap<String, String>();
	
	public static final String INITIATOR_FILE="/etc/iscsi/initiatorname.iscsi";

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
	
	protected void bashRemoveInterfaceCommands(PrintWriter out, String iface, String tag) {
		if (tag == null)
			return;
		
		String netDevice = iface + "." + tag;
		
		out.println("# configuration for removing tag " + tag + " from interface " + iface);
		
		out.println("# shut down tagged interface " + netDevice);
		out.println("ifconfig " + netDevice + " down");
		
		out.println("# remove tagged interface " + netDevice);
		out.println("vconfig rem " + netDevice);
	}
	
	protected void bashInterfaceCommands(PrintWriter out, String iface, String tag, String ip) {
		// bash commands to create a tagged or untagged interface and configure it with given IP

		if ((iface == null) || (tag == null))
			return;

		out.println("# configuration for interface " + iface + " on tag " + tag + " with ip " + ip);

		// load 8021q module
		out.println("# load 8021q");
		out.println("modprobe 8021q");
		// bring up the parent interface
		out.println("# bring up parent interface");
		out.println("ifconfig " + iface + " up");

		String ifname = iface;
		if (tag != null) {
			// we want them to look like eth1.4
			out.println("vconfig set_name_type DEV_PLUS_VID_NO_PAD");
			out.println("vconfig add " + iface + " " + tag);
			ifname = iface + "." + tag;
		}

		if (ip == null)
			return;

		String[] ipmask = ip.split("/");
		if ((ipmask.length != 2) || (ip.contains(";"))){
			System.out.println("IP address " + ip + " does not match expected format ip/netmask");
			return;
		}

		int nmVal = Integer.parseInt(ipmask[1]);
		String realMask = netmaskIntToString(nmVal);

		out.println("ifconfig " + ifname + " " + ipmask[0] + " netmask " + realMask + " up");
		return;
	}

	protected void bashStorageCommands(PrintWriter out, int i, String store_type, String target_ip, String target_port, String target_lun,
			String target_chap_user, String target_chap_secret, String target_should_attach, 
			String fs_type, String fs_options, String fs_should_format, String fs_mount_point) {
		
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

		if (target_should_attach.equalsIgnoreCase("yes")){
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
	
	/**
	 * User-supplied post-boot script
	 * @param out
	 * @param config
	 * @throws Exception
	 */
	protected void generateInstanceConfig(PrintWriter out, String config) throws Exception {
		System.out.println("Processing instanceConfig section");
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
		super.execute();
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
	
	protected void generateHostname(PrintWriter out) throws Exception {
		
		String hname = getProject().getProperty(UnitProperties.UnitHostName);
		
		if (hname != null) {
			out.println("# setting hostname to " + hname);
			out.println("hostname " + hname);
		} else {
			out.println("# no hostname set");
		}
	}
	
}
