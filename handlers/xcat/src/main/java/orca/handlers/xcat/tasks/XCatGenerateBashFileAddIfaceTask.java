package orca.handlers.xcat.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import orca.shirako.common.meta.UnitProperties;

import org.apache.tools.ant.BuildException;

public class XCatGenerateBashFileAddIfaceTask extends XCatGenerateBashFileModTask {


	protected void generateInterface(PrintWriter out) throws Exception {        

		out.println("#");
		out.println("# interface configuration section");
		out.println("#");

		// see if this is a physical or a vlan attachment
		String mode = getProject().getProperty(modifyPrefix() + UnitProperties.UnitEthModeSuffix);
		if (mode == null)
			mode = "vlan";

		// see what physical interface on the host we need to attach to (eth0 if unspecified)
		String hostNet = getProject().getProperty(modifyPrefix() + UnitProperties.UnitHostEthSuffix);
		if (hostNet == null) {
			System.out.println("Modify add interface " + modifyIndex + " is missing hosteth/network name. Ignoring");
			return;
		}
		String hosteth = networkMap.get(hostNet.trim());
		if (hosteth == null) {
			System.out.println("Unable to find an interface mapping for host interface network " + hostNet);
			return;
		}
		hosteth = hosteth.trim();
		String ip = getProject().getProperty(modifyPrefix() + UnitProperties.UnitEthIPSuffix);
		if (ip == null) {
			System.out.println("Modify add interface " + modifyIndex + " does not specify an IP.");
		}

		if (mode.equals("vlan")) {
			// attaching to vlan tag
			String tag = getProject().getProperty(modifyPrefix() + UnitProperties.UnitEthVlanSuffix);
			if (tag == null) {
				System.out.println("Modify add interface " + modifyIndex + " is missing vlan tag. Ignoring.");
				return;
			}
			if (ip != null) {
				System.out.println("Configuring modify interface " + modifyIndex + " on "+ hosteth + " vlan=" + tag + ", ip=" + ip);
			} else {
				System.out.println("Configuring modify interface " + modifyIndex + " on " + hosteth + " vlan=" + tag + ", ip=[no ip]");
			}

			bashInterfaceCommands(out, hosteth, tag, ip);
		}
		out.println(); 
	}


	protected void generateStorage(PrintWriter out) throws Exception {
		System.out.println("Processing storage section");

		out.println("#");
		out.println("# generated iSCSI storage script");
		out.println("#");

		out.println("sleep 20");

		//set initiator id
		String temp = getProject().getProperty(modifyPrefix() + ".iscsi.initiator.iqn");
		if (temp != null) {
			// comment out old initiator name, write in new one, consistent with neuca-guest-tools
			out.println("sed -i 's/^\\(InitiatorName=.*\\)$/##\\1/' " + INITIATOR_FILE);
			out.println("echo InitiatorName=" + temp + " >  " + INITIATOR_FILE);
		} else {
			// rely on the original initiator iqn
			;
		}

		//restart iscsi
		out.println("/etc/init.d/iscsid force-start");

		out.println("sleep 5");

		out.println("");

		String store_type = getProject().getProperty(modifyPrefix() + UnitProperties.UnitStoreTypeSuffix);
		if (store_type == null) {
			System.out.println("Store " + modifyPrefix() + " is missing store type. Ignoring");
			return;
		}

		//for now we only know about iscsi 
		if (store_type.compareTo("iscsi") != 0){
			System.out.println("Unknown storage type " + store_type + " for " + modifyPrefix() + ".  Ignoring");
			return;
		}

		String target_ip = getProject().getProperty(modifyPrefix() + UnitProperties.UnitTargetIPSuffix);
		if (target_ip == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify a target IP. Ignoring");
			return;
		}

		String target_port = getProject().getProperty(modifyPrefix() + UnitProperties.UnitTargetPortSuffix);
		if (target_port == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify a target port. Ignoring");
			return;
		}

		String target_lun = getProject().getProperty(modifyPrefix() + UnitProperties.UnitTargetLunSuffix);
		if (target_lun == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify a target lun. Assuming 0");
			target_lun = "0";
		}

		String target_should_attach = getProject().getProperty(modifyPrefix() + UnitProperties.UnitTargetShouldAttachSuffix);
		if (target_should_attach == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify if it should be attached. Assuming no.");
			target_should_attach = "no";
		}

		String target_chap_user = getProject().getProperty(modifyPrefix() + UnitProperties.UnitTargetChapUserSuffix);
		if (target_chap_user == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify chap user name. Assuming empty string.");
			target_chap_user ="";
		}

		String target_chap_secret = getProject().getProperty(modifyPrefix() + UnitProperties.UnitTargetChapSecretSuffix);
		if (target_chap_secret == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify chap secret. Assuming empty string.");
			target_chap_secret ="";
		}


		String fs_type = getProject().getProperty(modifyPrefix() + UnitProperties.UnitFSTypeSuffix);
		if (fs_type == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify file system type. Assuming empty string.");
			fs_type ="";
		}


		String fs_options = getProject().getProperty(modifyPrefix() + UnitProperties.UnitFSOptionsSuffix);
		if (fs_options == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify file system options. Assuming empty string.");
			fs_options ="";
		}


		String fs_should_format = getProject().getProperty(modifyPrefix() + UnitProperties.UnitFSShouldFormatSuffix);
		if (fs_should_format == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify if the file system should be formatted. Assuming no.");
			fs_should_format ="no";
		}

		String fs_mount_point = getProject().getProperty(modifyPrefix() + UnitProperties.UnitFSMountPointSuffix);
		if (fs_mount_point == null) {
			System.out.println("Store " + modifyPrefix() + " does not specify file system mount point. Assuming empty string (i.e. file system will not be mounted).");
			fs_mount_point ="";
		}

		bashStorageCommands(out, 100 + modifyIndex, store_type, target_ip, target_port, target_lun, 
				target_chap_user, target_chap_secret, target_should_attach, 
				fs_type, fs_options, fs_should_format, fs_mount_point);
	}

	public void execute() throws BuildException {
		try {
			super.execute();
			if (file == null) {
				throw new Exception("Missing file parameter");
			}
			PrintWriter out = new PrintWriter(new FileWriter(new File(file)));

			generateInterface(out);
			generateStorage(out);
			out.close();
		} catch (BuildException e) {
			throw e;
		} catch (Exception e) {
			throw new BuildException("An error occurred: " + e.getMessage(), e);
		}
	}

}
