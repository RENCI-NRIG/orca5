package orca.controllers.xmlrpc;

import java.util.List;
import java.util.Properties;

import orca.controllers.xmlrpc.statuswatch.IStatusUpdateCallback;
import orca.embed.policyhelpers.StringProcessor;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConverter;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.UnitMng;
import orca.shirako.common.ReservationID;
import orca.shirako.common.meta.UnitProperties;

public class ReservationDependencyStatusUpdate implements IStatusUpdateCallback<ReservationID> {
	
	ReservationMng reservation = null;

	@Override
	public void success(List<ReservationID> ok, List<ReservationID> actOn)
			throws StatusCallbackException {
		
		String parent_prefix = UnitProperties.UnitEthPrefix;
		String host_interface=null;
		String reservation_id = null;
		if(reservation!=null)
			reservation_id=reservation.getReservationID();
		if(actOn==null || ! (reservation_id.equals(actOn.get(0).toString()))){
			System.out.println("Empty modifying...."+actOn+";reservation="+reservation_id);
			return;
		}

		IOrcaServiceManager sm = null;
		try {
			sm = XmlrpcOrcaState.getInstance().getSM();
			System.out.println("SUCCESS ON MODIFY WATCH OF " + ok);
			//ok-parents
			//actOn-to be modified reservation
			String modifySubcommand = "addiface";

			// use the queueing version to avoid collisions with modified performed by the controller itself
			Properties local = OrcaConverter.fill(reservation.getLocalProperties());
			String  p_str = local.getProperty(ReservationConverter.PropertyNumNewParentReservations);
			String num_interface=local.getProperty(ReservationConverter.PropertyParentNumInterface);
			String num_storage=local.getProperty(ReservationConverter.PropertyParentNumStorage);
			int num_interface_int = 0, num_storage_int = 0;
			if(num_interface!=null)
				num_interface_int = Integer.valueOf(num_interface);
			if(num_storage!=null)
				num_storage_int = Integer.valueOf(num_storage);
			
			Properties pr_local=null,pr_u=null;
			int p = 0;
			String r_id=null,isNetwork=null,isLun=null;
			ReservationMng p_r = null;

			if(p_str!=null){
				p=Integer.valueOf(p_str);
				System.out.println("Number of parent reservations:"+p+";num_interface="+num_interface_int+";num_storage="+num_storage_int);
				for(int i=0;i<p;i++){
					String unit_tag = null,unit_parent_url=null;
					Properties modifyProperties=new Properties();
					String key=ReservationConverter.PropertyNewParent + String.valueOf(i);
					r_id=local.getProperty(key);
					if(r_id!=null){
						p_r = sm.getReservation(new ReservationID(r_id));
						pr_local=OrcaConverter.fill(p_r.getLocalProperties());
						if(p_r!=null){
							isNetwork = pr_local.getProperty(ReservationConverter.PropertyIsNetwork);
							isLun = pr_local.getProperty(ReservationConverter.PropertyIsLUN);

							if(isNetwork!=null && isNetwork.equals("1")){	//Parent is a networking reservation
								// seems it's possible for this to not produce a tag. Doing this in a loop with a fixed number of repetitions
								int n = 10;
								//while ((n-->0) && (unit_tag == null)) {
									List<UnitMng> un = sm.getUnits(new ReservationID(p_r.getReservationID()));
									if (un != null) {
										for (UnitMng u : un) {
											pr_u = OrcaConverter.fill(u.getProperties());										
											if (pr_u.getProperty(UnitProperties.UnitVlanTag) != null)
												unit_tag = pr_u.getProperty(UnitProperties.UnitVlanTag);
											if (pr_u.getProperty(UnitProperties.UnitVlanUrl) != null)
												unit_parent_url = pr_u.getProperty(UnitProperties.UnitVlanUrl);
										}
									}
									//if (unit_tag == null)
									//	Thread.sleep(1000L);
								//}
								System.out.println("parent r_id="+r_id+";unit tag:"+unit_tag+";unit_parent_url:"+unit_parent_url+";n=" + n);
								if(unit_tag!=null){
									host_interface=StringProcessor.getHostInterface(local,unit_parent_url);
									if(host_interface==null){
										System.out.println("Not find the parent interace index:unit_tag="+unit_tag);
										continue;
									}
										
									String parent_tag_name = parent_prefix.concat(host_interface).concat(UnitProperties.UnitEthVlanSuffix);
									modifyProperties.setProperty("vlan.tag",unit_tag);
									String parent_mac_addr = parent_prefix+host_interface+UnitProperties.UnitEthMacSuffix;
									String parent_ip_addr = parent_prefix+host_interface+UnitProperties.UnitEthIPSuffix;
									String parent_quantum_uuid = parent_prefix+host_interface+UnitProperties.UnitEthNetworkUUIDSuffix;
									String parent_interface_uuid = parent_prefix+host_interface+".uuid";
									String site_host_interface = parent_prefix + host_interface + UnitProperties.UnitHostEthSuffix;
									String parent_url = parent_prefix + host_interface + ".parent.url";	
									
									if(local.getProperty(parent_mac_addr)!=null)
										modifyProperties.setProperty("mac",local.getProperty(parent_mac_addr));
									if(local.getProperty(parent_ip_addr)!=null)
										modifyProperties.setProperty("ip",local.getProperty(parent_ip_addr));
									if(local.getProperty(parent_quantum_uuid)!=null)
										modifyProperties.setProperty("net.uuid",local.getProperty(parent_quantum_uuid));
									if(local.getProperty(parent_interface_uuid)!=null)
										modifyProperties.setProperty("uuid",local.getProperty(parent_interface_uuid));
									if(local.getProperty(site_host_interface)!=null)
										modifyProperties.setProperty("hosteth",local.getProperty(site_host_interface));
									if(local.getProperty(parent_url)!=null)
										modifyProperties.setProperty("parent.url",local.getProperty(parent_url));
										
									System.out.println("modifycommand:"+modifySubcommand+":properties:"+modifyProperties.toString());
									ModifyHelper.enqueueModify(reservation_id.toString(), modifySubcommand, modifyProperties);
								}else{	//no need to go futher
									System.out.println("Parent doesnot return the unit tag:"+pr_u);
									continue;
								}
							}
							//parent is lun 
							if(isLun!=null && isLun.equals("1")){	//Parent is a storage reservation
								// seems it's possible for this to not produce a tag. Doing this in a loop with a fixed number of repetitions
								int n = 10;
								//while ((n-->0) && (unit_tag == null)) {
									List<UnitMng> un = sm.getUnits(new ReservationID(p_r.getReservationID()));
									if (un != null) {
										for (UnitMng u : un) {
											pr_u = OrcaConverter.fill(u.getProperties());
											if (pr_u.getProperty(UnitProperties.UnitLUNTag) != null)
												unit_tag = pr_u.getProperty(UnitProperties.UnitLUNTag);
										}
									}
									//if (unit_tag == null)
									//	Thread.sleep(1000L);
								//}
								
								String parent_url=StringProcessor.getParentURL(local,pr_local);
								System.out.println("isLun="+isLun+";parent unit lun tag:"+unit_tag
										+";r_id="+r_id+";p_r_id="+p_r.getReservationID()+";parent_url=" + parent_url);
								if(unit_tag!=null){
									modifyProperties.setProperty("target.lun.num",unit_tag);
									if(parent_url!=null)
										modifyProperties.setProperty("parent.url", parent_url);
									host_interface=StringProcessor.getHostInterface(local,p_r);
									if(host_interface==null){
										System.out.println("Not find the parent interace index:unit_tag="+unit_tag);
										continue;
									}
									String parent_tag_name = parent_prefix.concat(host_interface).concat(UnitProperties.UnitEthVlanSuffix);
									String parent_mac_addr = parent_prefix+host_interface+UnitProperties.UnitEthMacSuffix;
									String parent_ip_addr = parent_prefix+host_interface+UnitProperties.UnitEthIPSuffix;
									String site_host_interface = parent_prefix + host_interface + UnitProperties.UnitHostEthSuffix;
										
									if(local.getProperty(parent_tag_name)!=null)
										modifyProperties.setProperty("vlan.tag",local.getProperty(parent_tag_name));
									if(local.getProperty(parent_mac_addr)!=null)
										modifyProperties.setProperty("mac",local.getProperty(parent_mac_addr));
									if(local.getProperty(parent_ip_addr)!=null)
										modifyProperties.setProperty("ip",local.getProperty(parent_ip_addr));
									if(local.getProperty(site_host_interface)!=null)
										modifyProperties.setProperty("hosteth",local.getProperty(site_host_interface));
									
									String storagePrefix = UnitProperties.UnitStoragePrefix+String.valueOf(num_storage_int);
									//String storageTargetPrefix = storagePrefix + ".target";  
									//String storageFSPrefix = storagePrefix + ".fs"; 
									
									//Other properties 
									if(local.getProperty(UnitProperties.UnitISCSIInitiatorIQN)!=null)
										modifyProperties.setProperty("iscsi.initiator.iqn",local.getProperty(UnitProperties.UnitISCSIInitiatorIQN));

									if(local.getProperty(storagePrefix + UnitProperties.UnitTargetIPSuffix)!=null)
										modifyProperties.setProperty("target.ip",local.getProperty(storagePrefix + UnitProperties.UnitTargetIPSuffix));

									if(local.getProperty(storagePrefix + UnitProperties.UnitTargetLunGuid)!=null)
										modifyProperties.setProperty("target.lun.guid",local.getProperty(storagePrefix + UnitProperties.UnitTargetLunGuid));
									
									if(local.getProperty(storagePrefix + UnitProperties.UnitFSTypeSuffix)!=null)
										modifyProperties.setProperty("fs.type",local.getProperty(storagePrefix + UnitProperties.UnitFSTypeSuffix));
									else
										modifyProperties.setProperty("fs.type","ext3");
									
									if(local.getProperty(storagePrefix + UnitProperties.UnitFSOptionsSuffix)!=null)
										modifyProperties.setProperty("target.options",local.getProperty(storagePrefix + UnitProperties.UnitFSOptionsSuffix));
									else
										modifyProperties.setProperty("target.options","-F -b 1024");
									
									if(local.getProperty(storagePrefix + UnitProperties.UnitFSMountPointSuffix)!=null)
										modifyProperties.setProperty("target.mount_point",local.getProperty(storagePrefix + UnitProperties.UnitFSMountPointSuffix));
									else
										modifyProperties.setProperty("target.mount_point","/mnt/target/"+String.valueOf(host_interface));
									
									if(local.getProperty(storagePrefix + UnitProperties.UnitFSShouldFormatSuffix)!=null)
										modifyProperties.setProperty("target.should_format",local.getProperty(storagePrefix + UnitProperties.UnitFSShouldFormatSuffix));
									else
										modifyProperties.setProperty("target.should_format",ReservationConverter.SUDO_NO);
									
									if(local.getProperty(storagePrefix + UnitProperties.UnitTargetChapUserSuffix)!=null)
										modifyProperties.setProperty("target.chap_user",local.getProperty(storagePrefix + UnitProperties.UnitTargetChapUserSuffix));
									if(local.getProperty(storagePrefix + UnitProperties.UnitTargetChapSecretSuffix)!=null)
										modifyProperties.setProperty("target.chap_password",local.getProperty(storagePrefix + UnitProperties.UnitTargetChapSecretSuffix));
									
									if(local.getProperty(storagePrefix+UnitProperties.UnitStoreTypeSuffix)!=null)
										modifyProperties.setProperty("type",local.getProperty(storagePrefix+UnitProperties.UnitStoreTypeSuffix));
									else	
										modifyProperties.setProperty("type","iscsi");
									if(local.getProperty(storagePrefix + UnitProperties.UnitTargetPortSuffix)!=null)
										modifyProperties.setProperty("target.port",local.getProperty(storagePrefix + UnitProperties.UnitTargetPortSuffix));
									else
										modifyProperties.setProperty("target.port","3260");
										
									if(local.getProperty(storagePrefix + UnitProperties.UnitTargetShouldAttachSuffix)!=null)
										modifyProperties.setProperty("target.should_attach",local.getProperty(storagePrefix + UnitProperties.UnitTargetShouldAttachSuffix));
									else
										modifyProperties.setProperty("target.should_attach",ReservationConverter.SUDO_YES);
									num_storage_int--;
									System.out.println("modifycommand:"+modifySubcommand+":properties:"+modifyProperties.toString());
									ModifyHelper.enqueueModify(reservation_id.toString(), modifySubcommand, modifyProperties);
								}else{	//no need to go futher
									System.out.println("Parent did not return the unit lun tag:"+pr_u);
									continue;
								}
							}
						}
					}
				}
			}
			local.setProperty(ReservationConverter.PropertyParentNumInterface,String.valueOf(num_interface));
			reservation.setLocalProperties(OrcaConverter.merge(local, reservation.getLocalProperties()));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (sm != null)
				XmlrpcOrcaState.getInstance().returnSM(sm);
		}

	}
	
	@Override
	public void failure(List<ReservationID> failed, List<ReservationID> ok,
			List<ReservationID> actOn) throws StatusCallbackException {
		System.out.println("FAILURE ON MODIFY WATCH OF " + failed);

	}

	public ReservationMng getReservation() {
		return reservation;
	}

	public void setReservation(ReservationMng reservation) {
		this.reservation = reservation;
	}
}
