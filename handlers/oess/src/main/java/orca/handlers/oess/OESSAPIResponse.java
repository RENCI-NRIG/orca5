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

package orca.handlers.oess;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Parameterized class that describes a generic response from API (including possible error codes)
 * The actual result is in the parameterized 'results' field that depends on the specific API call.
 * The possible classes in 'results' are static members of this class.
 * 
 * @author ibaldin
 *
 * @param <R> result class
 */
public class OESSAPIResponse<R> {
	int error;
	int success;
	OESSError error_text;
	R results;
	
	@Override
	public String toString() {
		String ret = "OESS API Response: error " + error + " success " + success;
		if (error_text != null)
			ret += "\n   " + error_text;
		if (results != null)
			ret += "\n   " + results.toString();
		
		return ret;
	}
	
	public OESSAPIResponse() {
		error = 0;
		success = 0;
	}
	
	/**
	 * Returned in case of error
	 */
	static public class OESSError {
		String stacktrace;
		String timestamp;
		String message;
		@Override
		public String toString() {
			return "OESS reports: " + message + " at " + timestamp;
		}
	}
	
	/**
	 * Returned by get_vlans() planning call
	 * @author ibaldin
	 *
	 */
	static public class VlanDefinition {
		public String status;
		public int num_endpoints;
		public int ckt_id;
		public String entity_abbr;
		public int state;
		public int ckt_wg_id;
		public int reserved_bw;
		public String z_end_int;
		public int tag;
		public String a_end_node;
		public String a_end_int;
		public String z_end_node;
		public String name;
		public String description;
		public String workgroup;
		public String entity_name;
		public int price_factor;
		public int entity_id;
		public int user_id;
		public int num_segs;
		public VlanDefinition() {}
	}

	/**
	 * returned by get_available_vlan_id() planning call
	 * @author ibaldin
	 *
	 */
	static public class VlanIdDefinition {
		public int vlan_id;
		public VlanIdDefinition() {}
	}
	
	// oess
	static public class WGDefinition {
		public String name;
		public int workgroup_id;
		public WGDefinition() {}
	}
	
	// oess
	static public class VlanIdAvailableDefinition {
		public int available;
		public VlanIdAvailableDefinition() {}
	}
	
	static public class TrunkDefinition {
		public float longitude;
		public float latitude;
		public String short_name;
		public int metric;
		public String name;
		public int interface_id;
		public String int_name;
		public int ckt_id;
		public String admin_state;
		public int node_id;
		public int ckt_endpoint_id;
		public String pop_code;
		public int admin_network_id;
		public String clli_code;
		public TrunkDefinition() {}
	}
	
	static public class EntityDefinition {
		public String abbr_name;
		public int entity_id;
		public String full_name;
		public EntityDefinition() {}
	}
	
	// OESS
	static public class InterfaceDefinition {
		public int interface_id;
		public String description;
		public String name;
		public InterfaceDefinition() {}
	}
	
	// oess
	static public class ExistingCircuitEndpoint {
		@SerializedName("interface ") public int intface;
		public String node;
		public ExistingCircuitEndpoint() {}
	}
	
	// oess
	static public class ExistingCircuitReservation {
		public int circuit_id;
		public int bandwidth;
		public String name;
		public String description;
		public List<ExistingCircuitEndpoint> endpoints;
		public String state;
		public ExistingCircuitReservation() {}
	}
	
	static public class VlanRemoveReservationDefinition {
		public String name;
		public VlanRemoveReservationDefinition() {}
	}
	
	// result of get_shortest_path (oess)
	static public class SPDefinition {
		public String link;
		public SPDefinition() {}
	}
	
	static public class VlanStatusDefinition {
		public String date;
		public String state;
		public int sherpa_workgroup_ckt;
		public int sherpa_workgroup_log_id;
		public int request_id;
		public String user_id;
		public String log;
		public int sherpa_workgroup_id;
		public VlanStatusDefinition() {}
	}
	
	// oess
	static public class CircuitRemoveDefinition {
		public int success;
		public CircuitRemoveDefinition() {}
	}
	
	// oess
	static public class CircuitProvisionDefinition {
		public String circuit_id;
		public int success;
		public CircuitProvisionDefinition() {}
	}
	
	// oess
	static public class OessLink {
		public int interface_z;
		public int port_no_z;
		public String node_z;
		public int port_no_a;
		public String node_a;
		public String name;
		public OessLink() {}
	}
	
	// oess
	static public class CircuitEndpoint {
		public int local;
		public String node;
		public int port_no;
		public int node_id;
		public String urn;
		@SerializedName("interface ") public int intface;
		public int tag;
		public String role;
		public CircuitEndpoint() {}
	}
	
	// oess (needs work /ib)
	static public class InternalIds {
		public Object primary;
		public InternalIds() {}
	}
	
	// oess
	static public class GetCircuitDefinition {
		public int circuit_id;
		public String name;
		public String description;
		public List<CircuitEndpoint> endpoints;
		public String state;
		public List<Object> backup_links;
		public int bandwidth;
		public String active_path;
		public InternalIds internal_ids;
		public List<OessLink> links;
		public GetCircuitDefinition() {}
	}
}
