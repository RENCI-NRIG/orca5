package orca.handlers.nlr;

/**
 * Parameterized class that describes a generic response from API (including possible error codes) The actual result is
 * in the parameterized 'results' field that depends on the specific API call. The possible classes in 'results' are
 * static members of this class.
 * 
 * @author ibaldin
 *
 * @param <R>
 *            result class
 */
public class SherpaAPIResponse<R> {
    int error;
    int success;
    SherpaError error_text;
    R results;

    @Override
    public String toString() {
        String ret = "Sherpa API Response: error " + error + " success " + success;
        if (error_text != null)
            ret += "\n   " + error_text;
        if (results != null)
            ret += "\n   " + results.toString();

        return ret;
    }

    public SherpaAPIResponse() {
        error = 0;
        success = 0;
    }

    /**
     * Returned in case of error
     */
    static public class SherpaError {
        String stacktrace;
        String timestamp;
        String message;

        @Override
        public String toString() {
            return "Sherpa reports: " + message + " at " + timestamp;
        }
    }

    /**
     * Returned by get_vlans() planning call
     * 
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

        public VlanDefinition() {
        }
    }

    /**
     * returned by get_available_vlan_id() planning call
     * 
     * @author ibaldin
     *
     */
    static public class VlanIdDefinition {
        public int vlan_id;

        public VlanIdDefinition() {
        }
    }

    static public class WGDefinition {
        public int max_vlans;
        public int atlas_map_id;
        public long max_reserved_bw;
        public int admin_network_id;
        public String description;
        public int sherpa_workgroup_id;

        public WGDefinition() {
        }
    }

    static public class VlanIdAvailableDefinition {
        public int available;

        public VlanIdAvailableDefinition() {
        }
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

        public TrunkDefinition() {
        }
    }

    static public class EntityDefinition {
        public String abbr_name;
        public int entity_id;
        public String full_name;

        public EntityDefinition() {
        }
    }

    static public class InterfaceDefinition {
        public int node_id;
        public int interface_id;
        public String description;
        public String int_name;

        public InterfaceDefinition() {
        }
    }

    static public class VlanGetReservationDefinition {
        public String status;
        public String name;
        public int ckt_id;
        public String description;
        public String entity_abbr;
        public int vlan_id;
        public String entity_name;
        public int entity_id;

        public VlanGetReservationDefinition() {
        }
    }

    static public class VlanAddReservationDefinition {
        public String ckt_name;
        public int status;
        public int ckt_id;

        public VlanAddReservationDefinition() {
        }
    }

    static public class VlanRemoveReservationDefinition {
        public String name;

        public VlanRemoveReservationDefinition() {
        }
    }

    // result of get_shortest_path
    static public class SPDefinition {
        public String circuit_id;

        public SPDefinition() {
        }
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

        public VlanStatusDefinition() {
        }
    }

    static public class VlanRemoveDefinition {
        public int config_et;
        public int planning_et;
        public int pretest_et;
        public int ckt_id;
        public int ping_status;
        public String ping_result;
        public int total_et;
        public int ping_et;

        public VlanRemoveDefinition() {
        }
    }

    static public class VlanProvisionDefinition {
        public int config_et;
        public int pretest_et;
        public int ckt_id;
        public int ping_status;
        public String ping_result;
        public int total_et;
        public int ping_et;

        public VlanProvisionDefinition() {
        }
    }

    static public class GetPathDefinition {
        public String name;

        public GetPathDefinition() {
        }
    }
}
