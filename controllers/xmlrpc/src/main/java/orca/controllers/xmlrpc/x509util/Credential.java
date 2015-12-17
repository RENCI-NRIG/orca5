package orca.controllers.xmlrpc.x509util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.login.CredentialException;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import orca.controllers.OrcaController;
import orca.controllers.xmlrpc.XmlrpcHandlerHelper;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Credential {

	private Document credentialDocument;
	
	private Date expirationDate = null;
	private String callerUrn = null;
	private Gid callerGid = null;
	private String objectUrn = null;
	private Gid objectGid= null;
	
	private Signature signature = null;
	private String refId = null;

        private String type = null; // What type of credential - "abac", "privilege"
	
	private Credential parent = null;

        
	protected static Logger logger = OrcaController.getLogger(Credential.class.getSimpleName());
        //protected static Logger logger = Logger.getLogger(Credential.class.getName());
	
	
	/**
	 * class representing the signature part of an xml credential
	 */
	public class Signature{
		private String refId;
		private Gid issuerGid;
		private Document credentialDocument;
		
		Signature(Element signature) throws CertificateException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException{
			this.credentialDocument = parse(nodeToString(signature));
			decode();
		}
		
		void decode() throws CertificateException, IOException{
			Element signature = (Element)this.credentialDocument.getElementsByTagName("Signature").item(0);

                        // Changed this for supporting speaks_for because refId for a signature might not be found from xml:id
                        // attribute in a speaks_for credential; xml:id attribute might not be present in a speaks_for credential;
                        // For other credentials, it might be in xml:id attribute

                        //this.refId = signature.getAttribute("xml:id").replaceFirst("Sig_", "");
                        
                        // refId calculation of a signature 
                        // if there is an xml:id attribute in Signature, start with that
                        // if that is null, use xml:id attribute in "Reference" inside Signature
                        // if that is also null, use URI attribute in "Reference" inside Signature
                        
                        String currRefId = signature.getAttribute("xml:id").replaceFirst("Sig_", "");
     
                        if (currRefId == null || currRefId.equalsIgnoreCase("")){
                            Element reference = (Element) signature.getElementsByTagName("Reference").item(0);
                            if(reference != null){
                                currRefId = reference.getAttribute("xml:id").replaceFirst("Sig_", "");
                                if (currRefId == null || currRefId.equalsIgnoreCase("")){
                                    currRefId = reference.getAttribute("URI").replaceFirst("#", "");
                                }
                            }
                        }

                        this.refId = currRefId;

			Element keyInfo = (Element)signature.getElementsByTagName("X509Data").item(0);
			String issuerGidString = getElementValue(keyInfo, "X509Certificate");
			this.issuerGid = new Gid(issuerGidString);
		}
		
		public String getRefId() {
			return this.refId;
		}

		public Gid getIssuerGid() {
			return this.issuerGid;
		}

		public Document getCredentialDocument(){
			return this.credentialDocument;
		}
	}
	
	public Credential(String credString) throws CredentialException, CertificateException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException {
		this.credentialDocument = parse(credString);
		decode();
	}

	public Credential(Element rootNode) throws CredentialException, CertificateException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException {
		this.credentialDocument = parse(nodeToString(rootNode));
		decode();
	}
	
	private Document parse(String credString) throws ParserConfigurationException, SAXException, IOException {
		//logger.debug(credString);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//logger.debug("DocumentBuilderFactory: "+ factory.getClass().getName());
        
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
        
        // Specify our own schema - this overrides the schemaLocation in the xml file
        
        // This schema file works only for normal credential
        //factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", "http://www.protogeni.net/resources/credential/credential.xsd");

        // TODO: Fix this. Package the schema files in the source tree instead of fetching them from geni-images
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", "http://geni-images.renci.org/images/anirban/geni/credential.xsd");

        DocumentBuilder builder = factory.newDocumentBuilder();
        //builder.setErrorHandler( new SimpleErrorHandler() );
        
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(credString));

        Document credDoc = builder.parse(is);
        
        return credDoc;
	}
	
	/**
	 * decodes the the individual elements out of the xml document
	 * @throws CredentialException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	private void decode() throws CredentialException, CertificateException, IOException, ParserConfigurationException, SAXException, TransformerFactoryConfigurationError, TransformerException{

                this.type = this.getElementValue("type");
                logger.debug("type: " + this.type);

                //SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                //dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String date = getElementValue("expires");
                logger.debug("Expiration date = " + date);
		try {
			//this.expirationDate = dateFormatter.parse(getElementValue("expires"));
			this.expirationDate = XmlrpcHandlerHelper.parseRFC3339Date(date);
		} catch (ParseException e) {
			// assume UTC if no timezone specified
			date += "Z";
			try {
				this.expirationDate = XmlrpcHandlerHelper.parseRFC3339Date(date);
			} catch (ParseException ee) {
				throw new CredentialException("Expiry date parse exception");
			}
		}

                // owner_urn, owner_gid, target_urn, target_gid, parent are not important for abac type credential

                if(!this.type.equalsIgnoreCase("abac")){ // parse these if credential is not "abac" type

                    this.callerUrn = this.getElementValue("owner_urn");
                    logger.debug("callerUrn: " + this.callerUrn);

                    String ownerGidString = this.getElementValue("owner_gid");
                    this.callerGid = new Gid(ownerGidString);

                    this.objectUrn = this.getElementValue("target_urn");
                    logger.debug("objectUrn: " + this.objectUrn);

                    String targetGidString = this.getElementValue("target_gid");
                    this.objectGid = new Gid(targetGidString);

                    NodeList parentNode = this.credentialDocument.getElementsByTagName("parent");
                    if(parentNode.getLength() != 0){
                            this.parent = new Credential((Element)parentNode.item(0).getFirstChild());
                    }

                }

                NodeList signedCred = this.credentialDocument.getElementsByTagName("signed-credential");
                Element credential;
                NodeList signatures = null;
                if(signedCred.getLength() > 0){
                        credential = (Element)((Element)signedCred.item(0)).getElementsByTagName("credential").item(0);
                        NodeList signatureList = ((Element)signedCred.item(0)).getElementsByTagName("signatures");
                        if(signatureList.getLength() > 0){
                                signatures = ((Element)signatureList.item(0)).getElementsByTagName("Signature");
                        }
                }else{
                        credential = (Element)this.credentialDocument.getElementsByTagName("credential").item(0);
                }

                this.refId = credential.getAttribute("xml:id");
                logger.debug("refId: " + this.refId);

                if(signatures != null){
                        for(int sigItr = 0; sigItr < signatures.getLength(); sigItr++){
                        Signature signature = new Signature((Element)signatures.item(sigItr));
                        Iterator<Credential> credentials = this.getCredentialList().iterator();
                        while(credentials.hasNext()){
                                Credential currentCredential =  credentials.next();
                                if(currentCredential.refId.equals(signature.refId)){
                                        currentCredential.signature = signature;
                                        break;
                                }
                        }
                    }
                }

	}
	
	/**
	 * Verify that:
	 * All of the signatures are valid and that the root credential issuer traces 
	 * back to trusted roots
	 * The XML matches the credential schema
	 * That the issuer of the credential is the authority in the target's urn
	 * In the case of a delegated credential, this must be true of the root
	 * That all of the gids presented in the credential are valid
	 * The credential is not expired
	 * 
	 * For Delegates (credentials with parents)
	 * 	The privileges must be a subset of the parent credentials
	 * 	The privileges must have "can_delegate" set for each delegated privilege
	 * 	The target gid must be the same between child and parents
	 * 	The expiry time on the child must be no later than the parent
	 * 	The signer of the child must be the owner of the parent
	 * 
	 * Verify does *NOT*
	 *  ensure that an xmlrpc client's gid matches a credential gid, that
	 * @param trustStorePath
	 * @param trustStorePassword
	 * @throws CredentialException
	 * @throws CertPathValidatorException
	 * @throws CertificateException
	 * @throws XMLSignatureException
	 * @throws MarshalException
	 */
	public void verify(String trustStorePath, String trustStorePassword) throws CredentialException, CertPathValidatorException, CertificateException, XMLSignatureException, MarshalException{
		
            if(!this.expirationDate.after(new Date())){
                    throw new CredentialException("Expired Credential");
            }

            if(!this.callerGid.getUrn().equals(this.callerUrn)){
                    throw new CredentialException("Owner Urn Gid Mismatch");
            }

            if(!this.objectGid.getUrn().equals(this.objectUrn)){
                    throw new CredentialException("Target Urn Gid Mismatch");
            }

            // Verify the gids of this cred and of its parents
            Iterator<Credential> credentials = this.getCredentialList().iterator();
            while(credentials.hasNext()){
                    Credential credential =  credentials.next();
                    credential.callerGid.verifyCertChain(trustStorePath, trustStorePassword);
                    credential.objectGid.verifyCertChain(trustStorePath, trustStorePassword);
            }

            //validating all the xml signatures
            XmlDigitalSigValidator.validate(this.getCredentialDocument());
        
            if(this.parent != null)
		verifyParent();
		
            verifyIssuer(trustStorePath, trustStorePassword);
            
	}
	
	/**
	 * For Delegates (credentials with parents)
	 * 	The privileges must be a subset of the parent credentials
	 * 	The privileges must have "can_delegate" set for each delegated privilege
	 * 	The target gid must be the same between child and parents
	 * 	The expiry time on the child must be no later than the parent
	 * 	The signer of the child must be the owner of the parent
	 * @throws CredentialException
	 */
	private void verifyParent() throws CredentialException{
		
		// make sure the rights given to the child are a subset of the
		// parents rights (and check delegate bits)
		Set<String> delegatablePrivileges = parent.getDelegatablePrivileges();
        	if((!delegatablePrivileges.contains("*")) && (!delegatablePrivileges.containsAll(this.getPrivileges()))){
            		throw new CredentialException("Parent cred not superset of delegated cred");
        	}
		
		// make sure my target gid is the same as the parent's
		if(!parent.objectGid.equals(this.objectGid)){
			throw new CredentialException("Target gid not equal between parent and child");
		}
		
		// make sure my expiry time is <= my parent's
		if(this.expirationDate.after(parent.expirationDate)){
			throw new CredentialException("Delegated credential expires after parent");
		}
		
		// make sure my signer is the parent's caller
		if(!parent.callerGid.equals(this.signature.getIssuerGid())){
			throw new CredentialException("Delegated credential not signed by parent caller");
		}
		
		if(this.parent.parent != null){
			this.parent.verifyParent();
		}
	}
	
	/**
	 * verify if the credential's target gid was signed by (or is the same) the entity that signed 
	 * the original credential or an authority over that namespace.
	 * @param trustKeyStorePath
	 * @param keyStorePassword
	 * @throws CertPathValidatorException
	 * @throws CertificateException
	 * @throws CredentialException
	 */
	private void verifyIssuer(String trustKeyStorePath, String keyStorePassword) throws CertPathValidatorException, CertificateException, CredentialException {
		
		Credential rootCredential = getRootCredential();
		Gid rootTargetGid = rootCredential.objectGid;
		Gid rootIssuerGid = rootCredential.signature.getIssuerGid();
		
		// cred signer matches target signer, return success
		if(rootTargetGid.isSignedBy(rootIssuerGid)){
			return;
		}
		
		// cred signer is target, return success
		if(rootTargetGid.equals(rootIssuerGid)){
			return;
		}
		
		// See if it the signer is an authority over the domain of the target
		// Maybe should be (hrn, type) = urn_to_hrn(root_cred_signer.get_urn())
		rootIssuerGid.verifyCertChain(trustKeyStorePath, keyStorePassword);
		String rootCredSignerType = rootIssuerGid.getType();
		if(rootCredSignerType.equals("authority")){
			//signer is an authority, see if target is in authority's domain
			String hrn = rootIssuerGid.getHrn();
			if(rootTargetGid.getHrn().startsWith(hrn)){
				return;
			}
		}
		
		// We've required that the credential be signed by an authority
		// for that domain. Reasonable and probably correct.
		// A looser model would also allow the signer to be an authority
		// in my control framework - eg My CA or CH. Even if it is not
		// the CH that issued these, eg, user credentials.

		// Give up, credential does not pass issuer verification

		throw new CredentialException("Could not verify credential owned by " + callerGid.getUrn() + 
				"for object " + objectGid.getUrn() + ". Cred signer " + rootIssuerGid.getHrn() + 
				" not the trusted authority for Cred target " + rootTargetGid.getHrn()); 
	}
	
	public String getElementValue(Element element, String subElement){
		NodeList sub = element.getElementsByTagName(subElement);
		if ((sub != null) && (sub.item(0) != null) && (sub.item(0).getChildNodes().getLength() > 0)){
			return sub.item(0).getChildNodes().item(0).getNodeValue();
		}
		else{
			return null;
		}
	}
	
	public String getElementValue(String subElement){
		return getElementValue((Element)this.credentialDocument.getFirstChild(), subElement);
	}
	
	/**
	 * gets the set of delegatable privileges
	 * @return set of delegatable privileges
	 */
	public Set<String> getDelegatablePrivileges(){
		Set<String> privilegeSet = new HashSet<String>();
		Node privileges = this.credentialDocument.getElementsByTagName("privileges").item(0);
		if(privileges.hasChildNodes()){
			String privilegeName; boolean delegatable; String delegatableNodeValue;
			Node privilege = privileges.getFirstChild();
			while(privilege != null){
				privilegeName = null; delegatable = false; delegatableNodeValue = null;
				Node childNode = privilege.getFirstChild();
				while(childNode != null){
					if(childNode.getNodeName().equals("name")){
						privilegeName = childNode.getFirstChild().getNodeValue();
					}else if(childNode.getNodeName().equals("can_delegate")){
						delegatableNodeValue = childNode.getFirstChild().getNodeValue();
                        			delegatable = ((delegatableNodeValue != null) &&
                                			((delegatableNodeValue.toLowerCase().equals("true")) || (delegatableNodeValue.equals("1"))));
                        			//delegatable = new Boolean(childNode.getFirstChild().getNodeValue());
					}
					childNode = childNode.getNextSibling();
				}
				if(delegatable && privilegeName != null){
					privilegeSet.add(privilegeName);
				}
				privilege = privilege.getNextSibling();
			}
		}
		return privilegeSet;
	}
	
	/**
	 * gets the entire set of privileges
	 * @return entire set of privileges
	 */
	public Set<String> getPrivileges(){
		Set<String> privilegeSet = new HashSet<String>();
		Node privileges = this.credentialDocument.getElementsByTagName("privileges").item(0);
		if(privileges.hasChildNodes()){
			Node privilege = privileges.getFirstChild();
			while(privilege != null){
				Node childNode = privilege.getFirstChild();
				while(childNode != null){
					if(childNode.getNodeName().equals("name")){
						privilegeSet.add(childNode.getFirstChild().getNodeValue());
					}
					childNode = childNode.getNextSibling();
				}
				privilege = privilege.getNextSibling();
			}
		}
		return privilegeSet;
	}
	
	/**
	 * checks if the credential has at least one of the given set of privileges
	 * @param requiredPrivilege
	 * @return
	 */
	public boolean hasPrivilege(Set<String> requiredPrivilege){
		requiredPrivilege.retainAll(getPrivileges());
		return (!requiredPrivilege.isEmpty());
	}
	
	/**
	 * get the chain of credentials
	 * @return credential chain
	 */
	private List<Credential> getCredentialList(){
		List<Credential> credentialList = new ArrayList<Credential>();
		Credential currentCred = this;
		while(currentCred != null){
			credentialList.add(currentCred);
			currentCred = currentCred.parent;
		}
		return credentialList;
	}
	
	/**
	 * gets the root of the credential chain
	 * @return root credential
	 */
	private Credential getRootCredential(){
		if(this.parent == null){
			return this;
		}else{
			return parent.getRootCredential();
		}
	}
	
	public Document getCredentialDocument(){
		return this.credentialDocument;
	}

        public String getType(){
                return type;
        }

	public Date getExpirationDate() {
		return expirationDate;
	}
	
	public String getCallerUrn() {
		return callerUrn;
	}

	public Gid getCallerGid() {
		return callerGid;
	}

	public String getObejctUrn() {
		return objectUrn;
	}

	public Gid getObjectGid() {
		return objectGid;
	}

	public Credential getParent() {
		return parent;
	}

        public Signature getSignature(){
            return signature;
        }

        /** For "abac" type credentials, return keyid in the head
         *
         * @return keyid value inside head
         */
        public String getHeadKeyid(){
            
            if(this.type !=null  && this.type.equalsIgnoreCase("abac")){
                Node head = this.credentialDocument.getElementsByTagName("head").item(0);
                if(head.hasChildNodes()){
			Node headChild = head.getFirstChild();
                        while(headChild != null){
                            if(headChild.getNodeName().equals("ABACprincipal")){
                                Node keyIdNode = headChild.getFirstChild();
                                if(keyIdNode != null && keyIdNode.getNodeName().equals("keyid")){
                                    return keyIdNode.getTextContent();
                                }
                            }
                            headChild = headChild.getNextSibling();
                        }
                }
            }
            else{ // this is not "abac" type credential
                return null;
            }

            return null;

        }

        /** For "abac" type credentials, return role in the head
         *
         * @return role value inside head
         */
        public String getHeadRole(){

            if(this.type !=null  && this.type.equalsIgnoreCase("abac")){
                Node head = this.credentialDocument.getElementsByTagName("head").item(0);
                if(head.hasChildNodes()){
			Node headChild = head.getFirstChild();
                        while(headChild != null){
                            if(headChild.getNodeName().equals("role")){
                                return headChild.getTextContent();
                            }
                            headChild = headChild.getNextSibling();
                        }
                }
            }
            else{ // this is not "abac" type credential
                return null;
            }

            return null;

        }

        /** For "abac" type credentials, return keyid in the tail
         *
         * @return keyid value inside tail
         */

        public String getTailKeyid(){

            if(this.type !=null  && this.type.equalsIgnoreCase("abac")){
                Node tail = this.credentialDocument.getElementsByTagName("tail").item(0);
                if(tail.hasChildNodes()){
			Node tailChild = tail.getFirstChild();
                        while(tailChild != null){
                            if(tailChild.getNodeName().equals("ABACprincipal")){
                                Node keyIdNode = tailChild.getFirstChild();
                                if(keyIdNode != null && keyIdNode.getNodeName().equals("keyid")){
                                    return keyIdNode.getTextContent();
                                }
                            }
                            tailChild = tailChild.getNextSibling();
                        }
                }
            }
            else{ // this is not "abac" type credential
                System.out.println("not abac cred");
                return null;
            }

            return null;

        }


	private String nodeToString(Node node) throws TransformerFactoryConfigurationError, TransformerException {
		StringWriter sw = new StringWriter();
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		t.transform(new DOMSource(node), new StreamResult(sw));
		return sw.toString();
	}
	
	public static void main(String[] argv) {
		String date = "2013-05-21T03:06:32";
                //date = "2014-06-14T17:04:11.646Z";
		Date expirationDate = null;
        //SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        //dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			//expirationDate = dateFormatter.parse(date);
			expirationDate = XmlrpcHandlerHelper.parseRFC3339Date(date);
		} catch (ParseException e) {
			// assume UTC if no timezone specified
			date += "Z";
			try {
				expirationDate = XmlrpcHandlerHelper.parseRFC3339Date(date);
			} catch (ParseException ee) {
				System.out.println("Expiry date parse exception");
			}
		}
		System.out.println(date + "    " + expirationDate);
	}
}
