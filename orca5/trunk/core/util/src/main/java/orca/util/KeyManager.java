/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;


/**
 * Utility class for performing common key management operations.
 */
public class KeyManager
{
    /**
     * Default algorithm.
     */
    private static final String DEFAULT_ALGORITHM = "DSA";

    /**
     * Default key size.
     */
    private static final int DEFAULT_KEY_SIZE = 1024;

    /**
     * Op code: error.
     */
    private static final int OP_ERROR = 0;

    /**
     * Op code: create.
     */
    private static final int OP_CREATE = 1;

    /**
     * Op code: export from certificate.
     */
    private static final int OP_EXPORT_FROM_CERT = 2;

    /**
     * Op code: export from keystore.
     */
    private static final int OP_EXPORT_FROM_STORE = 3;

    /**
     * Main entrypoint.
     *
     * @param args arguments
     */
    public static void main(final String[] args)
    {
        try {
            int op = validateInput(args);

            if (op == OP_ERROR) {
                Usage();

                return;
            }

            KeyManager man = new KeyManager();

            switch (op) {
                case OP_CREATE:
                    man.generateKeyPair(args[1]);

                    break;

                case OP_EXPORT_FROM_CERT:
                    man.exportCertificatePublicKey(args[1], args[2]);

                    break;

                case OP_EXPORT_FROM_STORE:
                    man.exportPublicKeyFromStore(args[1], args[2], args[3], args[4], args[5]);

                    break;

                default:
                    Usage();

                    break;
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Prints the usage message.
     */
    private static void Usage()
    {
        System.out.println(
            "-create fileName                                   Creates a Public/Private DSA key pair of size 1024 bits");
        System.out.println(
            "-exportfromcert certFileName publicKeyFileName     Exports the public key stored in the certificte");
        System.out.println(
            "-exportfromstore out store storepass alias keypass Exports the privateKey stored in the keystore");
    }

    /**
     * Validates the input
     *
     * @param args arguments
     *
     * @return operation code
     */
    private static int validateInput(final String[] args)
    {
        if (args.length == 0) {
            return OP_ERROR;
        }

        try {
            if (args[0].equals("-create")) {
                if (args.length < 2) {
                    return OP_ERROR;
                }

                return OP_CREATE;
            }

            if (args[0].equals("-exportfromcert")) {
                if (args.length < 3) {
                    return OP_ERROR;
                }

                return OP_EXPORT_FROM_CERT;
            }

            if (args[0].equals("-exportfromstore")) {
                if (args.length < 6) {
                    return OP_ERROR;
                }

                return OP_EXPORT_FROM_STORE;
            }

            return OP_ERROR;
        } catch (Exception e) {
            return OP_ERROR;
        }
    }

    /**
     * Exports a public key from a certificate.
     *
     * @param certificateFile certificate file
     * @param outputFile public key file
     *
     * @throws Exception
     */
    public void exportCertificatePublicKey(final String certificateFile, final String outputFile)
                                    throws Exception
    {
        // Note: ASSUMES A DSA key
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        X509Certificate cert = (X509Certificate) cf.generateCertificate(
            new FileInputStream(certificateFile));

        exportKey(cert.getPublicKey(), outputFile);
        System.out.println("Successfully exported the public key.");
    }

    /**
     * Exports the key to the file.
     *
     * @param key key
     * @param fileName file
     *
     * @throws Exception
     */
    public void exportKey(final Key key, final String fileName) throws Exception
    {
        FileOutputStream stream = new FileOutputStream(fileName);
        stream.write(key.getEncoded());
        stream.close();
    }

    /**
     * Exports a public key from a java keystore.
     *
     * @param outputFile output file
     * @param keyStore path to the keystore file
     * @param ksPass keystore password
     * @param alias key alias
     * @param keyPass key password
     *
     * @throws Exception
     */
    public void exportPublicKeyFromStore(final String outputFile, final String keyStore,
                                         final String ksPass, final String alias,
                                         final String keyPass) throws Exception
    {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new FileInputStream(keyStore), ksPass.toCharArray());
        exportKey(store.getKey(alias, keyPass.toCharArray()), outputFile);
        System.out.println("Private key successfully exported");
    }

    /**
     * Generates a new key pair to be exported to the specified file.
     * Two files will be created. The public key will have suffix ".pub" and
     * the private key will have suffix ".priv".
     *
     * @param fileName file name
     *
     * @throws Exception
     */
    public void generateKeyPair(final String fileName) throws Exception
    {
        generateKeyPair(DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE, fileName);
    }

    /**
     * Generates key pair.
     *
     * @param algorithm algorithm to use
     * @param keySize key size
     *
     * @return a key pair
     *
     * @throws Exception
     */
    public KeyPair generateKeyPair(final String algorithm, final int keySize)
                            throws Exception
    {
        KeyPairGenerator kg = KeyPairGenerator.getInstance(algorithm);
        kg.initialize(keySize);

        KeyPair kp = kg.genKeyPair();

        return kp;
    }

    /**
     * Generates a key pair and exports the keys to files with the
     * given prefix. The public key will have suffix ".pub" and the private
     * key will have suffix ".priv".
     *
     * @param algorithm algorithm to use
     * @param keySize key size
     * @param fileName file name (prefix)
     *
     * @throws Exception
     */
    public void generateKeyPair(final String algorithm, final int keySize, final String fileName)
                         throws Exception
    {
        KeyPair kp = generateKeyPair(algorithm, keySize);

        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        exportKey(publicKey, fileName + ".pub");
        exportKey(privateKey, fileName + ".priv");

        System.out.println("Key pair successfully generated.");
    }

    /**
     * Imports a private key from a file.
     *
     * @param fileName file
     *
     * @return private key
     *
     * @throws Exception
     */
    public PrivateKey importPrivateKeyFromFile(final String fileName) throws Exception
    {
        byte[] encoded = readKeyFromFile(fileName);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encoded);

        // XXX: This needs to be automated. Possibily change the format of
        // storing the private key so that it says what algorithm to use
        KeyFactory keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM);

        return keyFactory.generatePrivate(privateKeySpec);
    }

    /**
     * Imports a public key from a file.
     *
     * @param fileName file
     *
     * @return public key
     *
     * @throws Exception
     */
    public PublicKey importPublicKeyFromFile(final String fileName) throws Exception
    {
        byte[] encoded = readKeyFromFile(fileName);

        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);

        // XXX: This needs to be automated. Possibily change the format of
        // storing the private key so that it says what algorithm to use
        KeyFactory keyFactory = KeyFactory.getInstance(DEFAULT_ALGORITHM);

        return keyFactory.generatePublic(publicKeySpec);
    }

    /**
     * Reads a key from the file.
     *
     * @param fileName file to read from
     *
     * @return key
     *
     * @throws Exception
     */
    private byte[] readKeyFromFile(final String fileName) throws Exception
    {
        File file = new File(fileName);
        FileInputStream stream = new FileInputStream(file);

        byte[] buffer = new byte[(int) file.length()];
        stream.read(buffer);
        stream.close();

        return buffer;
    }
}