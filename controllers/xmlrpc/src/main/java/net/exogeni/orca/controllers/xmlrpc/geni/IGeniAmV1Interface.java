package net.exogeni.orca.controllers.xmlrpc.geni;

import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

/**
 * 
 * @author ibaldin
 *
 */
public interface IGeniAmV1Interface {

    public Map<String, Object> GetVersion() throws XmlRpcException, Exception;

    public String ListResources(Object[] credentials, Map<?, ?> options);

    public String CreateSliver(String slice_urn, Object[] credentials, String resReq, List<Map<String, ?>> users);

    public String SliverStatus(String slice_urn, Object[] credentials);

    public boolean DeleteSliver(String slice_urn, Object[] credentials);

    public boolean RenewSliver(String slice_urn, Object[] credentials, String newTermEnd);

    boolean Shutdown(String slice_urn, Object[] credentials);
}
