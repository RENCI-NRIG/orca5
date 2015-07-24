package orca.ndl.elements;

import orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;

public class SwitchMatrix extends NetworkElement {

	@Persistent
	private String capability;
	@Persistent
	private String swappingcapability;
	@Persistent
	private String tunnelingcapability;
	@Persistent
	private String direction;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toString());
		
		sb.append("Capabilities: " + capability + "/" + swappingcapability + "/" + tunnelingcapability + "/" + direction + "\n");
		
		return sb.toString();
	}
	
	public SwitchMatrix() {
		super();
		direction=Direction.BIDirectional.toString();	
	}
	
	public SwitchMatrix(OntModel m, OntResource rs){
		super(m,rs);
		direction=Direction.BIDirectional.toString();
	}
	
	public void print(Logger logger){
		super.print(logger);
		int size=0;
		int i=0;
		logger.info("Layer:"+atLayer);
		logger.info("SwappingCapability:"+swappingcapability);
		logger.info("Direction:"+direction);
		Interface intf=null;
		if(clientInterface!=null){
			size=clientInterface.size();
			for(i=0;i<size;i++){
				intf =(Interface) clientInterface.get(i);
				intf.print(logger);
			}
		}
	}
	
	public String getSwappingcapability() {
		return swappingcapability;
	}

	public void setSwappingcapability(String swappingcapability) {
		this.swappingcapability = swappingcapability;
	}
	
	public String getCapability(){
		return capability;
	}

	public void setCapability(String capability) {
		this.capability = capability;
	}
	
	public String getTunnelingcapability() {
		return tunnelingcapability;
	}

	public void setTunnelingcapability(String tunnelingcapability) {
		this.tunnelingcapability = tunnelingcapability;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

}
