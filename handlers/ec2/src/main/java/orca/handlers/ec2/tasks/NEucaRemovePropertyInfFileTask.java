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


public class NEucaRemovePropertyInfFileTask extends OrcaAntTask{
    protected String file;
    protected String cloudType;
    protected String outputProperty;
    protected String userdataOld;
    protected String userdataNew;

    protected String section = null;
    protected String key = null;

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
	  

	    System.out.println("PRUTH: remove this.section = " + this.section + ", this.key = " + this.key );
	    BufferedReader userdataSource;
	    if (this.file.equals(this.userdataOld)){
                System.out.println("PRUTH: reading userdata from file " + this.file);
		userdataSource = new BufferedReader(new FileReader(this.file)); 
	    } else {
		System.out.println("PRUTH: using userdata from userdataOld string");
                userdataSource = new BufferedReader(new StringReader(this.userdataOld));
            }

  
	    System.out.println("PRUTH: this.section = " + this.section + ", this.key = " + this.key);
	    String userdataNew = "";
	    System.out.println("PRUTH: removeing this.section = " + this.section + ", this.key = " + this.key);
		try {
		    StringBuilder sb = new StringBuilder();
		    String line = userdataSource.readLine();
		    
		    String section = "";
		    String prev_section = "";
		    		    
		    //remove the colons from the mac address
		    if(this.section != null && this.section.equals("interfaces")){
			this.key = this.key.replace(":","").trim();
		    }
		    
		    boolean processedKey = false;
		    while (line != null) {
			System.out.println("PRUTH: line = " + line);

			//if there is nothing to add, just copy lines
			if(this.section == null || this.key == null ){
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
			    sb.append(line);
                            sb.append(System.lineSeparator());
			    System.out.println("found section " + section);
			    line = userdataSource.readLine();
			    continue;
			}
			
			//check to see if we checked all the existing interfaces.  if so, add the new one.
			key=line.split("=")[0].trim();
			//value=line.split("=")[1].trim();
			System.out.println("processing line:  key: " + key + ", section: " + section);
						
			//skip if we find a match... remember we are removing the key/value
			if(! (section.equals(this.section) && key.equals(this.key))){
			    //modify an existing interface
			    System.out.println("Keeping line: " + line);
			    sb.append(line);
			    sb.append(System.lineSeparator());
			}  else {
			    System.out.println("Deleting line: " + line);
			}

			line = userdataSource.readLine();
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


    public void setUserdataold(String userdataold) {
        this.userdataOld = userdataold;
    }

    
}
