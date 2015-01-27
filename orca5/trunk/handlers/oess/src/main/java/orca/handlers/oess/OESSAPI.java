package orca.handlers.oess;
/*
* Copyright (c) 2013 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/

import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import orca.handlers.oess.OESSAPIResponse.ExistingCircuitReservation;
import orca.handlers.oess.OESSAPIResponse.GetCircuitDefinition;
import orca.handlers.oess.OESSAPIResponse.InterfaceDefinition;
import orca.handlers.oess.OESSAPIResponse.SPDefinition;
import orca.handlers.oess.OESSAPIResponse.VlanIdAvailableDefinition;
import orca.handlers.oess.OESSAPIResponse.CircuitProvisionDefinition;
import orca.handlers.oess.OESSAPIResponse.CircuitRemoveDefinition;
import orca.handlers.oess.OESSAPIResponse.WGDefinition;

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
 * and do "?action=help;method_name=$a_name_returned_by_help"  and it will provide more details 
 * on what parameters are expected and what pattern the input must adhere to.
 */

public class OESSAPI {
	private static final String CGI_RETURNED_ERROR = "CGI indicates error in JSON output: ";
	private OESSSession oessSession;
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
	public OESSAPI(int pNet, int pWg, Logger log) {
		net = pNet;
		wg = pWg;
		logger = log;
		oessSession = new OESSSession(); // will pass some parameters later
		ctor_initialize();
	}
	
	/**
	 * Pass in only the workgroup, assumes you will be working on NLR (uses net=1)
	 * @param pWg
	 */
	public OESSAPI(int pWg, Logger log) {
		wg = pWg;
		logger = log;
		oessSession = new OESSSession(); // will pass some parameters later
		ctor_initialize();
	}
	
	/**
	 * A ctor that accepts an external session to rely on to store the login state. Works
	 * only on NLR (net=1)
	 * @param pWg
	 * @param ss
	 */
	public OESSAPI(OESSSession ss, int pWg, Logger log) {
		wg = pWg;
		logger = log;
		oessSession = ss;
		ctor_initialize();
	}
	
	/**
	 * A ctor that accepts an external session to rely on to store the login state. Allows
	 * to pass in a admin network number other than 1 (NLR)
	 * @param ss
	 * @param pNet
	 * @param pWg
	 */
	public OESSAPI(OESSSession ss, int pNet, int pWg, Logger log) {
		net = pNet;
		wg = pWg;
		oessSession = ss;
		logger = log;
		ctor_initialize();
	}
	
	/*
	 * Data API
	 */
	
	/**
	 * Returns a vlan id that is available for use.
	 * @param net
	 * @param wg
	 * @return vlan id
	 */
//	public VlanIdDefinition get_available_vlan_id() throws Exception {
//		String cmd = "?action=get_available_vlan_id";
//		try {
//			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
//			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {}
//		
//		Reader ret = oessSession.executeDataCmd(cmd);
//		
//		Type myType = new TypeToken<OESSAPIResponse<VlanIdDefinition>>() {}.getType();
//		OESSAPIResponse<VlanIdDefinition> resp = gson.fromJson(ret, myType);
//		
//		if (resp == null)
//			throw new Exception("Unable to parse JSON output for command " + cmd);
//		
//		if ((resp.error == 0) || (resp.success == 1)) 
//			return resp.results;
//		else
//			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
//	}
	
	/**
	 * Retrieve all provisioned vlans
	 * 
	 * @param net network number (usually 1)
	 * @param wg workgroup number (find out using get_workgroups)
	 * @return list of vlan definitions (see SherpaAPIResponse.VlanDefinition)
	 */
//	public List<VlanDefinition> get_all_vlans() throws Exception {
//		
//		String cmd = "?action=get_vlans";
//		try {
//			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
//			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {}
//
//		Reader ret = oessSession.executeDataCmd(cmd);
//		
//		Type myType = new TypeToken<OESSAPIResponse<List<VlanDefinition>>>() {}.getType();
//		OESSAPIResponse<List<VlanDefinition>> resp = gson.fromJson(ret, myType);
//		
//		if (resp == null)
//			throw new Exception("Unable to parse JSON output for command " + cmd);
//		
//		if ((resp.error == 0) || (resp.success == 1)) 
//			return resp.results;
//		else
//			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
//	}
	
	/**
	 * Retrieve provisioned VLANs that match a regex pattern. ckt_id, description and
	 * vlan_id fields are searched.
	 * 
	 * @param pattern - strings are keywords for description and ckt_id. Numeric characters are matched to vlan_id.
	 * @return
	 * @throws Exception
	 */
//	public List<VlanDefinition> get_matching_vlans(String pattern) throws Exception {
//		
//		String cmd = "?action=get_vlans";
//		try {
//			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
//			cmd+="&wg=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//			cmd+="&search=" + URLEncoder.encode(pattern, HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {}
//
//		Reader ret = oessSession.executeDataCmd(cmd);
//		
//		Type myType = new TypeToken<OESSAPIResponse<List<VlanDefinition>>>() {}.getType();
//		OESSAPIResponse<List<VlanDefinition>> resp = gson.fromJson(ret, myType);
//		
//		if (resp == null)
//			throw new Exception("Unable to parse JSON output for command " + cmd);
//		
//		if ((resp.error == 0) || (resp.success == 1)) 
//			return resp.results;
//		else
//			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
//	}
	
	/**
	 * get a list of workgroups you belong to (oess)
	 * 
	 * @return list of workgroup definitions
	 */
	public List<WGDefinition> get_workgroups() throws Exception {
		String cmd = "?action=get_workgroups";

		Reader ret = oessSession.executeDataCmd(cmd);
		
		Type myType = new TypeToken<OESSAPIResponse<List<WGDefinition>>>() {}.getType();
		OESSAPIResponse<List<WGDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * is a particular vlan tag available? (oess)
	 * 
	 * @param vlan_id
	 * @param node name
	 * @param interface name
	 * @return boolean
	 */
	public boolean is_vlan_id_available(int vlan_id, String node, String intface) throws Exception {
		String cmd = "?action=is_vlan_tag_available";
		try {
			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&vlan_tag=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
			cmd+="&node=" + URLEncoder.encode(node, HTTP.UTF_8);
			cmd+="&interface=" + URLEncoder.encode(intface, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = oessSession.executeDataCmd(cmd);
		
		Type myType = new TypeToken<OESSAPIResponse<VlanIdAvailableDefinition>>() {}.getType();
		OESSAPIResponse<VlanIdAvailableDefinition> resp = gson.fromJson(ret, myType);
		
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
//	public List<TrunkDefinition> get_trunks() throws Exception {
//		String cmd = "?action=get_trunks";
//		try {
//			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
//			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {}
//
//		Reader ret = oessSession.executeDataCmd(cmd);
//		
//		Type myType = new TypeToken<OESSAPIResponse<List<TrunkDefinition>>>() {}.getType();
//		OESSAPIResponse<List<TrunkDefinition>> resp = gson.fromJson(ret, myType);
//		
//		if (resp == null)
//			throw new Exception("Unable to parse JSON output for command " + cmd);
//		
//		if ((resp.error == 0) || (resp.success == 1)) 
//			return resp.results;
//		else
//			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
//	}
	
	/**
	 * get the list of administrative entities that can be assigned to vlans by users in this group
	 * 
	 * @param net
	 * @param wg
	 * @return
	 */
//	public List<EntityDefinition> get_entities() throws Exception {
//		String cmd = "?action=get_entities";
//		try {
//			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
//			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {}
//
//		Reader ret = sherpaSession.executeDataCmd(cmd);
//		
//		Type myType = new TypeToken<OESSAPIResponse<List<EntityDefinition>>>() {}.getType();
//		OESSAPIResponse<List<EntityDefinition>> resp = gson.fromJson(ret, myType);
//		
//		if (resp == null)
//			throw new Exception("Unable to parse JSON output for command " + cmd);
//		
//		if ((resp.error == 0) || (resp.success == 1)) 
//			return resp.results;
//		else
//			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
//	}
	
	/**
	 * get a list of interfaces on a particular node (OESS)
	 * 
	 * @param net
	 * @param wg
	 * @param node_name
	 * @return
	 */
	public List<InterfaceDefinition> get_interfaces(String node_name) throws Exception {
		String cmd = "?action=get_node_interfaces";
		try {
			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&node=" + URLEncoder.encode(node_name, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = oessSession.executeDataCmd(cmd);
		
		Type myType = new TypeToken<OESSAPIResponse<List<InterfaceDefinition>>>() {}.getType();
		OESSAPIResponse<List<InterfaceDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * get a list of trunks to traverse between two nodes. Used to generate the input to the
	 * provision_vlan call (oess)
	 * 
	 * @param net
	 * @param wg
	 * @param node_a
	 * @param node_z
	 * @param reserved_bandwidth
	 * @return
	 */
	public List<SPDefinition> get_shortest_path(String node_a, String node_z, long reserved_bandwidth) throws Exception {
		String cmd = "?action=get_shortest_path";
		try {
			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&node=" + URLEncoder.encode(node_a, HTTP.UTF_8);
			cmd+="&node=" + URLEncoder.encode(node_z, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}

		Reader ret = oessSession.executeDataCmd(cmd);
		Type myType = new TypeToken<OESSAPIResponse<List<SPDefinition>>>() {}.getType();
		OESSAPIResponse<List<SPDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}
	
	/**
	 * Get details of a given circuit (oess)
	 * 
	 * @param ckt_id
	 * @return
	 * @throws Exception
	 */
	public List<GetCircuitDefinition> get_circuit_details(int ckt_id) throws Exception {
		String cmd = "?action=get_circuit_details";
		try {
			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&circuit_id=" + URLEncoder.encode(String.format("%d", ckt_id), HTTP.UTF_8); 
		} catch (UnsupportedEncodingException e) {}

		Reader ret = oessSession.executeDataCmd(cmd);
		Type myType = new TypeToken<OESSAPIResponse<List<GetCircuitDefinition>>>() {}.getType();
		OESSAPIResponse<List<GetCircuitDefinition>> resp = gson.fromJson(ret, myType);
		
		if (resp == null)
			throw new Exception("Unable to parse JSON output for command " + cmd);
		
		if ((resp.error == 0) || (resp.success == 1)) 
			return resp.results;
		else
			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
	}

	/**
	 * get a list of reserved circuits
	 * 
	 * @return list of circuit reservation definitions
	 */
	public List<ExistingCircuitReservation> get_existing_circuits() throws Exception {
		String cmd = "?action=get_existing_circuits";
		try {
			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = oessSession.executeDataCmd(cmd);
		
		Type myType = new TypeToken<OESSAPIResponse<List<ExistingCircuitReservation>>>() {}.getType();
		OESSAPIResponse<List<ExistingCircuitReservation>> resp = gson.fromJson(ret, myType);
		
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
	 * Get reservations matching a pattern. ckt_id, description and vlan_id fields are checked against
	 * the pattern
	 * @param pattern
	 * @return
	 * @throws Exception
	 */
//	public List<ExistingCircuitReservation> get_matching_reservations(String pattern) throws Exception {
//		String cmd = "?action=get_reservations";
//		try {
//			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
//			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//			cmd+="&search=" + URLEncoder.encode(pattern, HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {}
//		
//		Reader ret = oessSession.executeProvisioningCmd(cmd);
//		
//		Type myType = new TypeToken<OESSAPIResponse<List<ExistingCircuitReservation>>>() {}.getType();
//		OESSAPIResponse<List<ExistingCircuitReservation>> resp = gson.fromJson(ret, myType);
//		
//		if (resp == null)
//			throw new Exception("Unable to parse JSON output for command " + cmd);
//		
//		if ((resp.error == 0) || (resp.success == 1)) 
//			return resp.results;
//		else
//			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
//	}
	
	/**
	 * Create a reservation for a vlan tag
	 * 
	 * @param net
	 * @param wg
	 * @param vlan_id
	 * @param description
	 * @return success?
	 */
//	public boolean add_reservation(int vlan_id, String description) throws Exception {
//		String cmd = "?action=add_reservation";
//		try {
//			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
//			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//			cmd+="&vlan_id=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
//			cmd+="&description=" + URLEncoder.encode(description, HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {}
//		
//		Reader ret = oessSession.executeProvisioningCmd(cmd);
//		
//		Type myType = new TypeToken<OESSAPIResponse<List<VlanAddReservationDefinition>>>() {}.getType();
//		OESSAPIResponse<List<VlanAddReservationDefinition>> resp = gson.fromJson(ret, myType);
//	
//		if (resp != null) {
//			if ((resp.error == 0) || (resp.success == 1)) 
//				return true;
//			else
//				throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
//		} else
//			throw new Exception(CGI_RETURNED_ERROR + " (no error output)" + " for command " + cmd);
//	}
	
	/**
	 * remove a reservation for a vlan tag.
	 * @param net
	 * @param wg
	 * @param vlan_id
	 * @return true if success
	 */
//	public boolean remove_reservation(int vlan_id) throws Exception {
//		String cmd = "?action=remove_reservation";
//		try {
//			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
//			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//			cmd+="&vlan_id=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {}
//		
//		Reader ret = oessSession.executeProvisioningCmd(cmd);
//		
//		Type myType = new TypeToken<OESSAPIResponse<VlanRemoveReservationDefinition>>() {}.getType();
//		OESSAPIResponse<VlanRemoveReservationDefinition> resp = gson.fromJson(ret, myType);
//	
//		if (resp != null) {
//			if ((resp.error == 0) || (resp.success == 1))
//				return true;
//			else
//				throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
//		} else
//			throw new Exception(CGI_RETURNED_ERROR + " (no error output)" + " for command " + cmd);
//		
//	}
	
	/**
	 * Provision a point-to-point curcuit. Lot's of parameters needed. Accepts output of the get_shortest_path call as path. (oess)
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
	public boolean provision_circuit(String node_a, String int_a, String node_z, String int_z,  int tag_a, int tag_z, 
			List<SPDefinition> path, long reserved_bandwidth, 
			String description) throws Exception { 
		String cmd = "?action=provision_circuit";
		// create comma-separated path
		Iterator<SPDefinition> it = path.iterator();
		String csPath = "";
		if (it.hasNext())
			csPath = it.next().link;
		
		while(it.hasNext())
			csPath += " " + it.next().link;
		
		try {
			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&circuit_id=" + URLEncoder.encode(String.format("%d", -1), HTTP.UTF_8);
			cmd+="&description=" + URLEncoder.encode(description, HTTP.UTF_8);
			cmd+="&bandwidth=" + URLEncoder.encode(String.format("%d", reserved_bandwidth), HTTP.UTF_8);
			cmd+="&provision_time=" + URLEncoder.encode(String.format("%d", -1), HTTP.UTF_8);
			cmd+="&remove_time=" + URLEncoder.encode(String.format("%d", -1), HTTP.UTF_8);
			for(SPDefinition l: path) {
				cmd+="&link=" + URLEncoder.encode(l.link, HTTP.UTF_8);
			}
			cmd+="&node=" + URLEncoder.encode(node_a, HTTP.UTF_8);
			cmd+="&interface=" + URLEncoder.encode(int_a, HTTP.UTF_8);
			cmd+="&tags=" + URLEncoder.encode(String.format("%d", tag_a), HTTP.UTF_8);
			
			cmd+="&node=" + URLEncoder.encode(node_z, HTTP.UTF_8);
			cmd+="&interface=" + URLEncoder.encode(int_z, HTTP.UTF_8);
			cmd+="&tags=" + URLEncoder.encode(String.format("%d", tag_z), HTTP.UTF_8);

		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = oessSession.executeProvisioningCmd(cmd);
		
		Type myType = new TypeToken<OESSAPIResponse<CircuitProvisionDefinition>>() {}.getType();
		OESSAPIResponse<CircuitProvisionDefinition> resp = gson.fromJson(ret, myType);
	
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
	 * Remove (tear-down) a previously provisioned circuit (oess)
	 * 
	 * @param ckt_id
	 * @return
	 * @throws Exception
	 */
	public boolean remove_vlan(int ckt_id) throws Exception { 
		String cmd = "?action=remove_circuit";
		try {
			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
			cmd+="&circuit_id=" + URLEncoder.encode(String.format("%d", ckt_id), HTTP.UTF_8);
			cmd+="&remove_time=" + URLEncoder.encode(String.format("%d", -1), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {}
		
		Reader ret = oessSession.executeProvisioningCmd(cmd);
		
		Type myType = new TypeToken<OESSAPIResponse<CircuitRemoveDefinition>>() {}.getType();
		OESSAPIResponse<CircuitRemoveDefinition> resp = gson.fromJson(ret, myType);
	
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
//	public List<VlanStatusDefinition> get_status(int vlan_id, int request_id) throws Exception { 
//		String cmd = "?action=get_status";
//		try {
//			cmd+="&net=" + URLEncoder.encode(String.format("%d", net), HTTP.UTF_8);
//			cmd+="&workgroup_id=" + URLEncoder.encode(String.format("%d", wg), HTTP.UTF_8);
//			cmd+="&vlan_id=" + URLEncoder.encode(String.format("%d", vlan_id), HTTP.UTF_8);
//			cmd+="&request_id=" + URLEncoder.encode(String.format("%d", request_id), HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {}
//		
//		Reader ret = oessSession.executeProvisioningCmd(cmd);
//		
//		Type myType = new TypeToken<OESSAPIResponse<List<VlanStatusDefinition>>>() {}.getType();
//		OESSAPIResponse<List<VlanStatusDefinition>> resp = gson.fromJson(ret, myType);
//	
//		if (resp == null)
//			throw new Exception("Unable to parse JSON output for command " + cmd);
//		
//		if ((resp.error == 0) || (resp.success == 1)) 
//			return resp.results;
//		else
//			throw new Exception(CGI_RETURNED_ERROR + resp.error_text + " for command " + cmd);
//	}
}
