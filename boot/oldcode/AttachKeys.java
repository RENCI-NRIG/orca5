package orca.boot.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;

import java.security.*;

/**
 * This class is used to preprocess a shirako configuration file and attach key pairs to each communication link.
 * 
 * @author aydan
 */
public class AttachKeys {
    private static final String DEFAULT_ALGORITHM = "DSA";
    private static final int DEFAULT_KEY_SIZE = 1024;

    private String algorithm;
    private int keySize;

    private String inputFile;
    private String outputFile;

    private KeyPairGenerator kg = null;
    private JAXBContext jc = null;
    private ObjectFactory factory = null;

    private Configuration config;

    public AttachKeys(String inputFile, String outputFile) {
        this(inputFile, outputFile, DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE);
    }

    public AttachKeys(String inputFile, String outputFile, String algorithm, int keySize) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.algorithm = algorithm;
        this.keySize = keySize;
    }

    private void initialize() throws Exception {
        // initialize the key pair generator
        kg = KeyPairGenerator.getInstance(algorithm);
        kg.initialize(keySize);
        // initialize the jaxb context
        jc = JAXBContext.newInstance("boot.beans");
        factory = new ObjectFactory();
    }

    private Object[] generateKeyPair() throws Exception {
        KeyPair kp = kg.generateKeyPair();

        CryptoKey publicKey = factory.createCryptoKey();
        publicKey.setAlgorithm(kg.getAlgorithm());
        publicKey.setValue(kp.getPublic().getEncoded());

        CryptoKey privateKey = factory.createCryptoKey();
        privateKey.setAlgorithm(kg.getAlgorithm());
        privateKey.setValue(kp.getPrivate().getEncoded());

        return new Object[] { publicKey, privateKey };
    }

    private void attachKeys(Vertex v) throws Exception {
        Object[] keys = generateKeyPair();

        v.setPublicKey((CryptoKey) keys[0]);
        v.setPrivateKey((CryptoKey) keys[1]);
    }

    private void attachKeys(Actor a) throws Exception {
        Object[] keys = generateKeyPair();

        a.setPublicKey((CryptoKey) keys[0]);
        a.setPrivateKey((CryptoKey) keys[1]);
    }

    private void readConfiguration() throws Exception {
        Unmarshaller u = jc.createUnmarshaller();
        u.setValidating(true);
        config = (Configuration) u.unmarshal(new FileInputStream(inputFile));
    }

    public void process() throws Exception {
        initialize();
        readConfiguration();

        // attach keys to the edges
        Iterator iter = config.getTopology().getEdges().getEdge().iterator();
        while (iter.hasNext()) {
            Edges.Edge edge = (Edges.Edge) iter.next();
            attachKeys(edge.getFrom());
            attachKeys(edge.getTo());
        }

        // give each actor a default key pair
        iter = config.getActors().getActor().iterator();
        while (iter.hasNext()) {
            attachKeys((Actor) iter.next());
        }

        save();
    }

    private void save() throws Exception {
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(config, new FileOutputStream(outputFile));
    }

    public static void main(String[] args) {
        try {
            AttachKeys attach = new AttachKeys(args[0], args[1]);
            attach.process();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
}
