package orca.controllers.xmlrpc;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

		try {
			IOrcaServiceManager sm = XmlrpcOrcaState.getInstance().getSM();
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
			
			Properties pr_local=null;
			int p = 0;
			String r_id=null,isNetwork=null,isLun=null;
			ReservationMng p_r = null;
			String unit_tag = null,unit_parent_url=null;
			if(p_str!=null){
				p=Integer.valueOf(p_str);
				System.out.println("Number of parent reservations:"+p+";num_interface="+num_interface_int+";num_storage="+num_storage_int);
				for(int i=0;i<p;i++){
					Properties modifyProperties=new Properties();
					String key=ReservationConverter.PropertyNewParent + String.valueOf(i);
					r_id=local.getProperty(key);
					if(r_id!=null){
						p_r = sm.getReservation(new ReservationID(r_id));
						pr_local=OrcaConverter.fill(p_r.getLocalProperties());
						if(p_r!=null){
							isNetwork = pr_local.getProperty(ReservationConverter.PropertyIsNetwork);
							isLun = pr_local.getProperty(ReservationConverter.PropertyIsLUN);

							pr_local.clear();
							if(isNetwork!=null && isNetwork.equals("1")){	//Parent is a networking reservation
								List<UnitMng> un = sm.getUnits(new ReservationID(p_r.getReservationID()));
								if (un != null) {
									for (UnitMng u : un) {
										pr_local = OrcaConverter.fill(u.getProperties());										
										if (pr_local.getProperty(UnitProperties.UnitVlanTag) != null)
											unit_tag = pr_local.getProperty(UnitProperties.UnitVlanTag);
										if (pr_local.getProperty(UnitProperties.UnitVlanUrl) != null)
											unit_parent_url = pr_local.getProperty(UnitProperties.UnitVlanUrl);
									}
								}
								System.out.println("parent r_id="+r_id+";unit tag:"+unit_tag+";unit_parent_url:"+unit_parent_url);
								if(unit_tag!=null){
									host_interface=StringProcessor.getHostInterface(local,unit_parent_url);
									if(host_interface==null){
										System.out.println("Not find the parent interace index:unit_tag="+unit_tag);
										continue;
									}
										
									String parent_tag_name = parent_prefix.concat(host_interface).concat(".vlan.tag");
									modifyProperties.setProperty("vlan.tag",unit_tag);
									String parent_mac_addr = parent_prefix+host_interface+".mac";
									String parent_ip_addr = parent_prefix+host_interface+".ip";
									String parent_quantum_uuid = parent_prefix+host_interface+UnitProperties.UnitEthNetworkUUIDSuffix;
									String parent_interface_uuid = parent_prefix+host_interface+".uuid";
									String site_host_interface = parent_prefix + host_interface + ".hosteth";
										
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
										
									System.out.println("modifycommand:"+modifySubcommand+":properties:"+modifyProperties.toString());
									ModifyHelper.enqueueModify(reservation_id.toString(), modifySubcommand, modifyProperties);
								}else{	//no need to go futher
									System.out.println("Parent doesnot return the unit tag:"+pr_local);
									continue;
								}
							}
							//parent is lun 
							if(isLun!=null && isLun.equals("1")){	//Parent is a storage reservation
								List<UnitMng> un = sm.getUnits(new ReservationID(p_r.getReservationID()));
								if (un != null) {
									for (UnitMng u : un) {
										pr_local = OrcaConverter.fill(u.getProperties());
										if (pr_local.getProperty(UnitProperties.UnitLUNTag) != null)
											unit_tag = pr_local.getProperty(UnitProperties.UnitLUNTag);
									}
								}
								System.out.println("isLun="+isLun+";parent unit lun tag:"+unit_tag
										+";r_id="+r_id+";p_r_id="+p_r.getReservationID());
								if(unit_tag!=null){
									modifyProperties.setProperty("target.lun.num",unit_tag);
									host_interface=StringProcessor.getHostInterface(local,p_r);
									if(host_interface==null){
										System.out.println("Not find the parent interace index:unit_tag="+unit_tag);
										continue;
									}
									String parent_tag_name = parent_prefix.concat(host_interface).concat(".vlan.tag");
									String parent_mac_addr = parent_prefix+host_interface+".mac";
									String parent_ip_addr = parent_prefix+host_interface+".ip";
									String site_host_interface = parent_prefix + host_interface + ".hosteth";
										
									if(local.getProperty(parent_tag_name)!=null)
										modifyProperties.setProperty("vlan.tag",local.getProperty(parent_tag_name));
									if(local.getProperty(parent_mac_addr)!=null)
										modifyProperties.setProperty("mac",local.getProperty(parent_mac_addr));
									if(local.getProperty(parent_ip_addr)!=null)
										modifyProperties.setProperty("ip",local.getProperty(parent_ip_addr));
									if(local.getProperty(site_host_interface)!=null)
										modifyProperties.setProperty("hosteth",local.getProperty(site_host_interface));
									
									String storagePrefix = UnitProperties.UnitStoragePrefix+String.valueOf(num_storage_int);
									String storageTargetPrefix = storagePrefix + ".target";  
									String storageFSPrefix = storagePrefix + ".fs"; 
									
									//Other properties 
									if(local.getProperty(UnitProperties.UnitISCSIInitiatorIQN)!=null)
										modifyProperties.setProperty("iscsi.initiator.iqn",local.getProperty(UnitProperties.UnitISCSIInitiatorIQN));

									if(local.getProperty(storageTargetPrefix+".ip")!=null)
										modifyProperties.setProperty("target.ip",local.getProperty(storageTargetPrefix+".ip"));

									if(local.getProperty(storageTargetPrefix+".lun.guid")!=null)
										modifyProperties.setProperty("lun.guid",local.getProperty(storageTargetPrefix+".lun.guid"));
									
									if(local.getProperty(storageFSPrefix+".type")!=null)
										modifyProperties.setProperty("fs.type",local.getProperty(storageFSPrefix+".type"));
									else
										modifyProperties.setProperty("fs.type","ext3");
									
									if(local.getProperty(storageTargetPrefix+".options")!=null)
										modifyProperties.setProperty("options",local.getProperty(storageTargetPrefix+".options"));
									else
										modifyProperties.setProperty("options","-F -b 1024");
									
									if(local.getProperty(storageTargetPrefix+".mount_point")!=null)
										modifyProperties.setProperty("mount_point",local.getProperty(storageTargetPrefix+".mount_point"));
									else
										modifyProperties.setProperty("mount_point","/mnt/target/"+String.valueOf(host_interface));
									
									if(local.getProperty(storageTargetPrefix+".should_format")!=null)
										modifyProperties.setProperty("should_format",local.getProperty(storageTargetPrefix+".should_format"));
									else
										modifyProperties.setProperty("should_format",ReservationConverter.SUDO_NO);
									
									if(local.getProperty(storageTargetPrefix+".chap_user")!=null)
										modifyProperties.setProperty("chap_user",local.getProperty(storageTargetPrefix+".chap_user"));
									if(local.getProperty(storageTargetPrefix+".chap_password")!=null)
										modifyProperties.setProperty("chap_password",local.getProperty(storageTargetPrefix+".chap_password"));
									
									if(local.getProperty(storagePrefix+".type")!=null)
										modifyProperties.setProperty("type",local.getProperty(storagePrefix+".type"));
									else	
										modifyProperties.setProperty("type","iscsi");
									if(local.getProperty(storageTargetPrefix+".port")!=null)
										modifyProperties.setProperty("port",local.getProperty(storageTargetPrefix+".port"));
									else
										modifyProperties.setProperty("port","3260");
										
									if(local.getProperty(storageTargetPrefix+".should_attach")!=null)
										modifyProperties.setProperty("should_attach",local.getProperty(storageTargetPrefix+".should_attach"));
									else
										modifyProperties.setProperty("should_attach",ReservationConverter.SUDO_YES);
									num_storage_int--;
									System.out.println("modifycommand:"+modifySubcommand+":properties:"+modifyProperties.toString());
									ModifyHelper.enqueueModify(reservation_id.toString(), modifySubcommand, modifyProperties);
								}else{	//no need to go futher
									System.out.println("Parent doesnot return the unit lun tag:"+pr_local);
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
