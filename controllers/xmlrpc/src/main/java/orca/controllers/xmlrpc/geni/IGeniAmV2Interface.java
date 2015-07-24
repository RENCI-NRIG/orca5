package orca.controllers.xmlrpc.geni;

import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

/**
 * 
 * @author ibaldin
 *
 */
public interface IGeniAmV2Interface {

	/**
	 * Standard return field names
	 * @author ibaldin
	 *
	 */
	public enum ApiReturnFields {
		GENI_API("geni_api"),
		CODE("code"),
		CODE_GENI_CODE("geni_code"),
		CODE_AM_TYPE("am_type"),
		CODE_AM_CODE("am_code"),
		VALUE("value"),
		VALUE_API_VERSIONS("geni_api_versions"),
		VALUE_API_VERSIONS_URL("URL"),
		VALUE_REQ_RSPEC_VERSIONS("geni_request_rspec_versions"),
		VALUE_AD_RSPEC_VERSIONS("geni_ad_rspec_versions"),
		VALUE_RSPEC_TYPE("type"),
		VALUE_RSPEC_VERSION("version"),
		VALUE_RSPEC_SCHEMA("schema"),
		VALUE_RSPEC_NAMESPACE("namespace"),
		VALUE_RSPEC_EXTENSIONS("extensions"),
		OUTPUT("output"),
		GENI_URN("geni_urn"),
		GENI_STATUS("geni_status"),
		GENI_RESOURCES("geni_resources"),
		GENI_ERROR("geni_error"),
		GENI_AM_TYPE("geni_am_type"),
		
		ORCA_EXPIRES("orca_expires"),
		ORCA_VERSION("orca_version");
		
		public String name;
		ApiReturnFields(String s) {
			name = s;
		}
	}
	
	public enum ApiOptionFields {
		// for list resources
		GENI_COMPRESSED("geni_compressed"),
		GENI_SLICE_URN("geni_slice_urn"),
                GENI_RSPEC_VERSION("geni_rspec_version"),
                
                // for speaks-for credential check
                GENI_SPEAKING_FOR("geni_speaking_for"),
                
                // for "as long as possible" Renew option
                GENI_ALAP("geni_extend_alap");
		
		public String name;
		ApiOptionFields(String s) {
			name = s;
		}
	}
	
	/** slice states
	 * 
	 * @author ibaldin
	 *
	 */
	public enum GeniStates {
		CONFIGURING("configuring"),
		READY("ready"),
		FAILED("failed"),
		UNKNOWN("unknown");
		
		public String name;
		GeniStates(String s) {
			name = s;
		}
	}
	
	/**
	 * Standard return codes
	 * @author ibaldin
	 *
	 */
	public enum ApiReturnCodes {
		SUCCESS(0),
		BADARGS(1),
		ERROR(2),
		FORBIDDEN(3),
		BADVERSION(4),
		SERVERERROR(5),
		TOOBIG(6),
		REFUSED(7),
		TIMEDOUT(8),
		DBERROR(9),
		RPCERROR(10),
		UNAVAILABLE(11),
		SEARCHFAILED(12),
		UNSUPPORTED(13),
		BUSY(14),
		EXPIRED(15),
		INPROGRESS(16),
		ALREADYEXISTS(17);
		
		public int code;
		ApiReturnCodes(int i) {
			code = i;
		}
	}
	
	// GetVersion Return struct:
	//	{
	//		  int geni_api;
	//		  struct code = {
	//		       int geni_code;
	//		       [optional: string am_type;]
	//		       [optional: int am_code;]
	//		         }
	//		  struct value
	//		      {
	//		        int geni_api;
	//		        struct geni_api_versions {
	//		             URL <this API version #>; # value is a URL, name is a number
	//		             [optional: other supported API versions and the URLs where they run]
	//		        }
	//		        array geni_request_rspec_versions of {
	//		             string type;
	//		             string version;
	//		             string schema;
	//		             string namespace;
	//		             array extensions of string;
	//		        };
	//		        array geni_ad_rspec_versions of {
	//		             string type;
	//		             string version;
	//		             string schema;
	//		             string namespace;
	//		             array extensions of string;
	//		        };
	//		      }
	//		  string output;
	//		}
	public Map<String, Object> GetVersion(Map<String, Object> options) throws XmlRpcException, Exception;
	//public Map<String, Object> GetVersion() throws XmlRpcException, Exception;
	
	
	// ListResources return struct:
	//	{
	//		  struct code = {
	//		       int geni_code;
	//		       [optional: string am_type;]
	//		       [optional: int am_code;]
	//		         }
	//		  string value;
	//		  string output;
	//		}
	public Map<String, Object> ListResources(Object[] credentials, Map<?,?> options);
	//public Map<String, Object> ListResources(Object[] credentials);
	
	// CreateSliver return struct:
	//	{
	//		  struct code = {
	//		       int geni_code;
	//		       [optional: string am_type;]
	//		       [optional: int am_code;]
	//		         }
	//		  string value;
	//		  string output;
	//		}
	public Map<String, Object> CreateSliver(String slice_urn, Object[] credentials, String resReq, List<Map<String, ?>> users, 
			Map<String, Object> options);
	//public Map<String, Object> CreateSliver(String slice_urn, Object[] credentials, String resReq, List<Map<String, ?>> users);
	
	// SliverStatus return struct:
	//	{
	//		  geni_urn: <sliver URN>
	//		  geni_status: ready
	//		  geni_resources: [ { geni_urn: <resource URN>
	//		                      geni_status: ready
	//		                      geni_error: ''},
	//		                    { geni_urn: <resource URN>
	//		                      geni_status: ready
	//		                      geni_error: ''}
	//		                  ]
	//		}
	public Map<String, Object> SliverStatus(String slice_urn, Object[] credentials, Map<String, Object> options);
	//public Map<String, Object> SliverStatus(String slice_urn, Object[] credentials);
	
	// DeleteSliver return struct
	//	{
	//		  struct code = {
	//		       int geni_code;
	//		       [optional: string am_type;]
	//		       [optional: int am_code;]
	//		         }
	//		  string value;
	//		  string output;
	//		}
	public Map<String, Object> DeleteSliver(String slice_urn, Object[] credentials, Map<String, Object> options);
	//public Map<String, Object> DeleteSliver(String slice_urn, Object[] credentials);
	
	// RenewSliver return struct:
	//	{
	//		  struct code = {
	//		       int geni_code;
	//		       [optional: string am_type;]
	//		       [optional: int am_code;]
	//		         }
	//		  string value;
	//		  string output;
	//		}
	public Map<String, Object> RenewSliver(String slice_urn, Object[] credentials, String newTermEnd, Map<String, Object> options);
	//public Map<String, Object> RenewSliver(String slice_urn, Object[] credentials, String newTermEnd);
	
	// Shutdown return struct:
	//	{
	//		  struct code = {
	//		       int geni_code;
	//		       [optional: string am_type;]
	//		       [optional: int am_code;]
	//		         }
	//		  string value;
	//		  string output;
	//		}
	public Map<String, Object> Shutdown(String slice_urn, Object[] credentials, Map<String, Object> options);
	//public Map<String, Object> Shutdown(String slice_urn, Object[] credentials);
}
