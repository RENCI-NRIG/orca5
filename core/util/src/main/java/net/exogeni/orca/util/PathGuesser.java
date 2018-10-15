/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.util;

import java.io.File;
import java.net.URL;


public class PathGuesser
{
    private static final String ORCA_HOME = "ORCA_HOME";
	private static final String ORCA_CONTROLLER_HOME = "ORCA_CONTROLLER_HOME";
	public static final String WEBINF = "/WEB-INF";
    public static final String Prefix = "/";
    public static final String TestFile = "path.properties";

    public static String getDefaultBase()
    {
        File f = new File(".");

        return f.getAbsolutePath() + "/";
    }

    public static String getLocalBase() {
        String base = getWebBase();
        if (base == null){
            base = getDefaultBase();
        }
        return base;
    }
    
    /**
     * Returns the web application's base directory.
     * @return string contatining wev application's base directory path
     */
    public static String getWebBase() {
        PathGuesser p = new PathGuesser();
        URL url = p.getClass().getResource(TestFile);
        if (url != null) {
            String temp = url.getPath();
            int index = temp.indexOf(WEBINF);
            if (index > -1) {
                temp = temp.substring(0, index) + "/WEB-INF/";
                // remove prefix:
                index = temp.indexOf(Prefix);
                if (index > 0) {
                    temp = temp.substring(index);
                }
                return temp;
            }
        }
        return null;
    }
    
    public static String getOrcaHome() {
        // first check if ORCA_HOME is defined as a system property
        String orcaHome = System.getProperty(ORCA_HOME);
        if (orcaHome == null){
            // next check if there is an environment variable ORCA_HOME
            orcaHome = System.getenv(ORCA_HOME);
            // if not see if this is a controller
            if (orcaHome == null) {
            	orcaHome = System.getProperty(ORCA_CONTROLLER_HOME);
            	if (orcaHome == null) 
            		orcaHome = System.getenv(ORCA_CONTROLLER_HOME);
            }
        } 
        
        // if none of the above, get the working directory
        if (orcaHome == null){
        	orcaHome = getWorkingDirectory();
        }
        
        if (!orcaHome.endsWith("/")){
            orcaHome += "/";
        }
        
        if (!orcaHome.startsWith("/")){
        	orcaHome = getWorkingDirectory() + orcaHome;
        }
        
        return orcaHome;
    }

    public static String getOrcaControllerHome() {
        // first check if ORCA_CONTROLLER_HOME is defined as a system property
        String orcaCtrlrHome = System.getProperty(ORCA_CONTROLLER_HOME);
        if (orcaCtrlrHome == null){
            // next check if there is an environment variable ORCA_CONTROLLER_HOME
            orcaCtrlrHome = System.getenv(ORCA_CONTROLLER_HOME);
        }
        
        if (orcaCtrlrHome == null){
        	orcaCtrlrHome = getWorkingDirectory();
        }
        
        if (!orcaCtrlrHome.endsWith("/")){
            orcaCtrlrHome += "/";
        }
        
        if (!orcaCtrlrHome.startsWith("/")){
        	orcaCtrlrHome = getWorkingDirectory() + orcaCtrlrHome;
        }
        
        return orcaCtrlrHome;
    }

    public static String getHomeDirectory() {
    	String dir = getOrcaHome();
    	if (dir == null) {
    		dir = getWorkingDirectory();    		
    	}
        if (!dir.endsWith("/")){
            dir += "/";
        }
        return dir;   
    }
    
    public static String getRealBase()
    {   
        String base = getOrcaHome();
        if (base == null){
            base = getWebBase();
        }
        if (base == null){
            base = getDefaultBase();
        }
        return base;
    }

    public static String getWorkingDirectory() {
    	return getDefaultBase();
    }
    
    public static String getConfigDirectory() {
    	String configDirectory = System.getProperty("ORCA_CONFIG");
    	if (configDirectory != null) {
    		if (!configDirectory.endsWith("/")) {
    			configDirectory += "/";
    		}
    		return configDirectory;
    	}
    	return getWorkingDirectory();
    }

    public static String getRuntimeDirectory() {
    	String dir = System.getProperty("ORCA_RUN");
    	if (dir != null) {
    		if (!dir.endsWith("/")) {
    			dir += "/";
    		}
    		return dir;
    	}
    	return getWorkingDirectory();
    }
}
