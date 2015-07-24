package orca.handlers.nlr;
/* License
 * Copyright (c) 2009 Renaissance Computing Institute and Duke University
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and/or hardware specification (the “Work”) to deal in the Work without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to
 * the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Work.
 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS
 * IN THE WORK.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import orca.handlers.nlr.SherpaAPIResponse.EntityDefinition;
import orca.handlers.nlr.SherpaAPIResponse.GetPathDefinition;
import orca.handlers.nlr.SherpaAPIResponse.InterfaceDefinition;
import orca.handlers.nlr.SherpaAPIResponse.SPDefinition;
import orca.handlers.nlr.SherpaAPIResponse.TrunkDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanAddReservationDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanGetReservationDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanIdAvailableDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanIdDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanProvisionDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanRemoveDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanRemoveReservationDefinition;
import orca.handlers.nlr.SherpaAPIResponse.VlanStatusDefinition;
import orca.handlers.nlr.SherpaAPIResponse.WGDefinition;

import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * This file contains API definitions. Each API call 
 * (see http://noc.nlr.net/nlr/maps_documentation/nlr-framenet-documentation.html for details)
 * has a corresponding XXXResponse object that itself corresponds to the structure of 
 * JSON snippet returned by the Sherpa API.
 * 
 * To find out help on a particular method, invoke the appropriate cgi (planning or provisioning)
 * and do "?method=help;method_name=$a_name_returned_by_help"  and it will provide more details 
 * on what parameters are expected and what pattern the input must adhere to.
 */

public class SherpaAPI {
	private static final String CGI_RETURNED_ERROR = "CGI indicates error in JSON output: ";
	private SherpaSession sherpaSession;
	private Gson gson;
	
	private int net = NLR_NETNUM;
	private int wg = 0;
	
	// network number used by NLR
	public static final int NLR_NETNUM = 1;
	
	private Logger logger;
	
	private void ctor_initialize() {
		gson = new Gson();
	}
	
	/**
	 * pass in the network number and the workgroup number to use.
	 * For NLR network number is always 1.
	 * @param pNet
	 * @param pWg
	 */
	public SherpaAPI(int pNet, int pWg, Logger log) {
		net = pNet;
		wg = pWg;
		logger = log;
		sherpaSession = new SherpaSession(); // will pass some parameters later
		ctor_initialize();
	}
	
	/**
	 * Pass in only the workgroup, assumes you will be working on NLR (uses net=1)
	 * @param pWg
	 */
	public SherpaAPI(int pWg, Logger log) {
		wg = pWg;
		logger = log;
		sherpaSession = new SherpaSession(); // will pass some parameters later
		ctor_initialize();
	}
	
	/**
	 * A ctor that accepts an external session to rely on to store the login state. Works
	 * only on NLR (net=1)
	 * @param pWg
	 * @param ss
	 */
	public SherpaAPI(SherpaSession ss, int pWg, Logger log) {
		wg = pWg;
		logger = log;
		sherpaSession = ss;
		ctor_initialize();
	}
	
	/**
	 * A ctor that accepts an external session to rely on to store the login state. Allows
	 * to pass in a admin network number other than 1 (NLR)
	 * @param ss
	 * @param pNet
	 * @param pWg
	 */
	public SherpaAPI(SherpaSession ss, int pNet, int pWg, Logger log) {
		net = pNet;
		wg = pWg;
		sherpaSession = ss;
		logger = log;
		ctor_initialize();
	}
	
	/*
	 * PLANNING API
	 */
	
	/**
	 * Returns a vlan id that is available for use.
	 * @param net
	 * @param wg
	 * @return vlan id
	 */
	public VlanIdDefinition get_available_vlan_id() throws Exception {
		String cmd = "?method=get_available_vlan_id";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = sherpaSession.executePlanningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<VlanIdDefinition>>() {}.getType();
		SherpaAPIResponse<VlanIdDefinition> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * Retrieve all provisioned vlans
	 * 
	 * @param net network number (usually 1)
	 * @param wg workgroup number (find out using get_workgroups)
	 * @return list of vlan definitions (see SherpaAPIResponse.VlanDefinition)
	 */
	public List<VlanDefinition> get_all_vlans() throws Exception {
		
		String cmd = "?method=get_vlans";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = sherpaSession.executePlanningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<VlanDefinition>>>() {}.getType();
		SherpaAPIResponse<List<VlanDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * Retrieve provisioned VLANs that match a regex pattern. ckt_id, description and
	 * vlan_id fields are searched.
	 * 
	 * @param pattern - strings are keywords for description and ckt_id. Numeric characters are matched to vlan_id.
	 * @return
	 * @throws Exception
	 */
	public List<VlanDefinition> get_matching_vlans(String pattern) throws Exception {
		
		String cmd = "?method=get_vlans";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&search=" + URLEncoder.encode(pattern, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = sherpaSession.executePlanningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<VlanDefinition>>>() {}.getType();
		SherpaAPIResponse<List<VlanDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * get a list of workgroups you belong to
	 * 
	 * @param net network number (1 for NLR)
	 * @return list of workgroup definitions
	 */
	public List<WGDefinition> get_workgroups() throws Exception {
		String cmd = "?method=get_workgroups";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = sherpaSession.executePlanningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<WGDefinition>>>() {}.getType();
		SherpaAPIResponse<List<WGDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * is a particular vlan tag available?
	 * 
	 * @param net network number (1 for NLR)
	 * @param wg workgroup number (get it from get_workgroups)
	 * @param vlan_id
	 * @return 
	 */
	public boolean is_vlan_id_available(int vlan_id) throws Exception {
		String cmd = "?method=is_vlan_id_available";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&vlan_id=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = sherpaSession.executePlanningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<VlanIdAvailableDefinition>>() {}.getType();
		SherpaAPIResponse<VlanIdAvailableDefinition> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return (resp.results.available != 0);	
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * get the list of trunks available for use by the wg
	 * 
	 * @param net typically 1 for NLR
	 * @param wg get it from get_workgroups
	 * @return list of trunk definitions
	 */
	public List<TrunkDefinition> get_trunks() throws Exception {
		String cmd = "?method=get_trunks";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = sherpaSession.executePlanningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<TrunkDefinition>>>() {}.getType();
		SherpaAPIResponse<List<TrunkDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * get the list of administrative entities that can be assigned to vlans by users in this group
	 * 
	 * @param net
	 * @param wg
	 * @return
	 */
	public List<EntityDefinition> get_entities() throws Exception {
		String cmd = "?method=get_entities";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = sherpaSession.executePlanningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<EntityDefinition>>>() {}.getType();
		SherpaAPIResponse<List<EntityDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * get a list of interfaces on a particular node
	 * 
	 * @param net
	 * @param wg
	 * @param node_name
	 * @return
	 */
	public List<InterfaceDefinition> get_interfaces(String node_name) throws Exception {
		String cmd = "?method=get_interfaces";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&node_name=" + URLEncoder.encode(node_name, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = sherpaSession.executePlanningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<InterfaceDefinition>>>() {}.getType();
		SherpaAPIResponse<List<InterfaceDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * get a list of trunks to traverse between two nodes. Used to generate the input to the
	 * provision_vlan call
	 * 
	 * @param net
	 * @param wg
	 * @param node_a
	 * @param node_z
	 * @param reserved_bandwidth
	 * @return
	 */
	public List<SPDefinition> get_shortest_path(String node_a, String node_z, long reserved_bandwidth) throws Exception {
		String cmd = "?method=get_shortest_path";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//			cmd+="&node_a=" + URLEncoder.encode(node_a, HTTP.UTF_8);
//			cmd+="&node_z=" + URLEncoder.encode(node_z, HTTP.UTF_8);
			cmd+="&nodes=" + URLEncoder.encode(node_a + " " + node_z, HTTP.UTF_8);
			cmd+="&reserved_bandwidth=" + URLEncoder.encode(String.format("%d", reserved_bandwidth), HTTP.UTF_8); 
		} catch (UnsupportedEncodingException e) {}

		Reader ret = sherpaSession.executePlanningCmd(cmd);
		Type myType = new TypeToken<SherpaAPIResponse<List<SPDefinition>>>() {}.getType();
		SherpaAPIResponse<List<SPDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * Get a path of a given VLAN (identified by circuit id)
	 * 
	 * @param ckt_id
	 * @return
	 * @throws Exception
	 */
	public List<GetPathDefinition> get_vlan_path(int ckt_id) throws Exception {
		String cmd = "?method=get_vlan_path";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&ckt_id=" + URLEncoder.encode(String.format("%d", ckt_id), HTTP.UTF_8); 
		} catch (UnsupportedEncodingException e) {}

		Reader ret = sherpaSession.executePlanningCmd(cmd);
		Type myType = new TypeToken<SherpaAPIResponse<List<GetPathDefinition>>>() {}.getType();
		SherpaAPIResponse<List<GetPathDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/*
	 * PROVISIONING API
	 */
	
	/**
	 * get a list of reserved VLANs
	 * 
	 * @param net network identified (1 for NLR)
	 * @param wg workgroup identifier (get it from get_workgroups())
	 * @return list of vlan reservation definitions
	 */
	public List<VlanGetReservationDefinition> get_all_reservations() throws Exception {
		String cmd = "?method=get_reservations";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = sherpaSession.executeProvisioningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<VlanGetReservationDefinition>>>() {}.getType();
		SherpaAPIResponse<List<VlanGetReservationDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}

	/**
	 * Get reservations matching a pattern. ckt_id, description and vlan_id fields are checked against
	 * the pattern
	 * @param pattern
	 * @return
	 * @throws Exception
	 */
	public List<VlanGetReservationDefinition> get_matching_reservations(String pattern) throws Exception {
		String cmd = "?method=get_reservations";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&search=" + URLEncoder.encode(pattern, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = sherpaSession.executeProvisioningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<VlanGetReservationDefinition>>>() {}.getType();
		SherpaAPIResponse<List<VlanGetReservationDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * Create a reservation for a vlan tag
	 * 
	 * @param net
	 * @param wg
	 * @param vlan_id
	 * @param description
	 * @return success?
	 */
	public boolean add_reservation(int vlan_id, String description) throws Exception {
		String cmd = "?method=add_reservation";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&vlan_id=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
			cmd+="&description=" + URLEncoder.encode(description, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = sherpaSession.executeProvisioningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<VlanAddReservationDefinition>>>() {}.getType();
		SherpaAPIResponse<List<VlanAddReservationDefinition>> resp = gson.fromJson(ret, myType);
	
		if (resp != null) {
			if ((resp.error == 0) || (resp.success == 1)) 
				return true;
			else
				throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
		} else
			throw new Exception(CGI_RETURNED_ERROR + " (no error output)" + " for command " + cmd);
	}
	
	/**
	 * remove a reservation for a vlan tag.
	 * @param net
	 * @param wg
	 * @param vlan_id
	 * @return true if success
	 */
	public boolean remove_reservation(int vlan_id) throws Exception {
		String cmd = "?method=remove_reservation";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&vlan_id=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = sherpaSession.executeProvisioningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<VlanRemoveReservationDefinition>>() {}.getType();
		SherpaAPIResponse<VlanRemoveReservationDefinition> resp = gson.fromJson(ret, myType);
	
		if (resp != null) {
			if ((resp.error == 0) || (resp.success == 1))
				return true;
			else
				throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
		} else
			throw new Exception(CGI_RETURNED_ERROR + " (no error output)" + " for command " + cmd);
		
	}
	
	/**
	 * Provision a point-to-point VLAN. Lot's of parameters needed. Accepts output of the get_shortest_path call as path.
	 * 
	 * @param node_a
	 * @param int_a
	 * @param node_z
	 * @param int_z
	 * @param root_bridge
	 * @param path
	 * @param vlan_id
	 * @param reserved_bandwidth
	 * @param description
	 * @param request_id
	 * @param entity_id
	 * @return true if successful
	 * @throws Exception
	 */
	public boolean provision_vlan(String node_a, String int_a, String node_z, String int_z, String root_bridge, 
			List<SPDefinition> path, int vlan_id, long reserved_bandwidth, 
			String description, int request_id, int entity_id) throws Exception { 
		String cmd = "?method=provision_vlan";
		// create comma-separated path
		Iterator<SPDefinition> it = path.iterator();
		String csPath = "";
		if (it.hasNext())
			csPath = it.next().circuit_id;
		
		while(it.hasNext())
			csPath += " " + it.next().circuit_id;
		
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&vlan_id=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
			cmd+="&root_bridge=" + URLEncoder.encode(root_bridge, HTTP.UTF_8);
//			cmd+="&node_z=" + URLEncoder.encode(node_z, HTTP.UTF_8);
//			cmd+="&int_z=" + URLEncoder.encode(int_z, HTTP.UTF_8);
//			cmd+="&int_a=" + URLEncoder.encode(int_a, HTTP.UTF_8);
//			cmd+="&node_a=" + URLEncoder.encode(node_a, HTTP.UTF_8);
			// new multi-point API requires a different way of specifying endpoints (/ib 02/28/2010)
			cmd+="&endpoint_nodes=" + URLEncoder.encode(node_a + " " + node_z, HTTP.UTF_8);
			cmd+="&endpoint_interfaces=" + URLEncoder.encode(int_a + " " + int_z, HTTP.UTF_8);
			cmd+="&path=" + URLEncoder.encode(csPath, HTTP.UTF_8);
			cmd+="&reserved_bandwidth=" + URLEncoder.encode(String.format("%d", reserved_bandwidth), HTTP.UTF_8);
			cmd+="&description=" + URLEncoder.encode(description, HTTP.UTF_8);
			cmd+="&entity_id=" + URLEncoder.encode(String.format("%d", entity_id), HTTP.UTF_8);
			cmd+="&request_id=" + URLEncoder.encode(String.format("%d", request_id), HTTP.UTF_8);
			cmd+="&endpoint_modes=" + URLEncoder.encode("trunk trunk", HTTP.UTF_8); // for now just trunk
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = sherpaSession.executeProvisioningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<VlanProvisionDefinition>>() {}.getType();
		SherpaAPIResponse<VlanProvisionDefinition> resp = gson.fromJson(ret, myType);
	
		if (resp != null) {
			if ((resp.error == 0) || (resp.success == 1)) {
				return true;
			}
			else {
				throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
			}
		} else {
			throw new Exception(CGI_RETURNED_ERROR + " (no error output) for command " + cmd);
		}
		
	}
	
	/**
	 * Simplified version of the vlan provisioning - always sets root bridge to the source node (node_a).
	 * 
	 * @param node_a
	 * @param int_a
	 * @param node_z
	 * @param int_z
	 * @param path
	 * @param vlan_id
	 * @param reserved_bandwidth
	 * @param description
	 * @param request_id
	 * @param entity_id
	 * @return
	 * @throws Exception
	 */
	public boolean provision_vlan(String node_a, String int_a, String node_z, String int_z, 
			List<SPDefinition> path, int vlan_id, long reserved_bandwidth, 
			String description, int request_id, int entity_id) throws Exception { 
		return provision_vlan(node_a, int_a, node_z, int_z, node_a, path, vlan_id, reserved_bandwidth,
				description, request_id, entity_id);
	}
	
	/**
	 * Remove (tear-down) a previously provisioned vlan
	 * 
	 * @param vlan_id
	 * @param request_id
	 * @return
	 * @throws Exception
	 */
	public boolean remove_vlan(int vlan_id, int request_id) throws Exception { 
		String cmd = "?method=remove_vlan";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&vlan_id=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
			cmd+="&request_id=" + URLEncoder.encode(String.format("%d", request_id), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = sherpaSession.executeProvisioningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<VlanRemoveDefinition>>() {}.getType();
		SherpaAPIResponse<VlanRemoveDefinition> resp = gson.fromJson(ret, myType);
	
		if (resp != null) {
			if ((resp.error == 0) || (resp.success == 1))
				return true;
			else
				throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
		} else
			throw new Exception(CGI_RETURNED_ERROR + " (no error output)" + " for command " + cmd);
		
	}
	
	/**
	 * check the status of a provisioning request
	 * 
	 * @param vlan_id
	 * @param request_id
	 * @return
	 * @throws Exception
	 */
	public List<VlanStatusDefinition> get_status(int vlan_id, int request_id) throws Exception { 
		String cmd = "?method=get_status";
		try {
			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&vlan_id=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
			cmd+="&request_id=" + URLEncoder.encode(String.format("%d", request_id), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = sherpaSession.executeProvisioningCmd(cmd);
		
		Type myType = new TypeToken<SherpaAPIResponse<List<VlanStatusDefinition>>>() {}.getType();
		SherpaAPIResponse<List<VlanStatusDefinition>> resp = gson.fromJson(ret, myType);
	
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
}
