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
        // Note truststore password would be read from the IMF.pubsub.password property when using certificates

	static Logger logger;

	public ManifestPublisher(Logger l){
            logger = l;
        }

        /**
	 * prepare XMPP publisher object
	 * @param args
	 */
	static XMPPPubSub prepareXMPP(Properties orcaPubsubProps) {

            String xmppServerPort = orcaPubsubProps.getProperty(PUBSUB_SERVER_PROP);
            String xmppLogin = orcaPubsubProps.getProperty(PUBSUB_LOGIN_PROP);
            String xmppPassword = orcaPubsubProps.getProperty(PUBSUB_PASSWORD_PROP);

            XMPPPubSub xps = null;

            if ((xmppServerPort == null) ||
                            (xmppLogin == null) ||
                            (xmppPassword == null)) {
                    logger.error("Missing IMF.pubsub properties (server:port, login and password must be specified)");
                    return null;
            }

            int port = Integer.parseInt(xmppServerPort.split(":")[1]);

            String xmppUseCertificate = orcaPubsubProps.getProperty(PUBSUB_USECERTIFICATE_PROP);
            if((xmppUseCertificate == null) || (xmppUseCertificate.equalsIgnoreCase("false"))) {
                xps = new XMPPPubSub(xmppServerPort.split(":")[0], port, xmppLogin, xmppPassword, logger, null);
            }
            else if((xmppUseCertificate.equalsIgnoreCase("true"))){

                String kspath = orcaPubsubProps.getProperty(PUBSUB_KEYSTOREPATH_PROP);
                String kstype = orcaPubsubProps.getProperty(PUBSUB_KEYSTORETYPE_PROP);
                String tspath = orcaPubsubProps.getProperty(PUBSUB_TRUSTSTOREPATH_PROP);
                // Remember xmppPassword == truststorepass (when using certificates)
                String tspass = xmppPassword;

                if((kspath == null) || (kstype == null) || (tspath == null)){
                    logger.error("Missing keystore path, keystore type or truststore path for certificate based login");
                    logger.error("Specify IMF.pubsub.keystorepath , IMF.pubsub.keystoretype and IMF.pubsub.truststorepath properties in measurement.properties");
                    return null;
                }

                xps = new XMPPPubSub(xmppServerPort.split(":")[0], port,
                                xmppLogin, xmppPassword, kspath, kstype, tspath, tspass, logger, null);
            }
            else {
                logger.info("Certificate usage property has to be specified as: IMF.pubsub.usecertificate=[true|false]");
                return null;
            }

            return xps;

	}


        /**
	 * prepare XMPP publisher object
	 * @param args
	 */
	static XMPPPubSub prepareXMPPForAcctCreation(Properties orcaPubsubProps) {

            String xmppServerPort = orcaPubsubProps.getProperty(PUBSUB_SERVER_PROP);
            String xmppLogin = orcaPubsubProps.getProperty(PUBSUB_LOGIN_PROP);
            String xmppPassword = orcaPubsubProps.getProperty(PUBSUB_PASSWORD_PROP);

            XMPPPubSub xps = null;

            if ((xmppServerPort == null) ||
                            (xmppLogin == null)) {
                    logger.error("Missing IMF.pubsub properties (server:port and login must be specified)");
                    return null;
            }

            int port = Integer.parseInt(xmppServerPort.split(":")[1]);

            String defaultPassword = "defaultpass";
            xps = new XMPPPubSub(xmppServerPort.split(":")[0], port, xmppLogin, defaultPassword, logger, null);

            return xps;

	}

        /**
         * Main method to publish the manifest for a given actor and slice
         * @param actor_id
         * @param slice_id
         * @param manifest
         */

        public void publishManifest( String actor_id, String slice_id, String manifest )
        {

            if(actor_id == null || slice_id == null || manifest == null){
                logger.info("At least one of actor_id, slice_id, manifest is null; Can't publish manifest");
                return;
            }

            logger.info("Loading orca pubsub properties");

            Properties orcaPubsubProps = new Properties();
            File f = new File(OrcaController.ControllerConfigurationFile);
            if (f.exists()) {
                try {
                    logger.info("Successfully loaded orcapubsub.properties");
                    orcaPubsubProps.load(new FileInputStream(f));
                } catch (IOException ex) {
                    logger.info("Error loading orcapubsub properties file; Can't publish manifest");
                    return;
                }
            } else {
                    logger.info("No orcapubsub properties; Can't publish manifest");
                    return;
            }
            
            if (orcaPubsubProps == null) {
                    logger.error("Unable to load properties file. Make sure orcapubsub properties is at $ORCA_HOME/config; Can't publish manifest");
                    return;
            }
            
            // create new account if required
            logger.info("Creating XMPP connection for new account creation");
            XMPPPubSub xmppAcctCreation = prepareXMPPForAcctCreation(orcaPubsubProps);
            if (xmppAcctCreation == null) {
                    logger.info("Unable to create XMPP object for creating new accounts; Can't publish manifest");
                    return;
            }
            xmppAcctCreation.createAccountAndDisconnect();

            // create a pubsub object
            logger.info("Creating XMPP connection");
            XMPPPubSub xmpp = prepareXMPP(orcaPubsubProps);
            if (xmpp == null) {
                    logger.error("Unable to create XMPPPublisher object ; Can't publish manifest");
                    return;
            }

            String pubsubRoot = orcaPubsubProps.getProperty(PUBSUB_ROOT_PROP); // for example ORCA.pubsub.root=orca/sm
            String node = "/" + pubsubRoot + "/" + actor_id + "/" + slice_id + "/" + "manifest" ;

            logger.info("ManifestPublisher: publishing manifest for node: " + node);
            
            String base64EncodedZippedManifest = orca.util.CompressEncode.compressEncode(manifest);
            
            xmpp.publishManifest(node, base64EncodedZippedManifest);

            try {
                xmpp._finalize();
            } catch (Throwable ex) {
                logger.error("Error disconnecting from xmpp server");
            }

        }

        /**
         * Main method to publish slicList for an SM actor
         * @param actor_id
         * @param slice_id
         * @param manifest
         */

        public void publishSliceList( String actor_id, String sliceListString )
        {

            if(actor_id == null || sliceListString == null){
                logger.info("At least one of actor_id, sliceListString is null; Can't publish sliceList");
                return;
            }

            logger.info("Loading orca pubsub properties");

            Properties orcaPubsubProps = new Properties();
            File f = new File(OrcaController.ControllerConfigurationFile);
            if (f.exists()) {
                try {
                    logger.info("Successfully loaded orcapubsub.properties");
                    orcaPubsubProps.load(new FileInputStream(f));
                } catch (IOException ex) {
                    logger.info("Error loading orcapubsub properties file; Can't publish sliceList");
                    return;
                }
            } else {
                    logger.info("No orcapubsub properties; Can't publish sliceList");
                    return;
            }

            if (orcaPubsubProps == null) {
                    logger.error("Unable to load properties file. Make sure orcapubsub properties is at $ORCA_HOME/config; Can't publish sliceList");
                    return;
            }
            
            // create new account if required
            logger.info("Creating XMPP connection for new account creation");
            XMPPPubSub xmppAcctCreation = prepareXMPPForAcctCreation(orcaPubsubProps);
            if (xmppAcctCreation == null) {
                    logger.info("Unable to create XMPP object for creating new accounts; Can't publish sliceList");
                    return;
            }
            xmppAcctCreation.createAccountAndDisconnect();

            // create a pubsub object
            logger.info("Creating XMPP connection");
            XMPPPubSub xmpp = prepareXMPP(orcaPubsubProps);
            if (xmpp == null) {
                    logger.error("Unable to create XMPPPublisher object ; Can't publish sliceList");
                    return;
            }

            String pubsubRoot = orcaPubsubProps.getProperty(PUBSUB_ROOT_PROP); // for example ORCA.pubsub.root=orca/sm
            String node = "/" + pubsubRoot + "/" + actor_id + "/" + "sliceList" ;

            logger.info("ManifestPublisher: publishing sliceList for: " + node);
            xmpp.publishSliceList(node, sliceListString);

            try {
                xmpp._finalize();
            } catch (Throwable ex) {
                logger.error("Error disconnecting from xmpp server");
            }

        }


        public void expungeNode(String actor_id, String slice_id, String suffix){

            if(actor_id == null || slice_id == null || suffix == null){
                logger.info("At least one of actor_id, slice_id, suffix is null; Can't expunge xmpp pubsub node");
                return;
            }

            logger.info("Loading orca pubsub properties");

            Properties orcaPubsubProps = new Properties();
            File f = new File(OrcaController.ControllerConfigurationFile);
            if (f.exists()) {
                try {
                    logger.info("Successfully loaded orcapubsub.properties");
                    orcaPubsubProps.load(new FileInputStream(f));
                } catch (IOException ex) {
                    logger.info("Error loading orcapubsub properties file; Can't expunge xmpp pubsub node");
                    return;
                }
            } else {
                    logger.info("No orcapubsub properties; Can't expunge xmpp pubsub node");
                    return;
            }

            if (orcaPubsubProps == null) {
                    logger.error("Unable to load properties file. Make sure orcapubsub properties is at $ORCA_HOME/config; Can't expunge xmpp pubsub node");
                    return;
            }
            
            // create new account if required
            logger.info("Creating XMPP connection for new account creation");
            XMPPPubSub xmppAcctCreation = prepareXMPPForAcctCreation(orcaPubsubProps);
            if (xmppAcctCreation == null) {
                    logger.info("Unable to create XMPP object for creating new accounts; Can't expunge xmpp pubsub node");
                    return;
            }
            xmppAcctCreation.createAccountAndDisconnect();

            // create a pubsub object
            logger.info("Creating XMPP connection");
            XMPPPubSub xmpp = prepareXMPP(orcaPubsubProps);
            if (xmpp == null) {
                    logger.error("Unable to create XMPPPublisher object ; Can't expunge xmpp pubsub node");
                    return;
            }

            String pubsubRoot = orcaPubsubProps.getProperty(PUBSUB_ROOT_PROP); // for example ORCA.pubsub.root=orca/sm
            String node = "/" + pubsubRoot + "/" + actor_id + "/" + slice_id + "/" + suffix ;

            xmpp.deleteNode(node);

            try {
                xmpp._finalize();
            } catch (Throwable ex) {
                logger.error("Error disconnecting from xmpp server");
            }


        }

        public void expungeNode(String nodePath){

            if(nodePath == null){
                logger.info("nodePath is null; Can't expunge xmpp pubsub node");
                return;
            }

            logger.info("Loading orca pubsub properties");

            Properties orcaPubsubProps = new Properties();
            File f = new File(OrcaController.ControllerConfigurationFile);
            if (f.exists()) {
                try {
                    logger.info("Successfully loaded orcapubsub.properties");
                    orcaPubsubProps.load(new FileInputStream(f));
                } catch (IOException ex) {
                    logger.info("Error loading orcapubsub properties file; Can't expunge xmpp pubsub node");
                    return;
                }
            } else {
                    logger.info("No orcapubsub properties; Can't expunge xmpp pubsub node");
                    return;
            }

            if (orcaPubsubProps == null) {
                    logger.error("Unable to load properties file. Make sure orcapubsub properties is at $ORCA_HOME/config; Can't expunge xmpp pubsub node");
                    return;
            }
            
            // create new account if required
            logger.info("Creating XMPP connection for new account creation");
            XMPPPubSub xmppAcctCreation = prepareXMPPForAcctCreation(orcaPubsubProps);
            if (xmppAcctCreation == null) {
                    logger.info("Unable to create XMPP object for creating new accounts; Can't expunge xmpp pubsub node");
                    return;
            }
            xmppAcctCreation.createAccountAndDisconnect();

            // create a pubsub object
            logger.info("Creating XMPP connection");
            XMPPPubSub xmpp = prepareXMPP(orcaPubsubProps);
            if (xmpp == null) {
                    logger.error("Unable to create XMPPPublisher object ; Can't expunge xmpp pubsub node");
                    return;
            }

            String pubsubRoot = orcaPubsubProps.getProperty(PUBSUB_ROOT_PROP); // for example ORCA.pubsub.root=orca/sm
            String node = "/" + pubsubRoot + "/" + nodePath ;

            xmpp.deleteNode(node);

            try {
                xmpp._finalize();
            } catch (Throwable ex) {
                logger.error("Error disconnecting from xmpp server");
            }


        }



}
