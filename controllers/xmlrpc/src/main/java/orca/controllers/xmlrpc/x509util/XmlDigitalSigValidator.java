package orca.controllers.xmlrpc.x509util;

import java.security.Key;
import java.security.KeyException;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.List;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;

import orca.controllers.OrcaController;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This is a simple example of validating an XML
 * Signature using the JSR 105 API. It assumes the key needed to
 * validate the signature is contained in a KeyValue KeyInfo.
 */
public class XmlDigitalSigValidator {

        
	protected static Logger logger = OrcaController.getLogger(XmlDigitalSigValidator.class.getSimpleName());
        //protected static Logger logger = Logger.getLogger(XmlDigitalSigValidator.class.getName());
	
    public static void validate(Document doc) throws XMLSignatureException, MarshalException {

        // Find Signature element
        NodeList nl =
            doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0) {
            throw new XMLSignatureException("Cannot find Signature element");
        }

        // Create a DOM XMLSignatureFactory that will be used to unmarshal the
        // document containing the XMLSignature
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        for(int itr = 0; itr < nl.getLength(); itr++){
        	// Create a DOMValidateContext and specify a KeyValue KeySelector
            // and document context
            DOMValidateContext valContext = new DOMValidateContext
                (new KeyValueKeySelector(), nl.item(itr));

            // unmarshal the XMLSignature
            XMLSignature signature = fac.unmarshalXMLSignature(valContext);

            KeyValueKeySelector keyValueKeySelector = new KeyValueKeySelector();
            DOMValidateContext docContext = new DOMValidateContext(keyValueKeySelector, doc.getDocumentElement());
            
            // Validate the XMLSignature (generated above)
            boolean coreValidity = signature.validate(docContext);

            // Check core validation status
            if (coreValidity == false) {
                logger.error("Signature failed core validation");
                boolean sv = signature.getSignatureValue().validate(valContext);
                logger.debug("signature validation status: " + sv);
                // check the validation status of each Reference
                Iterator<?> i = signature.getSignedInfo().getReferences().iterator();
                for (int j=0; i.hasNext(); j++) {
                    boolean refValid =
                        ((Reference) i.next()).validate(docContext);
                    logger.debug("ref["+j+"] validity status: " + refValid);
                }
            } else {
            	logger.debug("Signature passed core validation");
            }
        }
    }

    /**
     * KeySelector which retrieves the public key out of the
     * KeyValue element and returns it.
     * NOTE: If the key algorithm doesn't match signature algorithm,
     * then the public key will be ignored.
     */
    private static class KeyValueKeySelector extends KeySelector {
    	
    	//private List<X509Certificate> signerCertificateChain = new ArrayList<X509Certificate>();
    	
        public KeySelectorResult select(KeyInfo keyInfo,
                                        KeySelector.Purpose purpose,
                                        AlgorithmMethod method,
                                        XMLCryptoContext context)
            throws KeySelectorException {
            if (keyInfo == null) {
                throw new KeySelectorException("Null KeyInfo object!");
            }
            
            SignatureMethod sm = (SignatureMethod) method;
            List<?> list = keyInfo.getContent();

            for (int i = 0; i < list.size(); i++) {
                XMLStructure xmlStructure = (XMLStructure) list.get(i);
                if (xmlStructure instanceof KeyValue) {
                    PublicKey pk = null;
                    try {
                        pk = ((KeyValue)xmlStructure).getPublicKey();
                    } catch (KeyException ke) {
                        throw new KeySelectorException(ke);
                    }
                    // make sure algorithm is compatible with method
                    if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
                        return new SimpleKeySelectorResult(pk);
                    }
                }
                /*else if (xmlStructure instanceof X509Data){
                	List<?> x509Data = ((X509Data)xmlStructure).getContent();
                	for (int x509DataItr = 0; x509DataItr < x509Data.size(); x509DataItr++) {
                        if (x509Data.get(x509DataItr) instanceof X509Certificate) {
                        	signerCertificateChain.add(((X509Certificate)x509Data.get(x509DataItr)));
                        }
                	}
                }*/
            }
            throw new KeySelectorException("No KeyValue element found!");
        }

        //FIXME: this should also work for key types other than DSA/RSA
        static boolean algEquals(String algURI, String algName) {
            if (algName.equalsIgnoreCase("DSA") &&
                algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
                return true;
            } else if (algName.equalsIgnoreCase("RSA") &&
                       algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
                return true;
            } else {
                return false;
            }
        }
        
        //public List<X509Certificate> getSignerCertificateChain() { return signerCertificateChain; }
    }

    private static class SimpleKeySelectorResult implements KeySelectorResult {
        private PublicKey pk;
        SimpleKeySelectorResult(PublicKey pk) {
            this.pk = pk;
        }

        public Key getKey() { return pk; }
    }
}
