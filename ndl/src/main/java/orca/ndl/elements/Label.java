package orca.ndl.elements;

import java.util.HashMap;
import java.util.Hashtable;

import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;

public class Label implements Persistable {
	
	@Persistent
	public String name = "unspecified";
	@Persistent
	public Float label;
	@Persistent
	public String type;
	@Persistent
	protected String label_uri;
	@Persistent
	public HashMap<String,String> metric; //bandwidth, etc......
	@Persistent
	public Float swap;
	
	public Label(String name,float label,String type){
		this.name=name;
		this.label=label;
		this.type=type;
		metric = new HashMap <String,String> ();
	}
	
	public Label(OntResource rs,float label,String type){
		this.label_uri=rs.getURI();
		this.label=label;
		this.type=type;
	}
	public Label(OntResource rs,String name,float label,String type){
		this.label_uri=rs.getURI();
		this.name=name;
		this.label=label;
		this.type=type;
		metric = new HashMap <String,String> ();
	}
	public Label(){
		
	}
	
	/**
	 * Limited to comparing type and label value
	 */
	public boolean equals(Object o) {
		if ((o instanceof Label) && (type != null)) {
			Label newL = (Label)o;
			return (type.equals(newL.type) && (label == newL.label)); 
		}
		return false;
	}
	
	public void setmetric(String key, String value){
		if (metric==null)
			metric = new HashMap <String,String> ();
		metric.put(key, value);
	}
	
	public String toString(){
		return name+":"+label+":"+type+":"+swap+":"+label_uri;
	}
	public void print(Logger logger){
		logger.info(label);
	}
	
	public void setResource(OntResource r) {
		if (r != null)
			label_uri = r.getURI();
		else
			label_uri = null;
	}
	
	/**
	 * Caller must provide a reference model
	 * @param m
	 * @return
	 */
	public OntResource getResource(OntModel m) {
		if(m.isClosed())
			return null;
		return m.getOntResource(label_uri);
	}
	
	public String getURI() {
		return label_uri;
	}
}
