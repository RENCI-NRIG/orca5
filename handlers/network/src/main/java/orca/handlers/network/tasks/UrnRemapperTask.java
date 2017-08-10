package orca.handlers.network.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import orca.shirako.plugins.config.OrcaAntTask;

import org.apache.tools.ant.BuildException;

/**
 * Remap URNs based on a property file
 * For now the property file has one property with an even number
 * of comma-separated URNs. The odd-ones are 'from', the even ones are 'to'
 * @author ibaldin
 *
 */
public class UrnRemapperTask extends OrcaAntTask {
	String oldUrn, newUrnProp, mapFile, mapProp;
	
	public void setOldUrn(String fu) {
		oldUrn = fu;
	}
	
	public void setNewUrnProp(String tu) {
		newUrnProp = tu;
	}
	
	public void setMapFile(String mf) {
		mapFile = mf;
	}
	
	public void setMapProperty(String mp) {
		mapProp = mp;
	}
	
	public void execute() throws BuildException {
		try {
            super.execute();
            if (mapFile == null) 
            	throw new BuildException("No URN mapping file specified");
            
            File props = new File(mapFile);
			FileInputStream is = new FileInputStream(props);
			BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			Properties prop = new Properties();
			prop.load(bin);
			bin.close();
            
            // extrac map property, parse, make a map, return result
            if (mapProp == null)
            	throw new BuildException("No map property name specified");
            
            String map = prop.getProperty(mapProp);
            
            if ((map == null) || (map.length() == 0)){
            	System.out.println("No " + mapProp + " was found in " + mapFile + ", noremapping will be done");
            	getProject().setProperty(newUrnProp, oldUrn.trim());
            	return;
            }
            
            String[] maps = map.trim().split(",");
            
            if (maps.length %2 != 0)
            	throw new BuildException("Map property contains odd number of entries");
            
            Map<String, String> realMap = new HashMap<String, String>();
            for(int i = 0; i < maps.length;) {
            	realMap.put(maps[i++], maps[i++]);
            }
            
            if (realMap.containsKey(oldUrn.trim()))
            	getProject().setProperty(newUrnProp, realMap.get(oldUrn.trim()));
            else
            	getProject().setProperty(newUrnProp, oldUrn.trim());
		} catch (BuildException e) {
			System.out.println("BuildException in mapping urn: " + e);
            throw e;
        } catch (Exception e) {
        	System.out.println("Exception in mapping urn: " + e);
            throw new BuildException("An error occurred mapping urn: " + e.getMessage(), e);
        }
	}
}
