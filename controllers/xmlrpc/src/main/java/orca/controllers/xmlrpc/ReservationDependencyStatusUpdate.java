package orca.controllers.xmlrpc;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import orca.controllers.xmlrpc.statuswatch.IStatusUpdateCallback;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConverter;
import orca.manage.beans.ReservationMng;
import orca.shirako.common.ReservationID;
import orca.shirako.common.meta.UnitProperties;

public class ReservationDependencyStatusUpdate implements IStatusUpdateCallback {
	
	ReservationMng reservation = null;
	
	@Override
	public void success(List<ReservationID> ok, List<ReservationID> actOn)
			throws StatusCallbackException {
		
		String parent_prefix = "unit.eth";
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
			Properties modifyProperties=new Properties();

			// use the queueing version to avoid collisions with modified performed by the controller itself
			Properties local = OrcaConverter.fill(reservation.getLocalProperties());
			String  p_str = local.getProperty(ReservationConverter.PropertyNumNewParentReservations);
			String num_interface=local.getProperty(ReservationConverter.parent_num_interface);
			int num_interface_int = Integer.valueOf(num_interface);
			Properties pr_local=null;
			int p = 0;
			String r_id=null,isWhat=null;
			ReservationMng p_r = null;
			if(p_str!=null){
				p=Integer.valueOf(p_str);
				for(int i=0;i<p;i++){
					String key=ReservationConverter.PropertyNewParent + String.valueOf(i);
					r_id=local.getProperty(key);
					if(r_id!=null){
						p_r = sm.getReservation(new ReservationID(r_id));
						pr_local=OrcaConverter.fill(p_r.getLocalProperties());
						if(p_r!=null){
							System.out.println("Extracting property of parent: " + p_r.getLocalProperties());
							isWhat = local.getProperty(ReservationConverter.PropertyIsNetwork);
							if(isWhat!=null && isWhat.equals("1")){	//Parent is a networking reservation
								String unit_tag = pr_local.getProperty(UnitProperties.UnitVlanTag);
								System.out.println("parent unit tag:"+unit_tag+";host intf="+num_interface_int);
								if(unit_tag!=null){
									num_interface_int++;
									host_interface=String.valueOf(num_interface_int);
									String parent_tag_name = parent_prefix.concat(host_interface).concat(".vlan.tag");
									modifyProperties.setProperty(parent_tag_name,unit_tag);
									ModifyHelper.enqueueModify(reservation_id.toString(), modifySubcommand, modifyProperties);
								}else{	//no need to go futher
									System.out.println("Parent doesnot return the unit tag:"+pr_local);
									continue;
								}
							}
						}
					}
				}
				local.setProperty(ReservationConverter.parent_num_interface,String.valueOf(num_interface));
				reservation.setLocalProperties(OrcaConverter.merge(local, reservation.getLocalProperties()));
			}			

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
