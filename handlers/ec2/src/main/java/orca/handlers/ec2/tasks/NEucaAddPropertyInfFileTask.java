package orca.handlers.ec2.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import orca.shirako.common.meta.UnitProperties;
import orca.shirako.common.meta.ConfigurationProperties;
import orca.shirako.plugins.config.OrcaAntTask;

import org.apache.tools.ant.BuildException;

import org.ini4j.Ini;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.StringReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class NEucaAddPropertyInfFileTask extends OrcaAntTask{
    protected String file;
    protected String cloudType;
    protected String outputProperty;
    protected String userdataOld;
    protected String userdataNew;

    protected String section = null;
    protected String key = null;
    protected String value = null;
    
    /*
    private void addProperty_Ini(){
        System.out.println("PRUTH: this.section = " + this.section + ", this.key = " + this.key + ", this.value = " + this.value);

        Ini ini = null;
        if (this.file.equals(this.userdataOld)){
            System.out.println("PRUTH: reading userdata from file " + this.file);
            ini = new Ini(new File(this.file));
        } else {
            System.out.println("PRUTH: using userdata from userdataOld string");
            ini = new Ini(new ByteArrayInputStream(this.userdataOld.getBytes()));
	}

        if(this.section != null && this.key != null && this.value != null){
            System.out.println("PRUTH: adding this.section = " + this.section + ", this.key = " + this.key + ", this.value = " + this.value);
            ini.put(this.section,this.key,this.value);
	} else {
            System.out.println("PRUTH: skipping add");
        }


        ini.store(new File(file));

    }
    */
/*
    private void addProperty(){
	System.out.println("PRUTH: this.section = " + this.section + ", this.key = " + this.key + ", this.value = " + this.value);

	if (this.file.equals(this.userdataOld)){
	    System.out.println("PRUTH: reading userdata from file " + this.file);
	    ini = new Ini(new File(this.file));
	} else {
	    System.out.println("PRUTH: using userdata from userdataOld string");
	    ini = new Ini(new ByteArrayInputStream(this.userdataOld.getBytes()));
	}
	
	if(this.section != null && this.key != null && this.value != null){
	    System.out.println("PRUTH: adding this.section = " + this.section + ", this.key = " + this.key + ", this.value = " + this.value);
	    ini.put(this.section,this.key,this.value);
	} else {
	    System.out.println("PRUTH: skipping add");
	}


	ini.store(new File(file));

    }
 */

    public void execute() throws BuildException {
		
	try {
            super.execute();
            if (file == null) {
                throw new Exception("Missing file parameter");
            }
	    	if (cloudType == null) {
				throw new Exception("Missing cloudType parameter");
	    	}
            
	    

	        System.out.println("file: " + file + ", cloudType: " +cloudType);
	  

	    System.out.println("PRUTH: this.section = " + this.section + ", this.key = " + this.key + ", this.value = " + this.value);
	    BufferedReader userdataSource;
	    if (this.file.equals(this.userdataOld)){
                System.out.println("PRUTH: reading userdata from file " + this.file);
		userdataSource = new BufferedReader(new FileReader(this.file)); 
	    } else {
		System.out.println("PRUTH: using userdata from userdataOld string");
                userdataSource = new BufferedReader(new StringReader(this.userdataOld));
            }

  
	    System.out.println("PRUTH: this.section = " + this.section + ", this.key = " + this.key + ", this.value = " + this.value);
	    String userdataNew = "";
	    //if(this.section != null && this.key != null && this.value != null){
		System.out.println("PRUTH: adding this.section = " + this.section + ", this.key = " + this.key + ", this.value = " + this.value);
		try {
		    StringBuilder sb = new StringBuilder();
		    String line = userdataSource.readLine();
		    
		    String section = "";
		    String prev_section = "";
		    String key = "";
		    
		    //remove the colons from the mac address
		    if(this.section != null && this.section.equals("interfaces")){
			this.key = this.key.replace(":","").trim();
		    }
		    
		    boolean processedKey = false;
		    while (line != null) {
			System.out.println("PRUTH: line = " + line);

			//if there is nothing to add, just copy lines
			if(this.section == null || this.key == null || this.value == null){
			    sb.append(line);
			    sb.append(System.lineSeparator());
			    line = userdataSource.readLine();
			    processedKey = true;
			    continue;
			}

			Pattern pattern = Pattern.compile("^\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()){
			    prev_section=section;
			    section=matcher.group(1);
			    System.out.println("found section " + section);
			}
			
			//check to see if we checked all the existing interfaces.  if so, add the new one.
			if(!processedKey && prev_section.equals(this.section)){
			    System.out.println("Adding new key to " + prev_section + " section of userdata: " + this.key.trim());
			    sb.append(this.key.trim() + "=" +  this.value.trim() + "\n");
			    processedKey = true;
			} 
			
			key=line.split("=")[0].trim();
			System.out.println("processing key: " + key);
			//find key 
			
			if (section.equals("users")){
			    if(key.equals(this.key)){
				sb.append(line.trim() +  this.value.trim() + ":\n");
			    } else {
				sb.append(line);
			    }
			} else if (section.equals("interfaces")){
			    if(!processedKey && key.equals(this.key)){
				//modify an existing interface
				System.out.println("Modifying interface ing userdata: " + key);
				sb.append(this.key.trim() + "=" +  this.value.trim() + "\n");
			    } else {
				System.out.println("Ignoring interface in userdata: " + key);
				sb.append(line);
			    }
		        } else {
			    sb.append(line);
			}

			sb.append(System.lineSeparator());
			line = userdataSource.readLine();
		    }

		    if(!processedKey){
			System.out.println("Adding section interfaces userdata and adding interface: " + key);
			sb.append("[interfaces]");
			sb.append(this.key.trim() + " = " +  this.value.trim() + "\n");
			processedKey=true;
		    }

		    userdataNew = sb.toString();
		} finally {
		    userdataSource.close();
		}
		//}
	    userdataSource.close();

	    PrintWriter out = new PrintWriter(new FileWriter(new File(file)));
	    out.print(userdataNew);
	    out.close();


	    if(outputProperty != null)
		getProject().setProperty(this.outputProperty,userdataNew);
	    
  
	} catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }


    public void setFile(String file) {
        this.file = file;
    }

    public void setCloudtype(String cloudtype) {
        this.cloudType = cloudtype;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setOutputproperty(String outputproperty) {
        this.outputProperty = outputproperty;
    }

    public void setUserdataold(String userdataold) {
        this.userdataOld = userdataold;
    }

    
}
