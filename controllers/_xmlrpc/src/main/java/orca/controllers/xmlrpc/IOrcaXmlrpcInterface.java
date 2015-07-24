package orca.controllers.xmlrpc;

import java.util.List;
import java.util.Map;

/**
 * This is the native XMLRPC interface of ORCA controller to the world
 * @author ibaldin
 *
 */
public interface IOrcaXmlrpcInterface {
	
	public Map<String, Object> getVersion();

	public String listResources(Object[] credentials, Map<?,?> options);
	
	public String createSlice(String slice_urn, Object[] credentials, String resReq, List<Map<String, ?>> users);
	
	public String sliceStatus(String slice_urn, Object[] credentials);
	
	public boolean deleteSlice(String slice_urn, Object[] credentials);
	
	public boolean renewSlice(String slice_urn, Object[] credentials, String newTermEnd);
	
}
