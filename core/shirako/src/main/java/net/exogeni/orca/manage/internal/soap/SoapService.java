package net.exogeni.orca.manage.internal.soap;

import java.util.List;

import net.exogeni.orca.manage.beans.ResultMng;

public abstract class SoapService {	
	protected void updateStatus(ResultMng incoming, ResultMng outgoing){
		outgoing.setCode(incoming.getCode());
		outgoing.setDetails(incoming.getDetails());
		outgoing.setMessage(incoming.getMessage());
	}
	
	
	public static <T> T getFirst(List<T> list){
		if (list == null || list.size() == 0) {return null;}
		return list.get(0);
	}
}
