/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orca.controllers.xmlrpc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import orca.controllers.OrcaController;
import orca.ndl.NdlCommons;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 *
 * @author anirban, pruth
 */
public class ScriptConstructor {

    private static String TEMPLATE_FILE_TMP = "velocity-";
   
    private VelocityContext context = null;
    private ReservationElementCollection reservations;
    private HashMap<String,VM> vm_table;
    private Logger logger;
    private String tmpDir;
    
    public class LINK{
    	public String name;
    	public ArrayList<String> vms = new ArrayList<String>();
    	public ArrayList<String> groups = new ArrayList<String>();
    	public ReservationElementCollection reservations;
    	
    	public LINK(ReservationElementCollection reservations, String name){
    		this.reservations = reservations;
    		this.name = name;
    		this.vms = new ArrayList<String>();
    		this.groups = new ArrayList<String>();
    	}
    	
    	
    	public String Name(){
    		return name;
    	}
    	
    	public Collection<String> getGroups(){
    		return groups;
    	}
    	
    	public Collection<String> getVMs(){
    		return vms;
    	}
    	
    	public String toString(){
    		return "name: " + name +
    				", vms: " + vms +
    				", groups: " + groups;
    		
    	}
    }
    
    public class VM{
    	public String name;
    	public HashMap<String,String> ips = null;
    	public HashMap<String,String> macs = null;
    	public ArrayList<String> links = null;
    	public ReservationElementCollection reservations;
    	public String group;
    	
    	public VM(ReservationElementCollection reservations, String name){
    		this.reservations = reservations;
    		this.name = name;
    		this.ips = new HashMap<String,String>();
    		this.macs = new HashMap<String,String>();
    		this.links = new ArrayList<String>();
    		this.group = "Not in Group";
    	}
    	
    	public String IP(String link){
    		String ip_raw = ips.get(link);
    		return ip_raw.substring(0, ip_raw.indexOf('/'));    		
    	}
    	
    	public String Netmask(String link){
    		String ip_raw = ips.get(link);
    		return ip_raw.substring(ip_raw.indexOf('/'));    		
    	}
    	
    	public String MAC(String link){
    		return macs.get(link);
    	}
    	
    	public String Name(){
    		return name;
    	}
    	
    	public String Group(){
    		return group;
    	}
    	
    	public Collection<String> Links(){
    		return links;
    	}
    	
    	public String toString(){
    		return "name: " + name +
    				", macs: " + macs.toString() +
    				", ips: " + ips.toString() +
    				", links: " + links.toString() +
    				", group: " + group ;
    		
    	}
    }
    
    public class Group{
    	public String name;
    	public ArrayList<VM> vms = null;
    	
    	public Group(String name){
    		this.name = name;
    		this.vms = new ArrayList<VM>();
    	}
    }
    
    private static boolean needsInit = true;
    private static void VelocityInit(String tmpDir){
    	try{
    		if(needsInit){
    			Velocity.setProperty("runtime.log", OrcaController.HomeDirectory + "/logs/velocity.log");
    			Velocity.setProperty("file.resource.loader.path", tmpDir);
    			Velocity.init();
    		}
    		needsInit = false;
    	} catch (Exception e){
    		System.out.println("XXXXXX-PRUTH: VelocityInit Exception " + e);
    	}
    }	
    
    public ScriptConstructor(ReservationElementCollection reservations, XmlrpcControllerSlice ndlSlice){
    	try {
    		//System.out.println("XXXXXX-PRUTH: ScriptConstructor");
    		//System.out.println("XXXXXX-PRUTH: ScriptConstructor: reservations = " + reservations);
    	
    		logger = OrcaController.getLogger("velocity");
    		
    		tmpDir = OrcaController.getProperty(XmlRpcController.PropertyVelocityTmpdir);
    		if (tmpDir == null)
    			tmpDir = "/tmp";
    		
    		VelocityInit(tmpDir);
    		
    		this.reservations = reservations;
    		this.vm_table = new HashMap<String,VM>();
    		/*                                                                                                                       		 *  Make a context object and populate with the data.  This                                                          
    		 *  is where the Velocity engine gets the data to resolve the                                                        
    		 *  references (ex. $list) in the template                                                                           
    		 */

    		context = new VelocityContext();

    		//Add Slice info to context
    		if (ndlSlice != null && ndlSlice.getSliceUrn() != null){
    			context.put("sliceName", ndlSlice.getSliceUrn());
    		} else {
    			context.put("sliceName", "SliceNameNull");
    		}
    		
    		if (ndlSlice != null && ndlSlice.getUserDN() != null){
    			context.put("userDN", ndlSlice.getUserDN());
    		} else {
    			context.put("userDN", "SliceUserDNNull");
    		}
    		
       		if (ndlSlice != null && ndlSlice.getSliceID() != null){
    			context.put("sliceID", ndlSlice.getSliceID());
    		} else {
    			context.put("sliceID", "SliceUserIDNull");
    		}
    		
    		
    		
    			
    		ArrayList<VM> all_vms = new ArrayList<VM>();
    		
    		
    		//Add groups to context
    		Collection<String> groups = reservations.group_getGroups();
    		//System.out.println("XXXXXX-PRUTH: ScriptConstructor: groups = " + groups);
    		Iterator<String> i_groups = groups.iterator();
    		while(i_groups.hasNext()){
    			String group_url = i_groups.next();
    			if(group_url.equals("NotInGroup"))
    				continue;
    			Group group = new Group(reservations.group_getName(group_url));
    			if(reservations.group_getVMsInGroup(group.name)==null){
    				logger.error("ScriptConstructor:    group="+group_url+" (" + reservations.group_getName(group_url) + ")");
    				continue;
    			}
    			Iterator<String> i_vms = reservations.group_getVMsInGroup(group.name).iterator();
    			while(i_vms.hasNext()){
    				String vm_url = i_vms.next();
    				//System.out.println("XXXXXX-PRUTH: ScriptConstructor (group): processing vm " + vm_url +  " (" + reservations.vm_getName(vm_url) + ")");
    				VM vm = new VM(reservations, reservations.vm_getName(vm_url));
    				Iterator<String> i_links = reservations.vm_getAllLinks(vm_url).iterator();
    				vm.group = reservations.group_getName(group_url);
    				while(i_links.hasNext()){
    					String link_url = i_links.next();
    					//System.out.println("XXXXXX-PRUTH: ScriptConstructor (group): processing link " + link_url +  " (" + reservations.link_getName(link_url,vm_url) + ")");
    					vm.ips.put(reservations.link_getName(link_url,vm_url), reservations.vm_getIfaceIP(vm_url, link_url));
    					vm.macs.put(reservations.link_getName(link_url,vm_url), reservations.vm_getIfaceMAC(vm_url, link_url));
    					vm.links.add(reservations.link_getName(link_url,vm_url));
    				}
    				//Add vm to group
    				group.vms.add(vm);

    				//Add vm to list of all vms
    				vm_table.put(vm_url, vm);
    				all_vms.add(vm);

    				//Add vm to context
    				//System.out.println("XXXXXX-PRUTH: ScriptConstructor (group): " + vm.name + "\n" + vm.toString());
    				context.put(vm.name, vm);
    			}    
    			//Add group to context
    			//System.out.println("XXXXXX-PRUTH: ScriptConstructor: " + group.name + "\n" + group.vms.toString());
    			context.put(group.name, group.vms);
    		}

    		//Add individual vms to context
    		if(reservations.vm_getVMsNotInGroup()!=null){	
    			Iterator<String> i_vms = reservations.vm_getVMsNotInGroup().iterator();
    			while(i_vms.hasNext()){
    				String vm_url = i_vms.next();
    				//System.out.println("XXXXXX-PRUTH: ScriptConstructor (individual): processing vm " + vm_url +  " (" + reservations.vm_getName(vm_url) + ")");
    				VM vm = new VM(reservations, reservations.vm_getName(vm_url));
    				Iterator<String> i_links = reservations.vm_getAllLinks(vm_url).iterator();
    				while(i_links.hasNext()){
    					String link_url = i_links.next();
    					//System.out.println("XXXXXX-PRUTH: ScriptConstructor (individual): processing link " + link_url+  " (" + reservations.link_getName(link_url,vm_url) + ")");
    					vm.ips.put(reservations.link_getName(link_url,vm_url), reservations.vm_getIfaceIP(vm_url, link_url));
    					vm.macs.put(reservations.link_getName(link_url,vm_url), reservations.vm_getIfaceMAC(vm_url, link_url));
    					vm.links.add(reservations.link_getName(link_url,vm_url));
    				}
    				//Add vm to list of all vms
    				vm_table.put(vm_url, vm);
    				all_vms.add(vm);

    				//Add vm to context
    				//System.out.println("XXXXXX-PRUTH: ScriptConstructor (individual): " + vm.name + "\n" + vm.toString());
    				context.put(vm.name, vm);
    			}    
    		}	
    		
			//put list of all vms in context
			//System.out.println("XXXXXX-PRUTH: ScriptConstructor: " + "vms" + "\n" + vm_table.values().toString());
			context.put("vms", all_vms);
    		
    	} catch (Exception e) {
    		//System.out.println(e);
    		e.printStackTrace();
    	}  
    }
    
    public String constructScript(String template_str, String vm_url) //throws Exception 
    {
    	
    	if (context == null){
    		logger.debug("ScriptConstructor has null context");
    		return null;
    	}

    	VelocityContext working_context = (VelocityContext)context.clone();
    	
    	//System.out.println("XXXXXX-PRUTH: constructScript vm_url: " + vm_url);
    	//System.out.println("XXXXXX-PRUTH: constructScript template: " + template_str);
    
    	//System.out.println("XXXXXX-PRUTH: ScriptConstructor: vm_table: keys: " + vm_table.keySet().toString() + ".  values: " +  vm_table.values().toString());
    	//System.out.println("XXXXXX-PRUTH: ScriptConstructor: vm_table: " + vm_table.toString());
    	VM vm = vm_table.get(vm_url);
    	
    	
    	//System.out.println("XXXXXX-PRUTH: ScriptConstructor: vm: " + vm.toString());
    	
    	working_context.put("self", vm);
   
        /*  
         * Get the template file from the URL and copy it to a local tmp file.  
         * Velocity.getTemplate() seems to require the file to be in the local working dir.  So, no absolute paths allowed.  
         * Maybe this will be fixed in a newer version of Velocity
         * 
         * */
    	String tmpFileName = TEMPLATE_FILE_TMP + UUID.randomUUID().toString();
    	File tmpFile = new File(tmpDir + System.getProperty("file.separator") + tmpFileName);
        try {
            //URL templateURL = new URL("http://geni-test.renci.org/templates/pruth-template.vm");
            //URL templateURL = new URL(templateURL_str);
            //BufferedReader in = new BufferedReader(new InputStreamReader(templateURL.openStream()));

            BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile)); 

            out.write(template_str);
            out.newLine();
            
            out.flush();
            out.close();
        } catch (Exception e) {
            //Couldn't open file ** probably should re-throw exception **
            return null;
        }

        StringWriter writer = new StringWriter();
        
        try{
            /*                                                                                                                   
             *  get the Template object.  This is the parsed version of your                                                     
             *  template input file.  Note that getTemplate() can throw                                                          
             *   ResourceNotFoundException : if it doesn't find the template                                                     
             *   ParseErrorException : if there is something wrong with the VTL                                                  
             *   Exception : if something else goes wrong (this is generally                                                     
             *        indicative of as serious problem...)                                                                       
             */

            Template template = null;
            String templateFile = tmpFileName;
            try {
                template = Velocity.getTemplate(templateFile);

            } catch (ResourceNotFoundException rnfe) {
                logger.error("Error, cannot find template " + templateFile);
            } catch (ParseErrorException pee) {
                logger.error("Syntax error in template " + templateFile + ":" + pee);
            }

            /*                                                                                                                   
             *  Now have the template engine process your template using the                                                     
             *  data placed into the context.  Think of it as a  'merge'                                                         
             *  of the template and the data to produce the output stream.                                                       
             */

            try{

               if (template != null) {
                   template.merge(working_context, writer);
               }

            } catch (Exception e){
            	logger.error("Exception while processing bootscript: " + vm_url);
            	logger.error("template_str: " + template_str);
            	logger.error(e);	
            }
            /*                                                                                                                   
             *  flush and cleanup                                                                                                
             */

            writer.flush();
            //writer.close();
            working_context = null;
            
            tmpFile.delete();
            
        } catch (Exception e) {
            logger.error(e);
        }
        
        logger.error(writer.toString());
        return writer.toString();

    }
 
    public void remove_vm(String name){
    	this.reservations.remove_vm(name);
    }
    
    public static void main(String args[]) throws Exception {
        /*
    	System.out.println("ScriptConstructor: main");
        System.out.println("Template: ");
        System.out.println(getTemplate());
        System.out.println("");
        System.out.println("");
        System.out.println("IPs:");
        System.out.println(getIPs());
        
        
        System.out.println("");
        System.out.println("");
        System.out.println("Output:");
        System.out.println(ScriptConstructor.constructScript(getTemplate(), getIPs(), 0));
        System.out.println("ScriptConstructor: done");
        */
    	String tmpDir = "/tmp";
    	String tmpFileName = tmpDir + System.getProperty("file.separator") + TEMPLATE_FILE_TMP + UUID.randomUUID().toString();
    	File tmpFile = new File(tmpFileName);
        try {
            //URL templateURL = new URL("http://geni-test.renci.org/templates/pruth-template.vm");
            //URL templateURL = new URL(templateURL_str);
            //BufferedReader in = new BufferedReader(new InputStreamReader(templateURL.openStream()));

            BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile)); 

        } catch (Exception e) {
            //Couldn't open file ** probably should re-throw exception **
        	System.err.println("ERROR: " + e);
        }
        
    }

    // Extra test functions
    private static Map<String,ArrayList<String>> getIPs() {
        /*
        Map<String,ArrayList<String>> scriptMap = new HashMap<String,ArrayList<String>>();
        ArrayList<String> list; // = new ArrayList<String>();

        scriptMap.put("CondorMaster", new ArrayList<String>());
        scriptMap.get("CondorMaster").add("172.16.42.100");
        
        scriptMap.put("CondorWorkers", new ArrayList<String>());
        scriptMap.get("CondorWorkers").add("172.16.42.200");
        scriptMap.get("CondorWorkers").add("172.16.42.201");
        scriptMap.get("CondorWorkers").add("172.16.42.202");
        scriptMap.get("CondorWorkers").add("172.16.42.203");
     

        return scriptMap;
        */
    	return null;
    }
    
    private static String getTemplate(){
        String s = "";
      
        
        s = s.concat("## Licensed to the Apache Software Foundation (ASF) under one" + "\n");
        s = s.concat("## or more contributor license agreements.  See the NOTICE file" + "\n");
        s = s.concat("## distributed with this work for additional information" + "\n");
        s = s.concat("##  regarding copyright ownership.  The ASF licenses this file" + "\n");
        s = s.concat("## to you under the Apache License, Version 2.0 (the" + "\n");
        s = s.concat("## \"License\" you may not use this file except in compliance" + "\n");
        s = s.concat("## with the License.  You may obtain a copy of the License at" + "\n");
        s = s.concat("##" + "\n");
        s = s.concat("##   http://www.apache.org/licenses/LICENSE-2.0" + "\n");
        s = s.concat("##" + "\n");
        s = s.concat("## Unless required by applicable law or agreed to in writing," + "\n");
        s = s.concat("## software distributed under the License is distributed on an" + "\n");
        s = s.concat("## \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY" + "\n");
        s = s.concat("## KIND, either express or implied.  See the License for the" + "\n");
        s = s.concat("## specific language governing permissions and limitations" + "\n");
        s = s.concat("## under the License.    " + "\n");
        s = s.concat("   # Test script " + "\n");
        s = s.concat("   echo \"hello from neuca script\"" + "\n");
        s = s.concat("   mkdir -p /opt/pegasus      " + "\n");
        s = s.concat("   echo 'condor-nfs:/var/nfs /opt/pegasus nfs vers=3,proto=tcp,hard,intr,timeo=600,retrans=2,wsize=32768,rsize=32768 0 0' >> /etc/fstab" + "\n");
        s = s.concat("   $CondorMaster.get(0) condor-master >> /etc/hosts" + "\n");
        s = s.concat("#set( $nameList = [ \"condor-w1\", \"condor-w2\",\"condor-w3\", \"condor-w4\" ])" + "\n");
        s = s.concat("#set( $i = 0 )" + "\n");
        s = s.concat("#foreach( $name in $nameList )" + "\n");
        s = s.concat("   $CondorWorkers.get($i) $name >> /etc/hosts" + "\n");
        s = s.concat("   #set($i=$i+1)" + "\n");
        s = s.concat("#end" + "\n");
        s = s.concat("   echo condor-w1 > /etc/hostname" + "\n");
        s = s.concat("   /bin/hostname -F /etc/hostname" + "\n");
        s = s.concat("   #mount -a" + "\n");
        
        
        return s;
    }
}
