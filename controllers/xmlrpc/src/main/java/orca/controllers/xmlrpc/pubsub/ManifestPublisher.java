/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc.pubsub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import orca.controllers.OrcaController;

import org.apache.log4j.Logger;
import org.renci.xmpp_pubsub.XMPPPubSub;

/**
 *
 * @author anirban
 */
public class ManifestPublisher {

	private static final String PUBSUB_PROP_PREFIX = "ORCA.pubsub";
	private static final String PUBSUB_SERVER_PROP = PUBSUB_PROP_PREFIX + ".server";
	private static final String PUBSUB_LOGIN_PROP = PUBSUB_PROP_PREFIX + ".login";
	private static final String PUBSUB_PASSWORD_PROP = PUBSUB_PROP_PREFIX + ".password";
	private static final String PUBSUB_ROOT_PROP = PUBSUB_PROP_PREFIX + ".root";

	// For certificate based login
	private static final String PUBSUB_USECERTIFICATE_PROP = PUBSUB_PROP_PREFIX + ".usecertificate";
	private static final String PUBSUB_KEYSTOREPATH_PROP = PUBSUB_PROP_PREFIX + ".keystorepath";
	private static final String PUBSUB_KEYSTORETYPE_PROP = PUBSUB_PROP_PREFIX + ".keystoretype";
	private static final String PUBSUB_TRUSTSTOREPATH_PROP = PUBSUB_PROP_PREFIX + ".truststorepath";
	// Note truststore password would be read from the pubsub.password property when using certificates
	
	XMPPPubSub xps = null;

	static Logger logger;

	public ManifestPublisher(Logger l){
            logger = l;
     }

    /**
	 * prepare XMPP publisher object
	 * @param args
	 */
	public void prepareXMPP() throws Exception {

		String xmppServerPort = OrcaController.getProperty(PUBSUB_SERVER_PROP);
		String xmppLogin = OrcaController.getProperty(PUBSUB_LOGIN_PROP);
		String xmppPassword = OrcaController.getProperty(PUBSUB_PASSWORD_PROP);

		if ((xmppServerPort == null) ||
				(xmppLogin == null) ||
				(xmppPassword == null)) {
			throw new Exception("Missing properties (server:port, login and password must be specified)");
		}

		int port = Integer.parseInt(xmppServerPort.split(":")[1]);

		String xmppUseCertificate = OrcaController.getProperty(PUBSUB_USECERTIFICATE_PROP);
		if((xmppUseCertificate == null) || (xmppUseCertificate.equalsIgnoreCase("false"))) {
			xps = new XMPPPubSub(xmppServerPort.split(":")[0], port, xmppLogin, xmppPassword, logger, null);
		}
		else if((xmppUseCertificate.equalsIgnoreCase("true"))){

			String kspath = OrcaController.getProperty(PUBSUB_KEYSTOREPATH_PROP);
			String kstype = OrcaController.getProperty(PUBSUB_KEYSTORETYPE_PROP);
			String tspath = OrcaController.getProperty(PUBSUB_TRUSTSTOREPATH_PROP);
			// Remember xmppPassword == truststorepass (when using certificates)
			String tspass = xmppPassword;

			if((kspath == null) || (kstype == null) || (tspath == null)){
				throw new Exception("Missing keystore path, keystore type or truststore path for certificate based login");
			}

			xps = new XMPPPubSub(xmppServerPort.split(":")[0], port,
					xmppLogin, xmppPassword, kspath, kstype, tspath, tspass, logger, null);
			
			if (xps == null) 
				throw new Exception("Unable to create XMPP connection");
		}
		else {
			throw new Exception("Certificate usage property has to be specified as: pubsub.usecertificate=[true|false]");
		}
	}

	public void disconnectXMPP() throws Exception {
		try {
			if (xps != null)
				xps._finalize();
		} catch (Throwable ex) {
			throw new Exception("Error disconnecting from XMPP server: " + ex);
		}

	}

    /**
	 * prepare XMPP publisher object
	 * @param args
	 */
	public void createAccountAndDisconnect() throws Exception {

		logger.info("createAccountAndDisconnect(): Creating an account on XMPP server");
		String xmppServerPort = OrcaController.getProperty(PUBSUB_SERVER_PROP);
		String xmppLogin = OrcaController.getProperty(PUBSUB_LOGIN_PROP);
		String xmppPassword = OrcaController.getProperty(PUBSUB_PASSWORD_PROP);

		if ((xmppServerPort == null) ||
				(xmppLogin == null)) {
			throw new Exception("Missing properties (server:port and login must be specified)");
		}

		int port = Integer.parseInt(xmppServerPort.split(":")[1]);

		String defaultPassword = "defaultpass";
		XMPPPubSub xmppAcctCreation = new XMPPPubSub(xmppServerPort.split(":")[0], port, xmppLogin, defaultPassword, logger, null);

		xmppAcctCreation.createAccountAndDisconnect();
		logger.info("createAccountAndDisconnect(): Account created");
		
		try {
			xmppAcctCreation._finalize();
		} catch(Throwable ex) {
			throw new Exception("Error disconnecting from XMPP server " + ex);
		}
	}

	/**
	 * Main method to publish the manifest for a given actor and slice
	 * @param actor_id
	 * @param slice_id
	 * @param manifest
	 */

	public void publishManifest( String actor_id, String slice_id, String manifest ) {
		if (xps == null) { 
			logger.error("publishManifest(): XMPP object not initialized, skipping");
			return;
		}
		
		if (actor_id == null || slice_id == null || manifest == null){
			logger.error("publishManifest(): At least one of actor_id, slice_id, manifest is null; Can't publish manifest");
			return;
		}

		String pubsubRoot = OrcaController.getProperty(PUBSUB_ROOT_PROP); // for example ORCA.pubsub.root=orca/sm
		String node = "/" + pubsubRoot + "/" + actor_id + "/" + slice_id + "/" + "manifest" ;

		logger.info("publishManifest(): publishing manifest for node: " + node);

		String base64EncodedZippedManifest = orca.util.CompressEncode.compressEncode(manifest);

		xps.publishManifest(node, base64EncodedZippedManifest);
	}

	/**
	 * Main method to publish slicList for an SM actor
	 * @param actor_id
	 * @param slice_id
	 * @param manifest
	 */

	public void publishSliceList( String actor_id, String sliceListString )
	{
		if (xps == null) { 
			logger.error("publishSliceList(): XMPP object not initialized, skipping");
			return;
		}
		
		if(actor_id == null || sliceListString == null){
			logger.error("publishSliceList(): At least one of actor_id, sliceListString is null; Can't publish sliceList");
			return;
		}

		String pubsubRoot = OrcaController.getProperty(PUBSUB_ROOT_PROP); // for example ORCA.pubsub.root=orca/sm
		String node = "/" + pubsubRoot + "/" + actor_id + "/" + "sliceList" ;

		logger.info("publishSliceList(): publishing sliceList for: " + node);
		xps.publishSliceList(node, sliceListString);
	}


	public void expungeNode(String actor_id, String slice_id, String suffix){
		if (xps == null) { 
			logger.error("expungeNode(): XMPP object not initialized, skipping");
			return;
		}
		
		if (actor_id == null || slice_id == null || suffix == null){
			logger.error("expungeNode(): At least one of actor_id, slice_id, suffix is null; Can't expunge xmpp pubsub node");
			return;
		}

		String pubsubRoot = OrcaController.getProperty(PUBSUB_ROOT_PROP); // for example ORCA.pubsub.root=orca/sm
		String node = "/" + pubsubRoot + "/" + actor_id + "/" + slice_id + "/" + suffix ;

		xps.deleteNode(node);
	}

	public void expungeNode(String nodePath){

		if (xps == null) { 
			logger.error("expungeNode(): XMPP object not initialized, skipping");
			return;
		}
		
		if(nodePath == null){
			logger.error("expungeNode(): nodePath is null; Can't expunge xmpp pubsub node");
			return;
		}
		
		String pubsubRoot = OrcaController.getProperty(PUBSUB_ROOT_PROP); // for example ORCA.pubsub.root=orca/sm
		String node = "/" + pubsubRoot + "/" + nodePath ;

		xps.deleteNode(node);
	}
}
