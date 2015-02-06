/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage.internal;

import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import orca.extensions.internal.ExtensionPackage;
import orca.extensions.internal.Plugin;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.OrcaProxyProtocolDescriptor;
import orca.manage.beans.ActorMng;
import orca.manage.beans.AuthTokenMng;
import orca.manage.beans.CertificateMng;
import orca.manage.beans.ClientMng;
import orca.manage.beans.ConfigMappingMng;
import orca.manage.beans.EventMng;
import orca.manage.beans.GenericEventMng;
import orca.manage.beans.LeaseReservationMng;
import orca.manage.beans.LeaseReservationStateMng;
import orca.manage.beans.ListMng;
import orca.manage.beans.PackageMng;
import orca.manage.beans.PluginMng;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.ProtocolProxyMng;
import orca.manage.beans.ProxyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ReservationStateMng;
import orca.manage.beans.ReservationStateTransitionEventMng;
import orca.manage.beans.SliceMng;
import orca.manage.beans.TicketReservationMng;
import orca.manage.beans.UnitMng;
import orca.manage.beans.UserMng;
import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IAuthorityProxy;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IEvent;
import orca.shirako.api.IProxy;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.container.Globals;
import orca.shirako.container.OrcaContainer;
import orca.shirako.container.api.IActorContainer;
import orca.shirako.core.Actor;
import orca.shirako.core.ActorIdentity;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.kernel.ReservationFactory;
import orca.shirako.kernel.ReservationStateTransitionEvent;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.plugins.config.ConfigurationMapping;
import orca.shirako.proxies.ActorLocation;
import orca.shirako.proxies.Proxy;
import orca.shirako.proxies.local.LocalProxy;
import orca.shirako.proxies.soapaxis2.SoapAxis2Proxy;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.Term;
import orca.shirako.util.Client;
import orca.shirako.util.ReservationState;
import orca.shirako.util.ResourceData;
import orca.util.Base64;
import orca.util.ID;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.PersistenceUtils;

/**
 * This class provides a collection of static functions for converting Shirako
 * core objects to objects from the management layer and vice-versa.
 */
public class Converter extends OrcaConverter {
	public static final int TypeReservationClient = 1;
	public static final int TypeCodReservation = 2;
	private static SimpleDateFormat dateformat = new SimpleDateFormat("MM/dd/yyyy HH:mm");

	public static void absorbProperties(ReservationMng mng, IReservation r){
		Properties p = fill(mng.getLocalProperties());
		PropList.mergeProperties(p, r.getResources().getLocalProperties());
		
		fill(mng.getRequestProperties());
		PropList.mergeProperties(p, r.getResources().getRequestProperties());
	
		fill(mng.getResourceProperties());
		PropList.mergeProperties(p, r.getResources().getResourceProperties());
	
		fill(mng.getConfigurationProperties());
		PropList.mergeProperties(p, r.getResources().getConfigurationProperties());
	}
	
	public static void absorbProperties(SliceMng mng, ISlice s){
		Properties p = fill(mng.getLocalProperties());
		PropList.mergeProperties(p, s.getLocalProperties());
		
		fill(mng.getRequestProperties());
		PropList.mergeProperties(p, s.getRequestProperties());
	
		fill(mng.getResourceProperties());
		PropList.mergeProperties(p, s.getResourceProperties());
	
		fill(mng.getConfigurationProperties());
		PropList.mergeProperties(p, s.getConfigurationProperties());
	}

	public static void attachProperties(ReservationMng mng, IReservation r) {
		// XXX: solve the mess: too many locations that can contain properties
		// still a big mess
		Properties resourceProperties = null;
		Properties localProperties = null;
		Properties configurationProperties = null;
		Properties requestProperties = null;

		if ((r instanceof IServiceManagerReservation)) {
			IServiceManagerReservation rc = (IServiceManagerReservation) r;

			if (r.isActive()) {
				resourceProperties = rc.getLeasedResources().getResourceProperties();
				configurationProperties = rc.getLeasedResources().getConfigurationProperties();
				requestProperties = rc.getLeasedResources().getRequestProperties();
			} else {
				resourceProperties = rc.getResources().getResourceProperties();
				configurationProperties = rc.getResources().getConfigurationProperties();
				requestProperties = rc.getResources().getRequestProperties();
			}
			localProperties = rc.getResources().getLocalProperties();
		} else {
			ResourceSet rset = r.getResources();

			if (rset != null) {
				resourceProperties = rset.getResourceProperties();
				localProperties = rset.getLocalProperties();
				configurationProperties = rset.getConfigurationProperties();
				requestProperties = rset.getRequestProperties();
			}
		}

		Properties ticketProperties = null;
		ResourceSet resources = r.getResources();

		if (resources != null) {
			IConcreteSet cs = resources.getResources();

			if ((cs != null) && cs instanceof Ticket) {
				ticketProperties = ((Ticket) cs).getProperties();
			}
		}

		PropertiesMng configuration = Converter.fill(configurationProperties);
		PropertiesMng local = Converter.fill(localProperties);
		PropertiesMng request = Converter.fill(requestProperties);
		PropertiesMng resource = Converter.fill(resourceProperties);
		PropertiesMng ticket = Converter.fill(ticketProperties);

		mng.setConfigurationProperties(configuration);
		mng.setLocalProperties(local);
		mng.setRequestProperties(request);
		mng.setResourceProperties(resource);
		if (mng instanceof TicketReservationMng){
			((TicketReservationMng)mng).setTicketProperties(ticket);
		}
	}

	public static void attachProperties(SliceMng mng, ISlice sls) {
		if (sls.getRequestProperties() != null) {
			mng.setRequestProperties(Converter.fill(sls.getRequestProperties()));
		}

		if (sls.getResourceProperties() != null) {
			mng.setResourceProperties(Converter.fill(sls.getResourceProperties()));
		}

		if (sls.getConfigurationProperties() != null) {
			mng.setConfigurationProperties(Converter.fill(sls.getConfigurationProperties()));
		}

		if (sls.getLocalProperties() != null) {
			mng.setLocalProperties(Converter.fill(sls.getLocalProperties()));
		}
	}

	public static Properties decodeProperties(String p) {
		Properties result = new Properties();

		if (p != null) {
			String[] items = p.split(" ");
			int i = 0;

			while (i < items.length) {
				String temp = items[i].trim();

				if (!temp.startsWith("name")) {
					i++;

					continue;
				}

				int start = temp.indexOf("'");
				int end = temp.lastIndexOf("'");
				String name = temp.substring(start + 1, end);

				i++;

				if (i < items.length) {
					temp = items[i].trim();

					if (!temp.startsWith("value")) {
						i++;

						continue;
					}
				} else {
					break;
				}

				start = temp.indexOf("'");
				end = temp.lastIndexOf("'");

				String value = temp.substring(start + 1, end);
				value = new String(Base64.decode(value));
				result.setProperty(name, value);

				i++;
			}
		}

		return result;
	}

	// public static ActorMng[] fill(ArrayList<ActorWrapper> list)
	// {
	// ActorMng[] result = null;
	// if (list != null && list.size() > 0) {
	// result = new ActorMng[list.size()];
	// for (int i = 0; i < list.size(); i++) {
	// result[i] = fill(list.get(i));
	// }
	// }
	// return result;
	// }
	public static void fillActor(List<ActorMng> l, ArrayList<IActor> list) {
		if ((list != null) && (list.size() > 0)) {
			for (int i = 0; i < list.size(); i++) {
				l.add(fill(list.get(i)));
			}
		}
	}

	public static ConfigurationMapping fill(ConfigMappingMng mng) {
		ConfigurationMapping m = new ConfigurationMapping();
		m.setKey(mng.getKey());
		m.setConfigFile(mng.getFile());
		m.setProperties(Converter.fill(mng.getProperties()));

		return m;
	}

	public static PackageMng fill(ExtensionPackage pkg) throws Exception {
		PackageMng result = new PackageMng();
		result.setId(pkg.getId().toString());
		result.setName(pkg.getName());
		result.setDescription(pkg.getDescription());

		return result;
	}

	public static void fillPackages(List<PackageMng> l, ExtensionPackage[] pkgs) throws Exception {
		if (pkgs != null) {
			for (int i = 0; i < pkgs.length; i++) {
				l.add(fill(pkgs[i]));
			}
		}
	}

	public static ActorMng fill(IActor actor) {
		ActorMng a = new ActorMng();
		a.setName(actor.getName());
		a.setDescription(actor.getDescription());
		// a.setOwner(fill(actor.getOwner()));
		// a.setPolicyClass(actor.getPolicyClass());
		a.setType(actor.getType());
		a.setOnline(true);
		a.setID(actor.getGuid().toString());
		a.setPolicyGuid(actor.getPolicy().getGuid().toString());

		return a;
	}

	public static void fillProxy(List<ProxyMng> l, IProxy[] proxies) {
		if ((proxies != null) && (proxies.length > 0)) {
			for (int i = 0; i < proxies.length; i++) {
				l.add(fill((Proxy) proxies[i]));
			}
		}
	}

	public static ProxyMng fill(IProxy proxy) {
		ProxyMng p = new ProxyMng();
		p.setName(proxy.getName());
		p.setGuid(proxy.getGuid().toString());
		
		if (proxy instanceof LocalProxy) {
			p.setProtocol(OrcaConstants.ProtocolLocal);
		} else if (proxy instanceof SoapAxis2Proxy) {
			p.setProtocol(OrcaConstants.ProtocolSoapAxis2);
			p.setUrl(((SoapAxis2Proxy) proxy).getServiceEndpoint());
		}

		return p;
	}

	public static ProxyMng[] fill(IProxy[] proxies) {
		ProxyMng[] result = null;

		if ((proxies != null) && (proxies.length > 0)) {
			result = new ProxyMng[proxies.length];

			for (int i = 0; i < proxies.length; i++) {
				result[i] = fill(proxies[i]);
			}
		}

		return result;
	}

	public static SliceMng fill(ISlice slice) {
		SliceMng mngSlice = new SliceMng();
		mngSlice.setName(slice.getName());
		mngSlice.setDescription(slice.getDescription());
		mngSlice.setOwner(Converter.fill(slice.getOwner()));
		mngSlice.setSliceID(slice.getSliceID().toString());

		if (slice.getResourceType() != null) {
			mngSlice.setResourceType(slice.getResourceType().toString());
		}

		mngSlice.setClientSlice(slice.isClient());

		return mngSlice;
	}

	public static void fillSlice(List<SliceMng> l, ISlice[] slices, boolean full) {
		if ((slices != null) && (slices.length > 0)) {
			for (int i = 0; i < slices.length; i++) {
				SliceMng s = fill(slices[i]);
				l.add(s);

				if (full) {
					Converter.attachProperties(s, slices[i]);
				}
			}
		}
	}

	public static UnitMng fillUnit(Properties p) {
		UnitMng u = new UnitMng();
		u.setProperties(Converter.fill(p));
		return u;
	}

	public static Unit fill(UnitMng mng) throws Exception {
		Unit u = new Unit();
		Properties p = Converter.fill(mng.getProperties());
		PersistenceUtils.restore(u, p);
		return u;
	}

	public static void fillUnits(List<UnitMng> l, Vector<Properties> v) {
		for (Properties p : v) {
			l.add(fillUnit(p));
		}
	}

	public static PluginMng fill(Plugin p) {
		PluginMng result = new PluginMng();
		result.setId(p.getId().toString());

		if (p.getPackageId() != null) {
			result.setPackageId(p.getPackageId().toString());
		}

		result.setType(p.getPluginType());
		result.setFactory(p.isFactory());
		result.setName(p.getName());
		result.setDescription(p.getDescription());
		result.setClassName(p.getClassName());
		result.setConfigurationProperties(Converter.fill(p.getConfigProperties()));
		result.setConfigurationTemplate(p.getConfigTemplate());
		result.setPortalLevel(p.getPortalLevel());
		result.setActorType(p.getActorType());

		return result;
	}

	/**
	 * Returns the specified plugin converted to <code>PluginMng</code>.
	 * 
	 * @param packageId package identifier
	 * @param pluginId plugin identifier
	 * @return
	 */
	public static void fillPlugin(List<PluginMng> l, Plugin[] ps) {
		if (ps != null) {
			for (int i = 0; i <ps.length; i++) {
				l.add(fill(ps[i]));
			}
		}
	}
	
	public static ReservationStateMng fillReservationState(Properties p) throws Exception {
		int state = PropList.getRequiredIntegerProperty(p, IReservation.PropertyState);
		int pending = PropList.getRequiredIntegerProperty(p, IReservation.PropertyPending);
		if (p.containsKey(IServiceManagerReservation.PropertyJoining)) {
			LeaseReservationStateMng s = new LeaseReservationStateMng();
			s.setJoining(PropList.getRequiredIntegerProperty(p, IServiceManagerReservation.PropertyJoining));
			s.setPending(pending);
			s.setState(state);
			return s;
		}else {
			ReservationStateMng s = new ReservationStateMng();
			s.setState(state);
			s.setPending(pending);
			return s;
		}		
	}
	
	public static ReservationMng fillReservation(Properties p, boolean attachProperties) throws Exception {
		IReservation r = ReservationFactory.createInstance(p);
		return fill(r, attachProperties);
	}

	public static ReservationMng fill(IReservation r, boolean attachProperties) throws Exception {
		ReservationMng mng;
		if (r instanceof IServiceManagerReservation){
			mng = new LeaseReservationMng();
		}else if (r instanceof IClientReservation){
			mng = new TicketReservationMng();
		}else {
			mng = new ReservationMng();
		}
		mng.setReservationID(r.getReservationID().toString());
		mng.setSliceID(r.getSliceID().toString());
		
		ResourceType rtype = r.getType();
		if (rtype != null) {
			mng.setResourceType(rtype.toString());
		}

		mng.setUnits(r.getUnits());
		mng.setState(r.getState());
		mng.setPendingState(r.getPendingState());

		if (r instanceof IServiceManagerReservation) {
			IServiceManagerReservation rc = (IServiceManagerReservation) r;
			((LeaseReservationMng)mng).setLeasedUnits(rc.getLeasedAbstractUnits());
			((LeaseReservationMng)mng).setJoinState(rc.getJoinState());
			IAuthorityProxy authority = rc.getAuthority();
			if (authority != null) {
				((LeaseReservationMng)mng).setAuthority(authority.getGuid().toString());
			}
		}
		
		if (r instanceof IClientReservation){
			IClientReservation rc = (IClientReservation)r;
			IBrokerProxy broker = rc.getBroker();
			if (broker != null) {
				((TicketReservationMng)mng).setBroker(broker.getGuid().toString());
			}
			((TicketReservationMng)mng).setRenewable(rc.isRenewable());
			((TicketReservationMng)mng).setRenewTime(rc.getRenewTime());
		}

		if (r.getTerm() != null) {
			mng.setStart(r.getTerm().getStartTime().getTime());
			mng.setEnd(r.getTerm().getEndTime().getTime());
		} else {
			if (r.getRequestedTerm() != null) {
				mng.setStart(r.getRequestedTerm().getStartTime().getTime());
				mng.setEnd(r.getRequestedTerm().getEndTime().getTime());
			}
		}
		
		if (r.getRequestedTerm() != null) {
			mng.setRequestedEnd(r.getRequestedTerm().getEndTime().getTime());
		}

		if (attachProperties) {
			attachProperties(mng, r);
		}

		mng.setNotices(r.getNotices());
		return mng;
	}
	
	/**
	 * Converts a UserMng object to a User object
	 * 
	 * @param mng
	 * @return
	 * @throws Exception
	 */
	public static User fill(UserMng mng) throws Exception {
		User user = new User();

		user.setLogin(mng.getLogin());
		user.setFirst(mng.getFirst());
		user.setLast(mng.getLast());

		ListMng list = (ListMng) mng.getActors();

		if (list != null) {
			user.setActors(list.getItem());
		}

		list = (ListMng) mng.getRoles();

		if (list != null) {
			user.setRoles(list.getItem());
		}

		return user;
	}

	public static ActorMng fillActor(Properties p) {
		ActorMng mng = new ActorMng();
		mng.setName(p.getProperty(Actor.PropertyName));
		
		// FIXME: [recovery] do we need these?
		//mng.setActorClass(p.getProperty(Actor.PropertyClassName));
		//mng.setPolicyClass(p.getProperty(Actor.PropertyMapperClass));
		
		mng.setDescription(p.getProperty(Actor.PropertyDescription));
		mng.setID(p.getProperty(Actor.PropertyGuid));

		IActor actor = ActorRegistry.getActor(mng.getName());
		mng.setOnline((actor != null));

		try {
			mng.setType(PropList.getIntegerProperty(p, Actor.PropertyType));
		} catch (Exception e) {
		}

		return mng;
	}

	public static ActorMng[] fillActor(Vector v) {
		ActorMng[] result = null;

		if ((v != null) && (v.size() > 0)) {
			result = new ActorMng[v.size()];

			for (int i = 0; i < v.size(); i++) {
				result[i] = fillActor((Properties) v.get(i));
			}
		}

		return result;
	}

	public static AuthTokenMng fillAuthToken(Properties p) {
		AuthTokenMng mng = new AuthTokenMng();
		mng.setName(p.getProperty(AuthToken.PropertyAuthTokenName));

		return mng;
	}

	public static ConfigMappingMng fillConfigMapping(Properties p) throws Exception {
		ConfigurationMapping map = ConfigurationMapping.newInstance(p);
		ConfigMappingMng result = new ConfigMappingMng();

		result.setKey(map.getKey());
		result.setFile(map.getConfigFile());
		result.setProperties(Converter.fill(map.getProperties()));

		return result;
	}

	public static ConfigMappingMng[] fillConfigMapping(Vector<Properties> v) throws Exception {
		ConfigMappingMng[] result = null;

		if ((v != null) && (v.size() > 0)) {
			result = new ConfigMappingMng[v.size()];

			for (int i = 0; i < result.length; i++) {
				Properties p = (Properties) v.get(i);
				result[i] = fillConfigMapping(p);
			}
		}

		return result;
	}

	/**
	 * Converts a <code>ExtensionPackage</code> that is serialized into a
	 * properties list into a <code>PackageMng</code> object.
	 * 
	 * @param p properties list
	 * @return
	 * @throws Exception
	 */
	public static PackageMng fillExtensionPackage(Properties p) throws Exception {
		PackageMng result = new PackageMng();

		result.setId(p.getProperty(ExtensionPackage.PropertyId));
		result.setName(p.getProperty(ExtensionPackage.PropertyName));
		result.setDescription(p.getProperty(ExtensionPackage.PropertyDescription));

		return result;
	}

	/**
	 * Converts a serialized plugin descriptor to a <code>PluginMng</code>
	 * object.
	 * 
	 * @param p properties list representing a serialized plugin descriptor.
	 * @return the <code>PluginMng</code> object
	 * @throws Exception
	 */
	public static PluginMng fillPluginMng(Properties p) throws Exception {
		PluginMng mng = new PluginMng();
		mng.setId(PropList.getProperty(p, Plugin.PropertyPluginId));
		mng.setPackageId(PropList.getProperty(p, Plugin.PropertyPackageId));
		mng.setType(PropList.getIntegerProperty(p, Plugin.PropertyPluginType));
		mng.setFactory(PropList.getBooleanProperty(p, Plugin.PropertyFactory));
		mng.setName(PropList.getProperty(p, Plugin.PropertyName));
		mng.setDescription(PropList.getProperty(p, Plugin.PropertyDescription));
		mng.setClassName(PropList.getProperty(p, Plugin.PropertyClassName));
		mng.setConfigurationProperties(Converter.fill(PropList
				.getPropertiesProperty(p, Plugin.PropertyConfigProperties)));
		mng.setConfigurationTemplate(PropList.getProperty(p, Plugin.PropertyConfigTemplate));
		mng.setPortalLevel(PropList.getIntegerProperty(p, Plugin.PropertyPortalLevel));
		mng.setActorType(PropList.getIntegerProperty(p, Plugin.PropertyActorType));

		return mng;
	}

	public static void fillReservation(List<ReservationMng> l, Vector v, boolean full, int state) throws Exception {
		if ((v != null) && (v.size() > 0)) {
			for (int i = 0; i < v.size(); i++) {
				Properties p = (Properties) v.get(i);
				ReservationMng r = Converter.fillReservation(p, true);
				if (r.getState() == state){
					l.add(r);
				}
			}
		}
	}

	
	public static void fillReservation(List<ReservationMng> l, Vector v, boolean full) throws Exception {
		if ((v != null) && (v.size() > 0)) {
			for (int i = 0; i < v.size(); i++) {
				Properties p = (Properties) v.get(i);
				ReservationMng r = Converter.fillReservation(p, true);
				l.add(r);
			}
		}
	}

	public static void fillReservationState(List<ReservationStateMng> l, Vector v) throws Exception {
		if ((v != null) && (v.size() > 0)) {
			for (int i = 0; i < v.size(); i++) {
				Properties p = (Properties) v.get(i);
				ReservationStateMng r = Converter.fillReservationState(p);
				l.add(r);
			}
		}
	}
	
	public static void fillSlice(List<SliceMng> l, Vector v, boolean full) throws Exception {
		if ((v != null) && (v.size() > 0)) {
			for (int i = 0; i < v.size(); i++) {
				Properties p = (Properties) v.get(i);
				ISlice s = SliceFactory.createInstance(p);
				SliceMng sm = fill(s);
				l.add(sm);

				if (full) {
					Converter.attachProperties(sm, s);
				}
			}
		}
	}

	/**
	 * Converts a serialized User object to a UserMng object
	 * 
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public static UserMng fillUserMng(Properties p) throws Exception {
		UserMng mng = new UserMng();
		mng.setLogin(p.getProperty(User.PropertyLogin));
		mng.setFirst(p.getProperty(User.PropertyFirst));
		mng.setLast(p.getProperty(User.PropertyLast));

		String[] roles = PropList.getStringArrayProperty(p, User.PropertyRoles);

		if ((roles != null) && (roles.length > 0)) {
			ListMng list = new ListMng();
			for (String role : roles) {
				list.getItem().add(role);
			}
			mng.setRoles(list);
		}

		String[] actors = PropList.getStringArrayProperty(p, User.PropertyActors);

		if ((actors != null) && (actors.length > 0)) {
			ListMng list = new ListMng();
			for (String actor : actors) {
				list.getItem().add(actor);
			}
			mng.setActors(list);
		}

		return mng;
	}

	/**
	 * Converts a vector of serialized User objects to UserMng objects
	 * 
	 * @param v
	 * @return
	 * @throws Exception
	 */
	public static void fillUser(List<UserMng> l, Vector<Properties> v) throws Exception {
		if ((v != null) && (v.size() > 0)) {
			for (int i = 0; i < v.size(); i++) {
				l.add(fillUserMng(v.get(i)));
			}
		}
	}

	public static String getActorClass(ActorMng actor) {
		switch (actor.getType()) {
		case OrcaConstants.ActorTypeServiceManager:
			return "slots.SlottedSM";

		case OrcaConstants.ActorTypeBroker:
			return "slots.SlottedAgent";

		case OrcaConstants.ActorTypeSiteAuthority:
			return "slots.SlottedAuthority";

		default:
			return null;
		}
	}

	public static IBrokerProxy getAgentProxy(ProxyMng mng) {
		try {
			ActorLocation loc = new ActorLocation(mng.getUrl());
			ActorIdentity identity = new ActorIdentity(mng.getName(), new ID(mng.getGuid()));
			return (IBrokerProxy)OrcaContainer.getProxy(mng.getProtocol(), identity, loc, mng.getType()); 
		} catch (Exception e) {
			return null;
		}
	}

	public static String getMapperClass(ActorMng actor) {
		switch (actor.getType()) {
		case OrcaConstants.ActorTypeServiceManager:
			return "slots.SlottedSMMapper";

		case OrcaConstants.ActorTypeBroker:
			return "slots.SlottedAgentMapper";

		case OrcaConstants.ActorTypeSiteAuthority:
			return "slots.SlottedAuthorityMapper";

		default:
			return null;
		}
	}

	public static String getPluginClass(ActorMng actor) {
		switch (actor.getType()) {
		case OrcaConstants.ActorTypeServiceManager:
			return "cod.plugins.ServiceManagerCodPlugin";

		case OrcaConstants.ActorTypeBroker:
			return "sharp.plugins.ServerPlugin";

		case OrcaConstants.ActorTypeSiteAuthority:
			return "cod.plugins.Site";

		default:
			return null;
		}
	}

	public static String getPolicyClass(ActorMng actor) {
		switch (actor.getType()) {
		case OrcaConstants.ActorTypeServiceManager:

			if (actor.getPolicyClass() != null) {
				return actor.getPolicyClass();
			}

			return "policy.SMSimplePolicyPlugin";

		case OrcaConstants.ActorTypeBroker:
			return "policy.AgentSimplePolicyPlugin";

		case OrcaConstants.ActorTypeSiteAuthority:
			return "policy.AuthorityBasePolicyPlugin";

		default:
			return null;
		}
	}

	public static ResourceData getResourceData(ReservationMng mng) {
		ResourceData rd = new ResourceData();

		Properties p = Converter.fill(mng.getRequestProperties());

		if (p == null) {
			p = new Properties();
		}

		ResourceData.mergeProperties(p, rd.getRequestProperties());

		p = Converter.fill(mng.getResourceProperties());

		if (p == null) {
			p = new Properties();
		}

		ResourceData.mergeProperties(p, rd.getResourceProperties());

		p = Converter.fill(mng.getLocalProperties());

		if (p == null) {
			p = new Properties();
		}

		ResourceData.mergeProperties(p, rd.getLocalProperties());

		p = Converter.fill(mng.getConfigurationProperties());

		if (p == null) {
			p = new Properties();
		}

		ResourceData.mergeProperties(p, rd.getConfigurationProperties());

		return rd;
	}

	public static ResourceData getResourceData(SliceMng mng) {
		ResourceData rd = new ResourceData();

		Properties p = Converter.fill(mng.getRequestProperties());

		if (p == null) {
			p = new Properties();
		}

		ResourceData.mergeProperties(p, rd.getRequestProperties());

		p = Converter.fill(mng.getResourceProperties());

		if (p == null) {
			p = new Properties();
		}

		ResourceData.mergeProperties(p, rd.getResourceProperties());

		p = Converter.fill(mng.getLocalProperties());

		if (p == null) {
			p = new Properties();
		}

		ResourceData.mergeProperties(p, rd.getLocalProperties());

		p = Converter.fill(mng.getConfigurationProperties());

		if (p == null) {
			p = new Properties();
		}

		ResourceData.mergeProperties(p, rd.getConfigurationProperties());

		return rd;
	}

	public static ResourceSet getResourceSet(ReservationMng mng) {
		ResourceData rd = getResourceData(mng);

		return new ResourceSet(mng.getUnits(), new ResourceType(mng.getResourceType()), rd);
	}

	public static Term getTerm(String start, String end) throws Exception {
		Date s = dateformat.parse(start);
		Date e = dateformat.parse(end);

		Term t = new Term(s, e);

		return t;
	}

	/**
	 * Checks if a term has expired
	 * 
	 * @param term
	 * @return
	 */
	public static boolean isExpired(Term term) {
		boolean result = false;
		IActorContainer man = Globals.getContainer();
		long now = man.getCurrentCycle();

		if (man.getActorClock().cycle(term.getEndTime()) < now) {
			result = true;
		}

		return result;
	}

	public static Client fill(ClientMng client) {
		Client c = new Client();
		c.setName(client.getName());
		c.setGuid(new ID(client.getGuid()));
		return c;
	}

	public static ClientMng fillClientMng(Properties p) throws Exception {
		Client cc = new Client();
		PersistenceUtils.restore(cc, p);

		ClientMng c = new ClientMng();
		c.setName(cc.getName());
		c.setGuid(cc.getGuid().toString());
		return c;
	}

	public static void fillClient(List<ClientMng> l, Vector<Properties> v) throws Exception {
		if ((v != null) && (v.size() > 0)) {
			for (int i = 0; i < v.size(); i++) {
				l.add(fillClientMng(v.get(i)));
			}
		}
	}

	public static CertificateMng fill(Certificate c) throws Exception {
		if (c == null) {
			throw new IllegalArgumentException("c cannot be null");
		}
		CertificateMng cmng = new CertificateMng();
		cmng.setContents(c.getEncoded());
		return cmng;
	}

	public static List<ProtocolProxyMng> fill(OrcaProxyProtocolDescriptor[] proxies) {
		List<ProtocolProxyMng> result = new ArrayList<ProtocolProxyMng>();

		if (proxies != null) {
			for (int i = 0; i < proxies.length; i++) {
				result.add(fill(proxies[i]));
			}
		}

		return result;
	}

	public static ProtocolProxyMng fill(OrcaProxyProtocolDescriptor proxies) {
		ProtocolProxyMng result = null;

		if (proxies != null) {
			result = new ProtocolProxyMng();
			result.setProtocol(proxies.getProtocol());
			result.setProxyClass(proxies.getProxyClass());
		}

		return result;
	}

	
	public static ReservationStateMng convert(ReservationState state){
		ReservationStateMng mng;
		if (state.getJoining() != -1){
			mng = new LeaseReservationStateMng();
			((LeaseReservationStateMng)mng).setJoining(state.getJoining());
		}else {
			mng = new ReservationStateMng();
		}
		mng.setState(state.getState());
		mng.setPending(state.getPending());
		return mng;
	}
	
	public static EventMng convert(ReservationStateTransitionEvent e){
		ReservationStateTransitionEventMng mng = new ReservationStateTransitionEventMng();
		mng.setActorId(e.getActorID().toString());
		mng.setReservationId(e.getReservationID().toString());
		mng.setSliceId(e.getSliceID().toString());
		mng.setState(convert(e.getState()));
		return mng;
	}
	
	public static void convert(List<IEvent> from, List<EventMng> to){
		for (IEvent e : from){
			EventMng mng = null;
			if (e instanceof ReservationStateTransitionEvent){
				mng = convert((ReservationStateTransitionEvent)e); 
			}else {
				mng = new GenericEventMng();
				if (e.getActorID() != null){
					mng.setActorId(e.getActorID().toString());
				}
				Properties p = new Properties();
				p.setProperty(OrcaConstants.EventClass, e.getClass().getName());
				mng.setProperties(fill(p));
			}
			to.add(mng);
		}
	}
}
