package net.exogeni.orca.handlers.network.router;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.exogeni.orca.handlers.network.core.CommandException;
import net.exogeni.orca.handlers.network.core.INetworkDevice;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public abstract class JunosSSLDevice implements INetworkDevice, IJunosInteractor {
    protected Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
    protected String name = "Juniper SSL device";

    protected String basepath;
    protected boolean isEmulation = false;

    protected String deviceAddress;
    protected String uid;
    protected String password;

    private SSLContext sc;
    private SSLSocket sslSocket;
    private boolean validateCert = false;

    protected BufferedWriter bw;
    protected String junosRelease;
    protected XMLReader reader;
    protected InputSource readerInputSource;

    private static final int JunosXMLSSLPort = 3220;
    private static final String JunosDefaultRelease = "10.0S3";

    // Create an X.509 trust manager that does not validate certificate chains
    // to provide the option when connecting to Juniper devices
    static protected TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
    } };

    /**
     * 
     * @param deviceAddress deviceAddress
     * @param uid uid
     * @param password password
     */
    public JunosSSLDevice(String deviceAddress, String uid, String password) {
        this.deviceAddress = deviceAddress;
        this.uid = uid;
        this.password = password;
        this.junosRelease = JunosDefaultRelease;
    }

    public void connect() throws CommandException {
        if (isEmulationEnabled())
            return;
        try {
            SSLSocketFactory sslSF;
            if (!validateCert) {
                // create new SSL context
                sc = SSLContext.getInstance("SSL");
                // make sure it uses a trusting trust manager
                sc.init(null, trustAllCerts, null);
                // get ssl socket factory from the SSL context
                sslSF = sc.getSocketFactory();
            } else
                sslSF = (SSLSocketFactory) SSLSocketFactory.getDefault();

            sslSocket = (SSLSocket) sslSF.createSocket(deviceAddress, JunosXMLSSLPort);

            // create buffered writer for the socket
            bw = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));

            // create a parser, attach to the input stream
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(new JunosResponseHandler(this));
            readerInputSource = new InputSource(sslSocket.getInputStream());
        } catch (Exception e) {
            throw new CommandException("Unable to connect to device");
        }
    }

    public void execute() throws CommandException {
        try {
            reader.parse(readerInputSource);
        } catch (IOException e) {
            logger.warn("IO Exception while parsing Junos response: " + e);
        } catch (SAXException e) {
            throw new CommandException("Unable to parse system response: " + e);
        }
    }

    public void disconnect() {
        if (isEmulationEnabled())
            return;
        try {
            if (sslSocket != null)
                sslSocket.close();
        } catch (Exception e) {
            ;
        }
        sslSocket = null;
    }

    public boolean isConnected() {
        return !(sslSocket == null);
    }

    @Override
    protected void finalize() throws Throwable {
        if (isConnected()) {
            disconnect();
        }
        super.finalize();
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

    // enable validation of the device cert (requires moving OpenSSL cert into java keystore)
    public void enableCertValidation() {
        validateCert = true;
    }

    protected void sendToDevice(String toSend) {
        try {
            bw.write(toSend);
            bw.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unable to send string " + toSend + " to Junos device");
        }
    }

    // IJunosInteractor methods
    public void sendHandshakeAndLogin() {
        String hostname = "unknown";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException e) {
        }
        sendToDevice("<?xml version=\"1.0\" encoding=\"us-ascii\"?>");
        sendToDevice("<junoscript version=\"1.0\" hostname=\"" + hostname + "\" release=\"" + junosRelease + "\">");
        sendToDevice("<rpc> <request-login> <username>" + uid + "</username> <challenge-response>" + password
                + "</challenge-response> </request-login> </rpc>");
    }

    public void sendCommit() {
        sendToDevice("<rpc><commit-configuration/></rpc>");
    }

    public void sendEndSessionRequest() {
        sendToDevice("<rpc><request-end-session/></rpc>");
    }

    public void sendCloseJunoscript() {
        sendToDevice("</junoscript>");
    }

    public void sendConfigurationUpdate() {
        sendToDevice("<rpc> <load-configuration action=\"merge\" format=\"text\"> <configuration-text>"
                + getConfigurationText() + "</configuration-text></load-configuration></rpc>");
    }

    protected abstract String getConfigurationText();

    public void setJunosRelease(String rel) {
        junosRelease = rel;
    }
}
