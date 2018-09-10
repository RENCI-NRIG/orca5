/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.exogeni.orca.shirako.container;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author anirban
 */
public class OrcaState {

    private static final OrcaState instance = new OrcaState();
    private static File orcaStateFile = null;

    private OrcaState() {

    }

    public static OrcaState getInstance() {
            return instance;
    }

    public void createStateFile() throws IOException{

        File tmpDir = new File("/tmp");
        if(orcaStateFile != null){
            if(orcaStateFile.exists()){
                System.out.println("orca_running.tmp state file already exists; Possibly due to unclean shutdown");
                orcaStateFile.delete();
                orcaStateFile = File.createTempFile("orca_running", ".tmp", tmpDir);
            }
            else{
                orcaStateFile = File.createTempFile("orca_running", ".tmp", tmpDir);
            }
        }
        else {
            orcaStateFile = File.createTempFile("orca_running", ".tmp", tmpDir);
        }
        
    }

    public void deleteStateFile() throws IOException{

        if(orcaStateFile != null){
            if(orcaStateFile.exists()){
                orcaStateFile.delete();
            }
            else{
                System.out.println("Could not delete net.exogeni.orca state file");
            }
        }
        else{
            System.out.println("Could not delete net.exogeni.orca state file");
        }

    }

    public boolean checkOrcaRunning(){
        if(orcaStateFile != null){
            if(orcaStateFile.exists()){
                return true;
            }
            else {
                return false;
            }
        }
        else{
            return false;
        }
    }

}
