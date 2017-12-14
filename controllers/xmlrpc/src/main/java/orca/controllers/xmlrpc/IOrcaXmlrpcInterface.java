package orca.controllers.xmlrpc;

import java.util.List;
import java.util.Map;

/**
 * This is the native XMLRPC interface of ORCA controller to the world
 * 
 * @author ibaldin
 *
 */
public interface IOrcaXmlrpcInterface {

    public Map<String, Object> getVersion();

    public Map<String, Object> listResources(Object[] credentials, Map<?, ?> options);

    public Map<String, Object> createSlice(String slice_urn, Object[] credentials, String resReq,
            List<Map<String, ?>> users);

    public Map<String, Object> sliceStatus(String slice_urn, Object[] credentials);

    public Map<String, Object> deleteSlice(String slice_urn, Object[] credentials);

    public Map<String, Object> renewSlice(String slice_urn, Object[] credentials, String newTermEnd);

    public Map<String, Object> modifySlice(String slice_urn, Object[] credentials, String modReq);

    public Map<String, Object> modifySliver(String slice_urn, String sliver_guid, Object[] credentials,
            String modifySubcommand, List<Map<String, ?>> modifyProperties);

}
