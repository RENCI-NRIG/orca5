package orca.handlers.network.openflow;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import orca.handlers.network.core.CommandException;
import orca.handlers.network.core.XMLRPCDevice;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.flowvisor.api.FlowChange;
import org.flowvisor.api.FlowChange.FlowChangeOp;
import org.flowvisor.exceptions.MalformedFlowChange;

public class FlowVisorDevice extends XMLRPCDevice implements OpenFlowConstants {
	
	private static final String PRIORITY_REGEX = "\\d+";
	private static final String PORTS_REGEX = "any|(\\s*\\d+\\s*|\\s*\\d+\\s*-\\s*\\d+\\s*)\\s*(\\s*,\\s*(\\d+|\\d+\\s*-\\s*\\d+)\\s*)*";
	private static final String DPID_REGEX = "([0-9a-fA-F][0-9a-fA-F])(:[0-9a-fA-F][0-9a-fA-F]){7}|all";
	protected String uid;
	protected String passwd;

	public FlowVisorDevice(String url, String uid, String passwd){
		super(url);
		
		this.uid = uid;
		this.passwd = passwd;		
	}
	
	public Object createSlice(String name, String passwd, String controller, String email) throws CommandException {
        return execute("api.createSlice", new Object[] { name.trim(), passwd, controller, email });
    }
	
	public Object deleteSlice(String name) throws CommandException {
		return execute("api.deleteSlice", new Object[] { name.trim() });
	}
	
	/**
	 * Create an IP based flow space
	 * @param name
	 * @param dpid
	 * @param priority
	 * @param srcIP
	 * @param dstIP
	 * @throws CommandException
	 */
	public void addIPFlowSpace(String name, String dpid, String priority, String srcIP, String dstIP) throws CommandException {
		List<Map<String, String>> mapList = new LinkedList<Map<String, String>>();

		if (!dpid.trim().matches(DPID_REGEX))
			throw new CommandException("addIPFlowSpace(): Invalid DPID format: " + dpid);
		dpid = dpid.trim();
		
		if (!priority.trim().matches(PRIORITY_REGEX))
			throw new CommandException("addIPFlowSpace(): Invalid priority string: " + priority);
		priority = priority.trim();
		
		String actions = "Slice:" + name.trim() + "=4";
		
		try {
			String match = "nw_src=" + srcIP + ",nw_dst=" + dstIP;
			Map<String, String> map_src_to_dst = FlowChange.makeMap(
					FlowChangeOp.ADD, dpid, null, priority, match, actions);
			FlowChange.fromMap(map_src_to_dst);
			mapList.add(map_src_to_dst);
			
			match = "nw_src=" + dstIP + ",nw_dst=" + srcIP;
			Map<String, String> map_dst_to_src = FlowChange.makeMap(
					FlowChangeOp.ADD, dpid, null, priority, match, actions);
			FlowChange.fromMap(map_dst_to_src);
			mapList.add(map_dst_to_src);
			execute("api.changeFlowSpace", new Object[] { mapList });
		} catch (Exception e) {
			throw new CommandException(e); 
		}
    }
	
	/**
	 * Create a VLAN based flow space on specific ports
	 * @param name
	 * @param dpid
	 * @param priority
	 * @param tag
	 * @param ports - can be a combination of range and comma-separated list: a-b,c,d,e-f etc. It can also say 'any'
	 * @throws CommandException
	 */
	public void addVlanFlowSpace(String name, String dpid, String priority, int tag, String ports) throws CommandException {
		
		if ((name == null) || (dpid == null) || (priority == null) || (ports == null))
			throw new CommandException("addVlanFlowSpace(): Slice name, dpid, priority and fvPort list must be specified ");
		
		List<Map<String, String>> mapList = new LinkedList<Map<String, String>>();
		String actions = "Slice:" + name.trim() + "=4";
		
		if (!priority.trim().matches(PRIORITY_REGEX))
			throw new CommandException("addVlanFlowSpace(): Invalid priority string: " + priority);
		priority = priority.trim();
		
		if (!dpid.trim().matches(DPID_REGEX))
			throw new CommandException("addVlanFlowSpace(): Invalid DPID format: " + dpid);
		dpid = dpid.trim();
		
		// convert ports into a list
		if (!ports.matches(PORTS_REGEX))
			throw new CommandException("addVlanFlowSpace(): Invalid ports string: " + ports);
		ports = ports.trim();
		
		// to avoid repetitions
		Set<String> enumeratedPorts = new HashSet<String>();
		
		try {
			if (ports.matches("any")) {
				// insert one rule
				String match = "dl_vlan=" + tag;
				Map<String, String> mapVlan = FlowChange.makeMap(
						FlowChangeOp.ADD, dpid, null, priority, match, actions);
				FlowChange.fromMap(mapVlan);
				mapList.add(mapVlan);
			} else {
				// insert a bunch of rules
				String[] groups = ports.trim().split(",");
				for (String group: groups) {
					if (group.matches("\\s*\\d+\\s*-\\s*\\d+\\s*")) {
						String[] maxMin = group.split("-");
						Integer minP = Integer.decode(maxMin[0].trim());
						Integer maxP = Integer.decode(maxMin[1].trim());
						if (maxP < minP) {
							// swap
							minP += maxP;
							maxP = minP - maxP;
							minP = minP - maxP;
						}
						for(Integer i = minP; i <= maxP; i++) {
							enumeratedPorts.add(i.toString());
						}
					} else {
						enumeratedPorts.add(group.trim());
					}
				}
				for(String port: enumeratedPorts) {
					// create a rule per fvPort
					String match = "in_port=" + port + ",dl_vlan=" + tag;
					Map<String, String> mapVlan = FlowChange.makeMap(
							FlowChangeOp.ADD, dpid, null, priority, match, actions);
					FlowChange.fromMap(mapVlan);
					mapList.add(mapVlan);
				}
			}
			// call flowvisor
			execute("api.changeFlowSpace", new Object[] { mapList });
		} catch (MalformedFlowChange mfce) {
			throw new CommandException("addVlanFlowSpace(): malformed flow change: " + mfce);
		}
		
	}
	
	@Override
	public void connect() throws CommandException {
		if (isEmulationEnabled())
			return;
		installDumbTrust();
		config = new XmlRpcClientConfigImpl();
		config.setBasicUserName(uid);
		config.setBasicPassword(passwd);
		try {
			config.setServerURL(new URL(url));
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			logger.error("FlowVisorDevice: unable to connect to " + url + " user " + uid + " pass " + passwd + " due to URL problem " + e.getMessage());
			throw new CommandException(e.getMessage());
		} catch (Exception e) {
			logger.error("FlowVisorDevice: unable to connect to " + url + " user " + uid + " pass " + passwd + " due to exception " + e.getMessage());
			throw new CommandException(e.getMessage());
		}

		config.setEnabledForExtensions(true);

		client = new XmlRpcClient();
		client.setConfig(config);
	}
	
	@Override
	public Object execute(String exec, Object[] param) throws CommandException {
		if (isEmulationEnabled())
			return null;
		Object reply = null;
		connect();
		try {
			reply = this.client.execute(exec, param);
		} catch (XmlRpcException e) {
			throw new CommandException(e.getMessage());
		}
		return reply;
	}	
	
	// Create a trust manager that does not validate certificate chains
	public void installDumbTrust() {
//		System.err.println("WARN: blindly trusting server cert - FIXME");
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
				// Trust always
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
				// Trust always
			}
		} };
		try {
			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			// Create empty HostnameVerifier
			HostnameVerifier hv = new HostnameVerifier() {
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			};

			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (KeyManagementException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	
	public static void main(String[] argv) {
		System.out.println("STARTING");
		try {
			FlowVisorDevice fvd = new FlowVisorDevice(null, null, null);
			fvd.enableEmulation();
			fvd.addVlanFlowSpace("IliaSlice", "00:c8:08:17:f4:a6:6a:00", " 10 ", 115, 
			//" 5 - 8 , 6, 6, 6, 10,12,15-19,20,34-36, 35, 105-101");
					"0,2,3,4");
			//fvd.addVlanFlowSpace("IliaSlice", "00:c8:08:17:f4:a6:6a:00", " 10 ", 115, "any");
		} catch (Exception e) {
			System.err.println("ERROR: " + e);
			e.printStackTrace();
		}
		System.out.println("DONE");
	}
}
