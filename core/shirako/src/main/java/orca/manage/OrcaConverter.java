package orca.manage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import orca.manage.beans.AuthTokenMng;
import orca.manage.beans.LeaseReservationStateMng;
import orca.manage.beans.PoolInfoMng;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ReservationStateMng;
import orca.security.AuthToken;
import orca.shirako.common.ConfigurationException;
import orca.shirako.common.ReservationID;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.util.ID;
import orca.util.PropList;

public class OrcaConverter {
	public static PropertiesMng fill(Properties p) {
		PropertiesMng mng = new PropertiesMng();

		if ((p != null) && (p.size() > 0)) {
			Set<?> set = p.entrySet();
			Iterator<?> iter = set.iterator();
			while (iter.hasNext()) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
				PropertyMng pp = new PropertyMng();
				pp.setName((String) entry.getKey());
				pp.setValue((String) entry.getValue());
				mng.getProperty().add(pp);
			}
		}

		return mng;
	}

	public static Properties fill(PropertiesMng mng) {
		Properties p = new Properties();

		if (mng != null) {
			List<PropertyMng> props = mng.getProperty();

			for (PropertyMng pp : props) {
				p.setProperty(pp.getName(), pp.getValue());
			}
		}

		return p;
	}

	public static AuthTokenMng fill(AuthToken auth) {
		if (auth == null) {
			return null;
		}

		AuthTokenMng result = new AuthTokenMng();
		result.setName(auth.getName());
		if (auth.getGuid() != null) {
			result.setGuid(auth.getGuid().toString());
		}
		result.setLoginToken(auth.getLoginToken());
		return result;
	}

	public static AuthToken fill(AuthTokenMng auth) {
		if (auth == null) {
			return null;
		}
		ID guid = null;
		if (auth.getGuid() != null) {
			guid = new ID(auth.getGuid());
		}

		AuthToken result = new AuthToken(auth.getName(), guid);
		result.setLoginToken(auth.getLoginToken());

		return result;
	}

	public static ResourcePoolDescriptor fill(PoolInfoMng pool) throws ConfigurationException {
		ResourcePoolDescriptor rpd = new ResourcePoolDescriptor();
		Properties p = OrcaConverter.fill(pool.getProperties());
		rpd.reset(p);
		return rpd;
	}
	
	public static PropertiesMng unset(Properties from, PropertiesMng into){
		Properties dest = fill(into);
		PropList.unsetProperties(from, dest);
		return fill(dest);
	}
	
	public static PropertiesMng merge(Properties from, PropertiesMng into){
		Properties dest = fill(into);
		PropList.mergeProperties(from, dest);
		return fill(dest);
	}
	
	private static PropertyMng getProperty(PropertiesMng p, String name) {
		for (PropertyMng pp : p.getProperty()){
			if (pp.getName().equals(name)){
				return pp;
			}
		}
		return null;
	}
	
	public static String getLocalProperty(ReservationMng r, String name){
		PropertyMng p = getProperty(r.getLocalProperties(), name);
		if (p == null){
			return null;
		}
		return p.getValue();
	}

	public static String getConfigurationProperty(ReservationMng r, String name){
		PropertyMng p = getProperty(r.getConfigurationProperties(), name);
		if (p == null){
			return null;
		}
		return p.getValue();
	}

	public static String getRequestProperty(ReservationMng r, String name){
		PropertyMng p = getProperty(r.getRequestProperties(), name);
		if (p == null){
			return null;
		}
		return p.getValue();
	}
	
	public static String getResourceProperty(ReservationMng r, String name){
		PropertyMng p = getProperty(r.getResourceProperties(), name);
		if (p == null){
			return null;
		}
		return p.getValue();
	}


	public static void setProperty(PropertiesMng p, String name, String value){
		PropertyMng pp = getProperty(p, name);
		if (pp != null){
			pp.setValue(value);
		}else {
			pp = new PropertyMng();
			pp.setName(name);
			pp.setValue(value);
			p.getProperty().add(pp);
		}
	}
	
	public static void setLocalProperty(ReservationMng r, String name, String value){
		setProperty(r.getLocalProperties(), name, value);
	}

	public static void setConfigurationProperty(ReservationMng r, String name, String value){
		setProperty(r.getConfigurationProperties(), name, value);
	}
	
	public static void setRequestProperty(ReservationMng r, String name, String value){
		setProperty(r.getRequestProperties(), name, value);
	}

	public static void setResourceProperty(ReservationMng r, String name, String value){
		setProperty(r.getResourceProperties(), name, value);
	}

	public static String getState(ReservationStateMng state){
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(OrcaConstants.getReservationStateName(state.getState()));
		sb.append(",");
		sb.append(OrcaConstants.getReservationPendingStateName(state.getPending()));
		if (state instanceof LeaseReservationStateMng){
			sb.append(",");
			sb.append(OrcaConstants.getReservationPendingStateName(((LeaseReservationStateMng)state).getJoining()));
		}
		sb.append("]");
		return sb.toString();
	}
	
	public static boolean isActive(ReservationStateMng state){
		if (state.getState() == OrcaConstants.ReservationStateActive || state.getState() == OrcaConstants.ReservationStateActiveTicketed){
			if (state instanceof LeaseReservationStateMng){
				return (((LeaseReservationStateMng)state).getJoining() == OrcaConstants.ReservationJoinStateNoJoin);
			}else {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isTicketed(ReservationStateMng state){
		return (state.getState() == OrcaConstants.ReservationStateTicketed);
	}

	public static boolean isActiveTicketed(ReservationStateMng state){
		return (state.getState() == OrcaConstants.ReservationStateActiveTicketed);
	}

	public static boolean isClosed(ReservationStateMng state){
		return (state.getState() == OrcaConstants.ReservationStateClosed);
	}

	public static boolean isClosing(ReservationStateMng state){
		return (state.getPending() == OrcaConstants.ReservationPendingStateClosing);
	}

	public static boolean isFailed(ReservationStateMng state){
		return (state.getState() == OrcaConstants.ReservationStateFailed);
	}

	public static boolean isTerminal(ReservationStateMng state){
		return isClosed(state) || isFailed(state);
	}

	public static boolean hasNothingPending(ReservationStateMng state){
		if (state.getPending() == OrcaConstants.ReservationPendingStateNone){
			if (state instanceof LeaseReservationStateMng){
				return (((LeaseReservationStateMng)state).getJoining() == OrcaConstants.ReservationJoinStateNoJoin);
			}else {
				return true;
			}
		}
		return false;
	}
	
	public static boolean areTerminal(List<ReservationStateMng> list){
		for (ReservationStateMng s : list){
			if (s.getState() == OrcaConstants.ReservationStateUnknown){
				continue;
			}
			if (!isTerminal(s)){
				return false;
			}
		}
		return true;
	}

	public static boolean areTicketed(List<ReservationStateMng> list){
		for (ReservationStateMng s : list){
			if (s.getState() == OrcaConstants.ReservationStateUnknown){
				continue;
			}
			if (!isTicketed(s)){
				return false;
			}
		}
		return true;
	}

	public static boolean areActive(List<ReservationStateMng> list){
		for (ReservationStateMng s : list){
			if (s.getState() == OrcaConstants.ReservationStateUnknown){
				continue;
			}
			if (!isActive(s)){
				return false;
			}
		}
		return true;
	}

	public static boolean areClosed(List<ReservationStateMng> list){
		for (ReservationStateMng s : list){
			if (s.getState() == OrcaConstants.ReservationStateUnknown){
				continue;
			}
			if (!isClosed(s)){
				return false;
			}
		}
		return true;
	}

	public static boolean hasAtLeastOneFailed(List<ReservationStateMng> list){
		for (ReservationStateMng s : list){
			if (s.getState() == OrcaConstants.ReservationStateUnknown){
				continue;
			}
			if (isFailed(s)){
				return true;
			}
		}
		return false;
	}

	public static final int SLEEP_INTERVAL = 1000;
	
	public static boolean awaitActive(ReservationID rid, IOrcaServiceManager sm) throws Exception{
		ArrayList<ReservationID> list = new ArrayList<ReservationID>();
		list.add(rid);
		return awaitActive(list, sm);
	}
	
	public static boolean awaitActive(List<ReservationID> rids, IOrcaServiceManager sm) throws Exception{
		while(true){
			List<ReservationStateMng> s = sm.getReservationState(rids);
			if (areActive(s)) {
				return true;
			}
			if (hasAtLeastOneFailed(s)){
				return false;
			}
			Thread.sleep(SLEEP_INTERVAL);
		}
	}

	public static boolean awaitClosed(ReservationID rid, IOrcaServiceManager sm) throws Exception{
		ArrayList<ReservationID> list = new ArrayList<ReservationID>();
		list.add(rid);
		return awaitClosed(list, sm);
	}

	public static boolean awaitClosed(List<ReservationID> rids, IOrcaServiceManager sm) throws Exception{
		while(true){
			List<ReservationStateMng> s = sm.getReservationState(rids);
			if (areClosed(s)) {
				return true;
			}
			if (hasAtLeastOneFailed(s)){
				return false;
			}
			Thread.sleep(SLEEP_INTERVAL);
		}
	}

	public static boolean awaitTerminal(ReservationID rid, IOrcaServiceManager sm) throws Exception{
		ArrayList<ReservationID> list = new ArrayList<ReservationID>();
		list.add(rid);
		return awaitTerminal(list, sm);
	}

	public static boolean awaitTerminal(List<ReservationID> rids, IOrcaServiceManager sm) throws Exception{
		while(true){
			List<ReservationStateMng> s = sm.getReservationState(rids);
			if (areTerminal(s)) {
				return true;
			}
			if (hasAtLeastOneFailed(s)){
				return false;
			}
			Thread.sleep(SLEEP_INTERVAL);
		}
	}

}