package orca.embed.policyhelpers;

import java.io.IOException;
import java.util.Properties;

import orca.ndl.NdlCommons;

import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class LUNPolicyNDLPoolPropertyExtractor extends
GenericNDLPoolPropertyExtractor {
	public static String resourceType = "LUN"; 
	public static String lunStart="lun.tag.start";
	public static String lunEnd="lun.tag.end";
	
	public LUNPolicyNDLPoolPropertyExtractor(String modelStr) throws IOException {
		super(modelStr);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Properties getPoolProperties() {
		Properties vlanProperty= new Properties();
		Resource domain=null,set,setElement,lb,ub;
		Statement setStm,lbStm,ubStm;
		int lb_id = 0,ub_id = 0;
		int i=0;
		for(ResIterator j=delegateModel.listResourcesWithProperty(NdlCommons.RDF_TYPE, NdlCommons.networkDomainOntClass);j.hasNext();){
        	domain = j.next();
        	break;
        }
        if(domain==null)
        	return null;
        Resource networkService = domain.getProperty(NdlCommons.domainHasServiceProperty).getResource();
		for(StmtIterator j=networkService.listProperties(ontProcessor.availableLabelSet);j.hasNext();){
        	set=(Resource) j.next().getResource();
        	String type = null;
        	if(set.hasProperty(NdlCommons.domainHasResourceTypeProperty)){
        		type = set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource().getLocalName();
        		if(!type.endsWith(resourceType))
        			continue;
        	}else
        		continue;
        	
        	if(set.hasProperty(NdlCommons.domainIsAllocatable)){
				if(set.getProperty(NdlCommons.domainIsAllocatable).getBoolean()==false){
					continue;
				}
			}
        	
        	for (StmtIterator element=set.listProperties(ontProcessor.collectionElementProperty);element.hasNext();){
				i++;
        		setElement=element.next().getResource();
				
				lbStm=setElement.getProperty(ontProcessor.lowerBound);
				ubStm=setElement.getProperty(ontProcessor.upperBound);
				if(lbStm!=null){
					lb=lbStm.getResource();
					lb_id=lb.getProperty(ontProcessor.layerLabelIdProperty).getInt();
				}
				if(ubStm!=null){
					ub=ubStm.getResource();
					ub_id=ub.getProperty(ontProcessor.layerLabelIdProperty).getInt();
				}
				//single label
				if((lbStm==null) && (ubStm==null)){
					setStm=setElement.getProperty(ontProcessor.layerLabelIdProperty);
					if(setStm!=null){
						lb_id=setStm.getInt();
						ub_id=lb_id;
					}
				}
				
				vlanProperty.setProperty(lunStart+String.valueOf(i), String.valueOf(lb_id));
				vlanProperty.setProperty(lunEnd+String.valueOf(i), String.valueOf(ub_id));
        	}
        }
        
		return vlanProperty;
	}

}
