package orca.handlers.ec2.tasks;

import orca.shirako.plugins.config.OrcaAntTask;
import org.apache.tools.ant.BuildException;


public class NEucaCometDataTask extends OrcaAntTask {
    protected String outputProperty;
    protected String operation;
    protected String section = null;
    protected String key = null;
    protected String value = null;

    public static String OperationGenerate = "generate";
    public static String OperationAdd = "add";
    public static String OperationDelete = "delete";

    private void generate() throws Exception{
        NEucaGenerateCometData generator = new NEucaGenerateCometData(getProject());
        generator.doIt();
        if (outputProperty != null)
            getProject().setProperty(outputProperty, generator.getOutputProperty());
    }

    private void add() throws Exception {
        System.out.println("NEucaCometDataProcessor::execute: section = " + section + ", key = " + key
                + ", " + "value = " + value);

        // remove the colons from the mac address
        if (section != null && section.equals("interfaces")) {
            key = key.replace(":", "").trim();
        }

        if (section == null || key == null || value == null) {
            // Nothing to do; no change to UserData
            System.out.println("NEucaCometDataProcessor::execute:Nothing to do!");
        }
        else {
            NEucaCometDataProcessor processor = new NEucaCometDataProcessor(getProject());
            processor.add(section, key, value);
        }
    }

    private void delete() throws Exception {
        System.out.println("NEucaCometDataTask::execute: remove section = " + section + ", key = " + key);

        if (section == null || key == null) {
            // Nothing to do; no change to UserData
            System.out.println("NEucaCometDataTask::execute:Nothing to do!");
        }
        else {
            // remove the colons from the mac address
            if (section != null && section.equals("interfaces")) {
                key = key.replace(":", "").trim();
            }
            NEucaCometDataProcessor processor = new NEucaCometDataProcessor(getProject());
            processor.delete(section, key);
        }
    }

    public void execute() throws BuildException {
        System.out.println("NEucaCometDataTask::execute: IN");
        try {
            super.execute();
            if (operation == null) {
                throw new Exception("Missing operation parameter");
            }
            if(operation.compareToIgnoreCase(OperationGenerate) == 0) {
                generate();
            }
            else if(operation.compareToIgnoreCase(OperationAdd) == 0) {
                add();
            }
            else if(operation.compareToIgnoreCase(OperationDelete) == 0) {
                delete();
            }
            else {
                throw new BuildException("Unsupported operation");
            }

        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
        System.out.println("NEucaCometDataTask::execute: IN");
    }

    public void setOperation(String operation) {
        this.operation = operation;
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

}
