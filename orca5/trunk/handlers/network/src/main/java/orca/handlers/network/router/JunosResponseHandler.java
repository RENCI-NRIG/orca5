package orca.handlers.network.router;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/***
 * Handle responses from Junos devices for the stages:
 * 0. Session open
 * 1. Login
 * 2. Configuration update
 * 3. Commit 
 * 4. Session close
 * 
 * @author ibaldin
 *
 */
public class JunosResponseHandler extends DefaultHandler {
    protected Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
    protected String cdata = "";
    protected String message = "";
    protected Locator locator = null;
    
    protected enum SessionStages { Initial, SessionOpen, LoggedIn, ConfigurationUpdated, 
    	ConfigurationCommitted, SessionEnded, Completed, Error, Warning, EndOfDoc };
    protected SessionStages stage;
    protected SessionStages nextStage, nextStageFromWarning = SessionStages.Error;
    protected boolean inReply = false, commitSent = false;
	
    protected IJunosInteractor ji;
    
    JunosResponseHandler(IJunosInteractor ji) {
    	this.ji = ji;
    }
    
    private String useName(String uri, String name, String qname) {
    	return "".equals(uri) ? qname : name;
    }
    
    private void advanceStage() {
    	if (stage == nextStage)
    		throw new RuntimeException("Could not advance to the next processing stage from [" + getStage() + "]");
    	stage = nextStage;
    }
    
	@Override
	public void startElement(String uri, String name, String qname, Attributes attributes) throws SAXException {
		String localName = useName(uri, name, qname);
		
		// reset cdata
		cdata = "";
		
		if ((localName.equals("junoscript")) &&
				(stage == SessionStages.Initial)) {
			// check junos version?
			nextStage = SessionStages.SessionOpen;
			advanceStage();
			ji.sendHandshakeAndLogin();
			return;
		}
		
		if (localName.equals("rpc-reply")) {
			inReply = true;
			return;
		}
		
		// reset message
		if (localName.equals("message"))
			message = "";
		
		// must be inside rpc reply
		if (inReply) {
			switch(stage) {
			case SessionOpen:
				// wait for completion of 'status' tag
				;
				break;
			case LoggedIn:
				if (localName.equals("load-success")) {
					logger.debug("Configuration updated successfully");
					nextStage = SessionStages.ConfigurationUpdated;
					if (!commitSent) {
						ji.sendCommit();
						commitSent = true;
					}
				}
				if (localName.equals("error")) {
					logger.error("Error loading configuration");
					nextStage = SessionStages.Error;
				}
				// if we see a warning, we may still be ok, but we'll need
				// to check at endElement what the warning said
				if (localName.equals("warning")) {
					// we'll need to check what kind of a warning this is
					nextStage = SessionStages.Warning;
					nextStageFromWarning = SessionStages.ConfigurationUpdated;
					if (!commitSent) {
						ji.sendCommit();
						commitSent = true;
					}
				}
				break;
			case ConfigurationUpdated:
				if (localName.equals("commit-success")) {
					logger.debug("Configuration committed successfully");
					nextStage = SessionStages.ConfigurationCommitted;
					ji.sendEndSessionRequest();
				}
				if (localName.equals("error")) {
					logger.error("Error committing configuration");
					nextStage = SessionStages.Error;
				}
				break;
			case ConfigurationCommitted:
				if (localName.equals("end-session")) {
					logger.debug("Ended session");
					nextStage = SessionStages.SessionEnded;
					ji.sendCloseJunoscript();
				} 
				if (localName.equals("error")) {
					logger.error("Error ending session");
					nextStage = SessionStages.Error;
				}
				break;
			default:
				throw new RuntimeException("Don't know what to do from <reply> in stage [" + getStage() + "]");	
			}
		}
	}

	
	@Override
	public void endElement(String uri, String name, String qname) throws SAXException {	
		String localName = useName(uri, name, qname);
		
		// check for closing of the session
		if (localName.equals("junoscript"))  
			if (stage != SessionStages.SessionEnded)
				throw new RuntimeException("Junoscript session ended prematurely in stage [" + getStage()+ "]");
			else {
				stage = SessionStages.Completed;
				nextStage = SessionStages.EndOfDoc;
				return;
			}
		
		// coming out of reply 
		if (localName.equals("rpc-reply")) {
			// check for errors in this reply
			if (nextStage == SessionStages.Error)
				throw new RuntimeException("XML Reply returned error: " + message + " in stage [" + getStage() + "]");
			
			if (nextStage == SessionStages.Warning) {
				// if message is about 'statement not found', continue, because this
				// just means we were deleting something that didn't exist, else exit to be safe
				
				if (message.matches("^\\s*statement not found[.|\\s]*$")) {
					logger.debug("Warning about " + message + " encountered, continuing");
					nextStage = nextStageFromWarning;
				}
				else
					throw new RuntimeException("XML Reply returned warning: " + 
							message + " in stage [" + getStage() +"]");
			}
			advanceStage();
			inReply = false;
			return;
		}
		
		// catch all messages
		if (localName.equals("message")) {
			message += cdata;
			return;
		}

		if (inReply) {
			switch(stage) {
			case SessionOpen:
				// check the reported status - need to wait for closing
				// status to collect cdata
				if (localName.equals("status")) {
					if (cdata.equals("success")) {
						nextStage = SessionStages.LoggedIn;
						// send configuration
						ji.sendConfigurationUpdate();
					}
					else {
						logger.error("Unable to login to JunOS device");
						nextStage = SessionStages.Error;
					}
				}
				break;
			default:	
				;
			}
		}
	}
	
	@Override
	public void startDocument() throws SAXException {
		stage = SessionStages.Initial;
	}

	@Override
	public void endDocument() throws SAXException {
		if (nextStage != SessionStages.EndOfDoc)
			throw new RuntimeException("Unexpected end of JunOS response");
		advanceStage();
	}
	
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        cdata += new String(ch, start, length);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }
    
    public String getStage() {
    	switch(stage) {
    	case Initial: 
    		return "Initial";
    	case SessionOpen:
    		return "SessionOpen (sending login)";
    	case LoggedIn:
    		return "LoggedIn (sending configuration update)";
    	case ConfigurationUpdated:
    		return "ConfigurationUpdated (sending commit)";
    	case ConfigurationCommitted:
    		return "Committed (sending end session request)";
    	case SessionEnded:
    		return "SessionEnded (closing session)";
    	case Completed:
    		return "Completed";
    	case Error:
    		return "Error";
    	case EndOfDoc:
    		return "EndOfDoc";
    	}
    	return "";
    }
    
//    public void parse() {
//       	try {
//    		XMLReader reader = XMLReaderFactory.createXMLReader();
//    		reader.setContentHandler(new JunosResponseHandler());
//    		URL url = this.getClass().getResource("BadLoginJunosSession.xml");
//    		System.out.println("Loading " + url.toString());
//    		reader.parse(new InputSource(url.toString()));
//    	} catch (Exception e) {
//    		System.out.println("Some exception " + e);
//    	}
//    }
//    
//    
//    public static void main(String[] arstring) {
//    	try {
//    		JunosResponseHandler jrh = new JunosResponseHandler();
//    		jrh.parse();
//    	} catch (RuntimeException e) {
//    		System.out.println(e);
//    	}
//    }
}


