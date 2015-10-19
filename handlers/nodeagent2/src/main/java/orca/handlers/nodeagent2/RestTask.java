package orca.handlers.nodeagent2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import orca.nodeagent2.agentlib.PluginReturn;
import orca.nodeagent2.agentlib.Properties;
import orca.nodeagent2.agentlib.ReservationId;
import orca.shirako.plugins.config.OrcaAntTask;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.tools.ant.BuildException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Generic REST Task for NA2
 * @author ibaldin
 *
 */
public abstract class RestTask extends OrcaAntTask {
	protected final static String USERNAME="admin";
	protected String password;
	protected String na2Url;
	protected URL asUrl;
	protected String prefix, returnPrefix;
	protected String plugin;
	protected String reservationId;
	protected String statusProperty;
	protected String errorMsgProperty;
	protected String reservationIdProperty;

	protected static enum RestOperations { JOIN, LEAVE, MODIFY};

	protected RestOperations rop;

	/**
	 * Defaults to 80
	 * @return
	 */
	protected int getPort() {
		if (asUrl.getPort() == -1)
			return 80;
		return asUrl.getPort();
	}

	protected String getHost() {
		return asUrl.getHost();
	}

	/*
	 * Task setters
	 */
	public void setPassword(String s) {
		password = s;
	}

	public void setBaseUrl(String u) throws BuildException {
		na2Url = u;
		try {
			asUrl = new URL(u);
		} catch (MalformedURLException ue) {
			throw new BuildException("Invalid NA2 URL " + u);
		}
	}

	public void setPrefix(String p) {
		prefix = p;
	}

	public void setReturnPrefix(String rp) {
		returnPrefix = rp;
	}

	public void setPlugin(String p) {
		plugin = p;
	}

	public void setReservationId(String s) {
		reservationId = s;
	}

	/*
	 * Set names of properties that will be returned
	 */
	public void setStatusProperty(String s) {
		statusProperty = s;
	}

	public void setReservationIdProperty(String s) {
		reservationIdProperty = s;
	}

	public void setErrorMsgProperty(String s) {
		errorMsgProperty = s;
	}

	/**
	 * Collect properties with the specified prefix
	 * @return
	 */
	protected JSONObject collectPluginProperties() {
		JSONObject p = new JSONObject();

		Hashtable<?,?> props = getProject().getProperties();

		if (props == null)
			return null;
		
		Enumeration<?> e = props.keys();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			String val = (String)props.get(key);
			if (key.startsWith(prefix)) 
				p.put(key, val);
		}
		return p;
	}

	/**
	 * Post to http POST object. Resulting properties are put back on the project
	 * @param se
	 * @return
	 */
	protected void doPost() throws BuildException {
		DefaultHttpClient client = new DefaultHttpClient();

		//System.out.println("Executing " + rop + " for " + na2Url);
		try {
			client.getCredentialsProvider().
			setCredentials(new AuthScope(getHost(), getPort()),
					new UsernamePasswordCredentials(USERNAME,  password));

			if ((rop != RestOperations.JOIN) && (reservationId == null))
				throw new BuildException("Attribute reservationId must be specified for this operation");
			
			HttpPost post = new HttpPost(na2Url + "/" + rop.name().toLowerCase() + "/" + plugin + (reservationId != null ? "/" + reservationId : ""));

			// collect matching properties 
			JSONObject p = collectPluginProperties();

			StringEntity se;
			if (p != null) 
				se = new StringEntity(p.toString());
			else 
				se = new StringEntity("");

			se.setContentType("application/json");

			// post the command and wait for response
			post.setEntity(se);

			HttpResponse response = client.execute(post);
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) { 
				sb.append(line);
			}
			JSONObject o = (JSONObject)JSONValue.parse(sb.toString());

			PluginReturn pr = convert(o);

			// set return properties
			getProject().setProperty(statusProperty, pr.getStatus() + "");
			getProject().setProperty(errorMsgProperty, pr.getErrorMsg());

			if (pr.getResId() != null) 
				getProject().setProperty(reservationIdProperty, pr.getResId().getId());
			else
				getProject().setProperty(reservationIdProperty, "Not available");

			if (pr.getProperties() != null) {
				// 	set properties returned by the plugin
				for(Map.Entry<String, String> me: pr.getProperties().entrySet()) {
					getProject().setProperty(returnPrefix + "." + me.getKey(), me.getValue());
					//System.out.println("Adding property " + returnPrefix + "." + me.getKey() + "=" + me.getValue());
				}
			}

		} catch(BuildException be) {
			throw be;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildException("Unable to POST " + rop + " to " + plugin + ": " + e);
		}
	}

	/**
	 * Convert JSON object to plugin return status
	 * @param o
	 * @return
	 * @throws BuildException
	 */
	private static PluginReturn convert(JSONObject o) throws BuildException {
		long status = (Long)o.get("status");
		int st = (int)status;

		try {
			JSONObject ridEnvelope = (JSONObject)o.get("resId");
			ReservationId rid = null;
			if (ridEnvelope != null)
				rid = new ReservationId((String)ridEnvelope.get("id"));

			Properties props = null;
			if (o.get("properties") != null) {
				props = new Properties();
				props.putAll((JSONObject)o.get("properties"));
			}
			
			/**
			 * If HTTP error is returned, then it is 'message', if plugin error, it is 'errorMsg'
			 */
			String msg = (String)o.get("errorMsg");
			if (msg == null)
				msg = (String)o.get("message");
			PluginReturn pr = new PluginReturn(st, msg, rid, props);
			return pr;
		} catch (Exception e) {
			throw new BuildException("Unable to convert plugin status " + o);
		}               
	}

	public abstract void execute() throws BuildException;

	protected void _execute() throws BuildException {
		// check validity
		if ((plugin == null) || (password == null) || 
				(na2Url == null) || (prefix == null) || 
				(returnPrefix == null))
			throw new BuildException("The following input attributes must be specified for the task: plugin, password, baseUrl, prefix and returnPrefix");

		if ((statusProperty == null) || 
				(reservationIdProperty == null) || 
				(errorMsgProperty == null)) 
			throw new BuildException("The following attributes denoting expected property names must be specified in order to produce output: statusProperty, reservationIdProperty, errorMsgProperty");

		doPost();
	}

}
