package orca.embed.policyhelpers;

import java.util.Properties;
import java.util.Map.Entry;

import orca.manage.OrcaConverter;
import orca.manage.beans.ReservationMng;
import orca.shirako.common.meta.UnitProperties;

public class StringProcessor {
	
	public static String getHostInterface(Properties local,String unit_parent_url){
		String key=null,value=null,host_interface=null;
		if ((local == null) || local.isEmpty() || (unit_parent_url == null) || !local.containsValue(unit_parent_url))
			return null;
		for(Entry<Object, Object> entry:local.entrySet()){
			key=(String) entry.getKey();
			value=(String) entry.getValue();
			if(value.equals(unit_parent_url)){
				String url = null;
				if(key.contains(UnitProperties.UnitEthPrefix))
					url=key.split(UnitProperties.UnitEthPrefix)[1];
				if(key.contains("modify."))
					url=key.split("modify.")[1];
				if(url!=null){
					int index=url.indexOf(".parent.url");
					host_interface=url.substring(0, index);
					break;
				}
			}
		}
		return host_interface;
	}

	public static String getHostInterface(Properties local,ReservationMng p_r){
		String p_key=null,p_value=null,host_interface=null;
		Properties p_r_local = OrcaConverter.fill(p_r.getLocalProperties());
		if(local.isEmpty() || p_r_local.isEmpty())
			return null;
		//System.out.println("local:"+local.toString());
		//System.out.println("p_r local:"+p_r_local.toString());
		for(Entry<Object, Object> entry:p_r_local.entrySet()){
			p_key=(String) entry.getKey();
			p_value=(String) entry.getValue();
			
			if(p_key.endsWith(".parent.url")){
				//System.out.println("p_key="+p_key+"p_value="+p_value);
				host_interface=getHostInterface(local,p_value);
				break;
			}
				
		}
		return host_interface;
	}
	
	public static String getParentURL(Properties local,Properties pr_local){
		String key=null,value=null,p_key=null,p_value=null,parent_url=null;
		
		if(local.isEmpty() || pr_local.isEmpty())
			return null;
		//System.out.println("local:"+local.toString());
		//System.out.println("p_r local:"+p_r_local.toString());
		for(Entry<Object, Object> pr_entry:pr_local.entrySet()){
			p_key=(String) pr_entry.getKey();
			p_value=(String) pr_entry.getValue();
			if(p_key.endsWith(".parent.url")){
				for(Entry<Object, Object> entry:local.entrySet()){
					key=(String) entry.getKey();
					value=(String) entry.getValue();
					if(value.equals(p_value)){
						parent_url=value;
						break;
					}
				}
				if(parent_url!=null)
					break;
			}
		}
		return parent_url;
	}
}
