package orca.handlers.network.core;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public abstract class XMLRPCDevice implements INetworkDevice {
	protected Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

	protected String name = null;

	protected String url;

	protected XmlRpcClientConfigImpl config;
	protected XmlRpcClient client;

	protected boolean connected = false;
	protected boolean isEmulation = false;

	public XMLRPCDevice(String deviceUrl) {
		this.url = deviceUrl;
	}

	public void setUrl(String address, String port) {
		url = "https://" + address + ":" + port + "/xmlrpc";
	}

	public String getUrl() {
		return url;
	}

	public Object execute(String exec, Object[] param) throws CommandException {
		Object reply = null;
		try {
			reply = (Boolean) this.client.execute(exec, param);
		} catch (XmlRpcException e) {
			throw new CommandException(e.getMessage());
		}
		return reply;
	}

	public void connect() throws CommandException {
		if (isEmulationEnabled())
			return;
		config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL(this.url));
		} catch (MalformedURLException e) {
			throw new CommandException(e.getMessage());
		} catch (Exception e) {
			logger.error("XMLRPCDevice: unable to connect due to exception " + e.getMessage());
			throw new CommandException(e.getMessage());
		}

		client = new XmlRpcClient();
		client.setConfig(config);

		connected = true;
	}

	public void disconnect() {
		if (isEmulationEnabled())
			return;
	}

	public boolean isConnected() {
		return connected;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void enableEmulation() {
		isEmulation = true;
	}

	public void disableEmulation() {
		isEmulation = false;
	}

	public boolean isEmulationEnabled() {
		return isEmulation;
	}

}
