/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.nodeagent.client;

import orca.nodeagent.KeyMasterMessage;
import orca.nodeagent.NodeAgentServiceStub;
import orca.nodeagent.documents.GetServiceKeyElement;
import orca.nodeagent.documents.GetServiceKeyResultElement;
import orca.nodeagent.documents.RegisterAuthorityKeyElement;
import orca.nodeagent.documents.RegisterAuthorityKeyResultElement;

import org.apache.axis2.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyMasterClient {
    String clientPassword;
    char[] clientPasswordChar;
    String clientStorePassword;
    char[] clientStorePasswordChar;
    NodeAgentServiceStub stub;
    String location;
    String keyStoreLocation;
    String authorityIP;
    String keyAlias;
    String serverKey;

    public KeyMasterClient(String location, NodeAgentServiceStub stub, String keyStoreLocation, String keyStorePass, String keyAlias, String keyPass, String authorityIP, String serverKey) {
        this.location = location;
        this.stub = stub;
        this.keyStoreLocation = keyStoreLocation;
        this.authorityIP = authorityIP;
        this.clientStorePassword = keyStorePass;
        this.keyAlias = keyAlias;
        this.clientPassword = keyPass;
        this.serverKey = serverKey;

        clientPasswordChar = this.clientPassword.toCharArray();
        clientStorePasswordChar = this.clientStorePassword.toCharArray();
    }

    /**
     * Calls the registerAuthorityKey function (plays the KeyMaster protocol).
     * @return
     * @throws Exception
     */
    public int callRegisterAuthorityKey() throws Exception {
        // load the Authority Key
        KeyStore ks = KeyStore.getInstance("JKS");
        // System.out.println("keystore location: " + keyStoreLocation);

        FileInputStream fis = new FileInputStream(keyStoreLocation);
        ks.load(fis, clientStorePasswordChar);
        fis.close();

        Certificate cert = ks.getCertificate(keyAlias);

        byte[] certEncoding = cert.getEncoded();
        String alias = "authoritykey";

        // we send a message containing the Authority certificate, Authority
        // alias
        // and <NodeIP,NodeToken,Timestamp,AuthorityIP> signed by the authority
        // certificate
        RegisterAuthorityKeyElement rke = new RegisterAuthorityKeyElement();
        rke.setAlias(alias); // set alias
        rke.setCertificate(certEncoding); // set Authority certificate

        // create an xml document containing nodeIP, timestamp and authorityIP
        byte[] messageByte = createKeyMasterMessage();

        rke.setRequest(messageByte); // set message to send

        //printKeyMasterMessage(messageByte); // print the message to be sent

        // compute the signature over the message sent
        Signature sig = Signature.getInstance("MD5withRSA");
        PrivateKey privateKey = (PrivateKey) ks.getKey(keyAlias, clientPasswordChar);

        if (privateKey == null) {
            System.out.println("private key is null");
        }

        sig.initSign(privateKey);
        sig.update(messageByte);

        byte[] signature = sig.sign();

        rke.setSignature(signature); // set signature

        // make the call to register the Authority Key
        RegisterAuthorityKeyResultElement rkre = stub.registerAuthorityKey(rke);

        int res = rkre.getCode();

        if (res != 0) {
            return res;
        }

        // decrypt the simetric key (the simetric key encrypts the NodeAgent
        // reply message)
        // the simetric key is encrypted with the Authority public key
        byte[] encodedBytes = rkre.getKey();

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, ks.getKey(keyAlias, clientPasswordChar));

        byte[] decryptBytes = cipher.doFinal(encodedBytes);
        String sharedKeyBase64 = new String(decryptBytes);

        byte[] sharedKeyBytes = Base64.decode(sharedKeyBase64);
        SecretKey sharedKey = new SecretKeySpec(sharedKeyBytes, "TripleDES");

        // decrypt the NodeAgent reply message
        byte[] encodedBytesDoc = rkre.getResponse();

        Cipher cipherSym = Cipher.getInstance("TripleDES");
        cipherSym.init(Cipher.DECRYPT_MODE, sharedKey);

        byte[] decryptBytesDoc = cipherSym.doFinal(encodedBytesDoc);

        // System.out.println("NodeAgent reply message:");
        // printKeyMasterMessage(decryptBytesDoc);
        KeyMasterMessage kmm = createKeyMasterReplyMessage(decryptBytesDoc);

        // install the NodeAgent key in the authority keystore
        String certBase64 = kmm.getNode("nodecertificate");
        byte[] certBase64Bytes = Base64.decode(certBase64);
        Certificate nodeCert = generateCertificate(certBase64Bytes);
        ks.setCertificateEntry(serverKey, nodeCert);

        FileOutputStream fos = new FileOutputStream(keyStoreLocation);
        ks.store(fos, clientStorePasswordChar);
        fos.close();

        // the token has to be verified in order to authenticate the NodeAgent
        // (the token is a shared secret)
        String nodeToken = kmm.getNode("nodetoken");

        // System.out.println("NodeToken: " + nodeToken);
        return res;
    }

    /**
     * Calls the getServiceKey function (used for recovery).
     * @return
     * @throws Exception
     */
    public int callGetServiceKey() throws Exception {
        GetServiceKeyElement gske = new GetServiceKeyElement();

        // call the getServiceKey
        GetServiceKeyResultElement gskre = stub.getServiceKey(gske);

        int retCode = gskre.getCode();

        if (retCode != 0) {
            // System.out.println("getServiceKey call failed");
            // System.out.println("Error Code: "+retCode);
            return retCode;
        }

        // get Service Certificate
        if (retCode == 0) {
            // load the keystore
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(keyStoreLocation);
            ks.load(fis, clientStorePasswordChar);
            fis.close();

            byte[] encodedCert = gskre.getKey();
            Certificate cert = generateCertificate(encodedCert);
            Certificate servCert = ks.getCertificate(serverKey);

            if (cert.equals(servCert) == true) {
                return retCode;
            } else {
                return 1;
            }
        }

        return retCode;
    }

    /**
     * Creates the message to be sent to the KeyMaster.
     * @return
     */
    private byte[] createKeyMasterMessage() {
        // create an xml document containing "nodeIP", "timestamp",
        // "authorityIP" as tags
        KeyMasterMessage km = new KeyMasterMessage();
        String[] nodes1 = { "nodeIP", "timestamp", "authorityIP" };
        km.createXMLDocument(nodes1);

        String nodeIP = authorityIP;

        if ((nodeIP == null) || (nodeIP.length() == 0)) {
            InetAddress addr = null;

            try {
                addr = InetAddress.getLocalHost();
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }

            nodeIP = addr.getHostAddress();
        }

        km.setNode("nodeIP", nodeIP);
        km.setNode("authorityIP", authorityIP);

        // timestamp option is not implemented
        km.setNode("timestamp", "NA");

        byte[] messageByte = km.getBytes();

        return messageByte;
    }

    /**
     * Creates a KeyMasterMessage object from the KeyMaster reply.
     * @param decryptBytesDoc
     * @return
     */
    private KeyMasterMessage createKeyMasterReplyMessage(byte[] decryptBytesDoc) {
        KeyMasterMessage kmm = new KeyMasterMessage();
        kmm.buildXMLDocument(new String(decryptBytesDoc));

        return kmm;
    }

    /**
     * Prints a KeyMasterMessage represented in byte[].
     * @param messageByte
     */
    private void printKeyMasterMessage(byte[] messageByte) {
        String str = new String(messageByte);
        System.out.println(str);
    }

    public int run() throws Exception {
        // register Authority Key
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream(keyStoreLocation);
        ks.load(fis, clientStorePasswordChar);
        fis.close();

        Certificate cert = ks.getCertificate(keyAlias);

        // System.out.println(cert);
        PublicKey pkey = cert.getPublicKey();
        cert.verify(pkey);

        byte[] encCertInfo = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(encCertInfo);

        StringBuffer strBufDigest = new StringBuffer();
        int length = digest.length;

        for (int n = 0; n < length; n++) {
            strBufDigest.append(digest[n]);
        }

        // System.out.println(toHexString(digest));
        byte[] certEncoding = cert.getEncoded();
        String alias = "authoritykey";

        RegisterAuthorityKeyElement rke = new RegisterAuthorityKeyElement();

        rke.setAlias(alias);

        rke.setCertificate(certEncoding);

        KeyMasterMessage km = new KeyMasterMessage();
        String[] nodes1 = { "nodeIP", "timestamp", "authorityIP" };
        km.createXMLDocument(nodes1);

        InetAddress addr = InetAddress.getLocalHost();

        // Get IP Address
        String nodeIP = addr.getHostAddress();
        km.setNode("nodeIP", nodeIP);
        km.setNode("authorityIP", authorityIP);
        km.setNode("timestamp", "2007:06:16");

        byte[] messageByte = km.getBytes();

        rke.setRequest(messageByte);

        String str = new String(messageByte);
        KeyMasterMessage km1 = new KeyMasterMessage();
        km1.buildXMLDocument(str);

        String msgNodeIP = km1.getNode("nodeIP");

        String msgSSHPublicKey = km1.getNode("sshpublickey");

        String msgAuthIP = km1.getNode("authorityIP");

        String msgTimestamp = km1.getNode("timestamp");

        // System.out.println(msgNodeIP + " " + msgAuthIP + " " + msgTimestamp);
        Signature sig = Signature.getInstance("MD5withRSA");
        PrivateKey privateKey = (PrivateKey) ks.getKey(keyAlias, clientPasswordChar);
        sig.initSign(privateKey);
        sig.update(messageByte);

        byte[] signature = sig.sign();

        rke.setSignature(signature);

        RegisterAuthorityKeyResultElement rkre = stub.registerAuthorityKey(rke);

        int res = rkre.getCode();

        if (res != 0) {
            // System.out.println("First Key Registration failed");
            // System.out.println("exitCode: " + res);
            return res;
        }

        byte[] encodedBytes = rkre.getKey();

        // decrypt the simetric key (the simetric key encrypts the NodeAgent
        // reply message)
        // the simetric key is encrypted with the controller/this entity public
        // key
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, ks.getKey(keyAlias, clientPasswordChar));

        byte[] decryptBytes = cipher.doFinal(encodedBytes);
        String sharedKeyBase64 = new String(decryptBytes);

        byte[] sharedKeyBytes = Base64.decode(sharedKeyBase64);
        SecretKey sharedKey = new SecretKeySpec(sharedKeyBytes, "TripleDES");

        // decrypt the NodeAgent reply message
        byte[] encodedBytesDoc = rkre.getResponse();

        Cipher cipherSym = Cipher.getInstance("TripleDES");
        cipherSym.init(Cipher.DECRYPT_MODE, sharedKey);

        byte[] decryptBytesDoc = cipherSym.doFinal(encodedBytesDoc);

        String strDoc = new String(decryptBytesDoc);

        // System.out.println("NodeAgent reply message:");
        // System.out.println(strDoc);
        KeyMasterMessage kmm = new KeyMasterMessage();
        kmm.buildXMLDocument(strDoc);

        // install the NodeAgent key
        String certBase64 = kmm.getNode("nodecertificate");
        byte[] certBase64Bytes = Base64.decode(certBase64);
        Certificate nodeCert = generateCertificate(certBase64Bytes);
        ks.setCertificateEntry(serverKey, nodeCert);

        FileOutputStream fos = new FileOutputStream(keyStoreLocation);
        ks.store(fos, clientStorePasswordChar);
        fos.close();

        // the token has to be verified in order to authenticate the NodeAgent
        // (the token is a shared secret)
        String nodeToken = kmm.getNode("nodetoken");

        // System.out.println("NodeToken: " + nodeToken);
        return res;
    }

    /**
     * Helper function used to compute the hexadecimal representation of a
     * digest.
     * @param digest
     * @return
     */
    private String toHexString(byte[] digest) {
        char[] hexValues = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        StringBuffer stringBuf = new StringBuffer();
        int len = digest.length;

        for (int i = 0; i < len; i++) {
            int first = ((digest[i] & 0xf0) >> 4);
            int second = (digest[i] & 0x0f);
            stringBuf.append(hexValues[first]);
            stringBuf.append(hexValues[second]);

            if (i != (len - 1)) {
                stringBuf.append(":");
            }
        }

        return stringBuf.toString();
    }

    /**
     * Generates a certificate from byte encoding.
     * @param certificateEncoding
     * @return
     */
    private Certificate generateCertificate(byte[] certificateEncoding) {
        ByteArrayInputStream inStream = new ByteArrayInputStream(certificateEncoding);

        Certificate cert = null;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = cf.generateCertificate(inStream);

            while (inStream.available() > 0) {
                cert = cf.generateCertificate(inStream);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return cert;
    }
}
