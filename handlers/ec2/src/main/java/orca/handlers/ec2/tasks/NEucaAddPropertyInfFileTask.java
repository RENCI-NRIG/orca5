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


/*
public class  NEucaInfFileGenerator { 
    org.apache.tools.ant.Project project;

    protected org.apache.tools.ant.Project getProject(){
        return project;
    }

    abstract public void doIt(PrintWriter out) throws Exception;

    abstract public String getOutputProperty();

    protected String sanitizeBootScript(String script) {
        if (script != null)
	    return script.replaceAll("\n[\\s]*\n", "\n").replaceAll("\n", "\n\t");
        return script;
    }
    
}
*/


public class NEucaAddPropertyInfFileTask extends OrcaAntTask{
    protected String file;
    protected String cloudType;
    protected String outputProperty;
    protected String userdataOld;
    protected String userdataNew;

    protected String section = null;
    protected String key = null;
    protected String value = null;


    public void execute() throws BuildException {
		
	try {
            super.execute();
            if (file == null) {
                throw new Exception("Missing file parameter");
            }
	    if (cloudType == null) {
		throw new Exception("Missing cloudType parameter");
	    }
            //PrintWriter out = new PrintWriter(new FileWriter(new File(file)));

            System.out.println("file: " + file + ", cloudType: " +cloudType);
         


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


	    //if(outputProperty != null)
	    //   getProject().setProperty(userdataNew);
	    
  
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
