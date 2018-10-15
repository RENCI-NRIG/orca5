package net.exogeni.orca.ektorp;

import org.ektorp.support.CouchDbDocument;
import org.ektorp.support.TypeDiscriminator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author claris
 * Actor class corresponding to each individual record in Actor table in CouchdB.
 *  Every change, e.g., addition of fields, must be reflected in this document otherwise Ektorp will
 *  complain that can't CRUD operate on actor documents in CouchDB.
 */
@JsonIgnoreProperties("_deleted_conflicts")
public class actor extends CouchDbDocument {
		/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/*
	  * @TypeDiscriminator is used to mark properties that makes this class's documents unique in the database.
      */
     @TypeDiscriminator
	private String id;
	private String name;
	
	
	private String revision;
	private String type;
	private String description;
	private String soapURL;
	private String pubKey;
	private String cert;
	private String abstNDL;
	private String fullNDL;
	private String verified;
	private String alive;
	
	
	@JsonProperty("_id")
    public String getId() {
            return id;
    }
	
	@JsonProperty("_id")
    public void setId(String s) {
            id = s;
    }
	
	@JsonProperty("_rev")
    public String getRevision() {
            return revision;
    }

    @JsonProperty("_rev")
    public void setRevision(String s) {
            revision = s;
    }
    
    public String getAlive() {
    	return alive;
    }
    
    public void setAlive(String s) {
    	alive = s;
    }
    public String getName() {
    	return name;
    }
    
   
    public void setName(String s) {
    	name = s;
    }
    
    public String getType() {
    	return type;
    }
    
    public void setType(String s) {
    	type = s;
    }
    
    public String getDescription() {
    	return description;
    }
    
    public void setDescription(String s) {
    	description = s;
    }

    public String getSoapURL() {
    	return soapURL;
    }
    
    public void setSoapURL(String s) {
    	soapURL = s;
    }
    
    public String getPubKey() {
    	return pubKey;
    }
    
    public void setPutKey(String s) {
    	pubKey = s;
    }
    
    public String getCert() {
    	return cert;
    }
    
    public void setCert(String s) {
    	cert = s;
    }
    
    public String getAbstNDL() {
    	return abstNDL;
    }
    
    public void setAbstNDL(String s) {
    	abstNDL = s;
    }
    
    public String getFullNDL() {
    	return fullNDL;
    }
    
    public void setFullNDL(String s) {
    	fullNDL = s;
    }
    
    public String getVerified() {
    	return verified;
    }
    
    public void setVerified(String s){
    	verified = s;
    }
    
   
}
