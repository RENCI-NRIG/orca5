/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.proxies.soapaxis2;

import java.io.IOException;
import java.net.ConnectException;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;

import orca.security.AuthToken;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IProxy;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.common.ReservationID;
import orca.shirako.core.RPCRequestState;
import orca.shirako.kernel.RPCRequestType;
import orca.shirako.proxies.Proxy;
import orca.shirako.proxies.soapaxis2.beans.Plist;
import orca.shirako.proxies.soapaxis2.beans.PlistNode;
import orca.shirako.proxies.soapaxis2.services.ActorServiceSkeleton;
import orca.shirako.proxies.soapaxis2.services.ActorServiceStub;
import orca.shirako.proxies.soapaxis2.services.AuthorityServiceStub;
import orca.shirako.proxies.soapaxis2.services.BrokerServiceStub;
import orca.shirako.proxies.soapaxis2.services.FailedRPC;
import orca.shirako.proxies.soapaxis2.services.Query;
import orca.shirako.proxies.soapaxis2.services.QueryResult;
import orca.shirako.proxies.soapaxis2.util.Translate;
import orca.shirako.util.RPCError;
import orca.shirako.util.RPCException;
import orca.util.ID;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Stub;
import org.apache.log4j.Logger;

/**
 * o Base proxy class for all SOAP proxies.
 */
public abstract class SoapAxis2Proxy extends Proxy implements ICallbackProxy {
    public static final String SoapFaultNamespace = "http://schemas.xmlsoap.org/soap/envelope/";
    protected class SoapAxis2ProxyRequestState extends RPCRequestState {
        orca.shirako.proxies.soapaxis2.beans.Reservation reservation;
        orca.shirako.proxies.soapaxis2.beans.UpdateData udd;
        orca.shirako.proxies.soapaxis2.beans.Properties query;
        String callbackUrl;
        String requestID; // requestID sent back to link responses with requests
        ReservationID failedReservationID;
        RPCRequestType failedRequestType;
        String errorDetail;
    }

    public static final int TypeDefault = 0;
    public static final int TypeReturn = 1;
    public static final int TypeBroker = 2;
    public static final int TypeSite = 3;
    public static final String PropertyServiceEndPoint = "WSProxyEndPoint";

    /**
     * The endpoint of the actor service
     */
    @Persistent(key = PropertyServiceEndPoint)
    protected String serviceEndpoint;

    /**
     * Type for the stub.
     */
    @NotPersistent
    protected int stubType = TypeDefault;

    /**
     * Create a new SOAP proxy
     * @param serviceEndpoint Endpoint of the actor service
     * @param identity <code>AuthToken</code> representing the the actor
     * @param logger The logger
     */
    protected SoapAxis2Proxy(String serviceEndpoint, AuthToken identity, Logger logger) {
        super(identity);
    	this.serviceEndpoint = serviceEndpoint;
        this.logger = logger;
        this.proxyType = IProxy.ProxyTypeSoapAxis2;
    }

    public SoapAxis2Proxy() {
        this.proxyType = IProxy.ProxyTypeSoapAxis2;
    }

    /**
     * Classifies the soap fault and maps it to an RPCError.
     * @param qname soap fault
     * @return orca rpc error type
     */
    protected RPCError getRPCError(AxisFault e) {        
        QName qname = e.getFaultCode();
        
        if (qname == null) {
            Throwable t =e.getCause();
            if (t instanceof IOException) {
                return RPCError.NetworkError;
            }else {
                t = e.getCause();
                if ((t != null) && (t instanceof IOException)) {
                    return RPCError.NetworkError;
                }
            }
            // other special cases?
            return RPCError.Unknown;
        }
        
        if (ActorServiceSkeleton.OrcaSoapFaultNamespace.equals(qname.getNamespaceURI())) {
            try {
                return RPCError.valueOf(qname.getLocalPart());
            } catch (Exception ee) {
                return RPCError.Unknown;
            }
        }else if (SoapFaultNamespace.equals(qname.getNamespaceURI())) {
            if ("Client".equalsIgnoreCase(qname.getLocalPart())) {
                return RPCError.LocalError;
            } else if ("Server".equalsIgnoreCase(qname.getLocalPart())) {
                // this is a remote error, but the spec says that the message should be retried, 
                // so we treat this as a network error, since those are the only ones we retry
                return RPCError.NetworkError;
            } else if ("VersionMismatch".equalsIgnoreCase(qname.getLocalPart())) {
                return RPCError.InvalidRequest;
            } else if ("MustUnderstand".equalsIgnoreCase(qname.getLocalPart())) {
                return RPCError.InvalidRequest;
            } else {
                return RPCError.Unknown;
            }
        }
        return RPCError.NetworkError;
    }

    public void execute(IRPCRequestState state) throws RPCException {
        SoapAxis2ProxyRequestState soap = (SoapAxis2ProxyRequestState) state;
        switch (soap.getType()) {
            case Query: {
                try {
                    Stub stub = (Stub) getServiceStub(soap.getCaller());
                    Query request = new Query();
                    request.setMessageID(soap.getMessageID());
                    request.setProperties(soap.query);
                    request.setCallbackURL(soap.callbackUrl);
                    if (stub instanceof BrokerServiceStub) {
                        ((BrokerServiceStub) stub).query(request, soap.getCaller());
                    } else if (stub instanceof AuthorityServiceStub) {
                        ((AuthorityServiceStub) stub).query(request, soap.getCaller());
                    } else {
                        ((ActorServiceStub) stub).query(request, soap.getCaller());
                    }
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    RPCError rpcError = getRPCError(e);
                    throw new RPCException(rpcError, e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;
            case QueryResult: {
                try {
                    Stub stub = (Stub) getServiceStub(soap.getCaller());
                    QueryResult request = new QueryResult();
                    request.setMessageID(soap.getMessageID());
                    request.setProperties(soap.query);
                    request.setRequestID(soap.requestID);
                    if (stub instanceof BrokerServiceStub) {
                        ((BrokerServiceStub) stub).queryResult(request, soap.getCaller());
                    } else if (stub instanceof AuthorityServiceStub) {
                        ((AuthorityServiceStub) stub).queryResult(request, soap.getCaller());
                    } else {
                        ((ActorServiceStub) stub).queryResult(request, soap.getCaller());
                    }
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;
                
            case FailedRPC: {
                try {
                    Stub stub = (Stub) getServiceStub(soap.getCaller());
                    FailedRPC request = new FailedRPC();
                    request.setMessageID(soap.getMessageID());
                    request.setRequestID(soap.requestID);
                    request.setRequestType(soap.failedRequestType.ordinal());
                    if (soap.failedReservationID != null) {
                        request.setReservationID(soap.failedReservationID.toString());
                    } else {
                        request.setReservationID("");
                    }
                    request.setErrorDetails(soap.errorDetail);
                    if (stub instanceof BrokerServiceStub) {
                        ((BrokerServiceStub) stub).failedRPC(request, soap.getCaller());
                    } else if (stub instanceof AuthorityServiceStub) {
                        ((AuthorityServiceStub) stub).failedRPC(request, soap.getCaller());
                    } else {
                        ((ActorServiceStub) stub).failedRPC(request, soap.getCaller());
                    }
                } catch (SoapAxis2StubException e) {
                    throw new RPCException(RPCError.LocalError, e);
                } catch (AxisFault e) {
                    throw new RPCException(getRPCError(e), e);
                } catch (RemoteException e) {
                    throw new RPCException(RPCError.Unknown, e);
                }
            }
                break;
                
            default:
                throw new RPCException("Unsupported RPC: type=" + soap.getType(), RPCError.LocalError);
        }
    }

    public IRPCRequestState prepareQuery(ICallbackProxy callback, Properties query, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.query = Translate.translate(query);
        state.callbackUrl = ((SoapAxis2Proxy) callback).getServiceEndpoint();
        return state;
    }

    public IRPCRequestState prepareQueryResult(String requestID, Properties response, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.query = Translate.translate(response);
        state.requestID = requestID;
        return state;

    }

    public IRPCRequestState prepareFailedRPC(String requestID, RPCRequestType failedRequestType, ReservationID failedReservationID, String errorDetail, AuthToken caller) {
        SoapAxis2ProxyRequestState state = new SoapAxis2ProxyRequestState();
        state.requestID = requestID;
        state.failedRequestType = failedRequestType;
        state.failedReservationID = failedReservationID;
        state.errorDetail = errorDetail;
        return state;
    }
        
    /**
     * Returns the service stub for the specified caller
     * @param caller caller using the stub
     * @return service stub
     */
    protected Object getServiceStub(AuthToken caller) throws SoapAxis2StubException {
        Object stub = null;
        try {
            if (caller == null || caller.getGuid() == null) {
                throw new IllegalArgumentException("caller is invalid");
            }
            stub = StubManager.getInstance().getStub(caller.getGuid(), serviceEndpoint, stubType);
        } catch (Exception e) {
            throw new SoapAxis2StubException("An error occurred while obtaining service stub", e);
        }

        if (stub == null) {
            throw new SoapAxis2StubException("Could not obtain service stub");
        }
        return stub;
    }

    /**
     * SOAP-Encodes a properties list
     * @param properties Properties list
     * @return
     */
    public static Plist encodePropertiesSoap(Properties properties) {
        if ((properties == null) || (properties.size() == 0)) {
            return null;
        }

        PlistNode[] nodes = new PlistNode[properties.size()];
        Set<?> set = properties.entrySet();
        Iterator<?> iter = set.iterator();
        int i = 0;

        while (iter.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            PlistNode node = new PlistNode();
            node.setName((String) entry.getKey());
            node.setValue((String) entry.getValue());
            nodes[i++] = node;
        }

        Plist list = new Plist();
        list.setPlistNode(nodes);
        return list;
    }

    /**
     * Decode a SOAP-encoded properties list.
     * @param list Soap-encoded list
     * @return
     */
    public static Properties decodePropertiesSoap(Plist list) {
        if (list == null) {
            return null;
        }

        PlistNode[] nodes = list.getPlistNode();

        if ((nodes == null) || (nodes.length == 0)) {
            return null;
        }

        Properties p = new Properties();

        for (int i = 0; i < nodes.length; i++) {
            p.setProperty(nodes[i].getName(), nodes[i].getValue());
        }

        return p;
    }

    /**
     * Indexes the elements of the list into a hash table<br>
     * The hashtable will contain only entries with non-null value
     * @param list List of name, value pairs
     * @return
     */
    public static Hashtable<String, Object> getTable(Plist list) {
        PlistNode[] nodes = list.getPlistNode();
        assert nodes != null;

        Hashtable<String, Object> table = new Hashtable<String, Object>();

        for (int i = 0; i < nodes.length; i++) {
            PlistNode n = nodes[i];
            Object value = n.getValue();

            if (value == null) {
                value = n.getPvalue();
            }

            if (value != null) {
                table.put(n.getName(), value);
            }
        }

        return table;
    }

    /**
     * Returns the service endpoint of the actor
     * @return
     */
    public String getServiceEndpoint() {
        return serviceEndpoint;
    }
}
