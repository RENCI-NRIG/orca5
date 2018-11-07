package orca.tools.build;

import org.apache.tools.ant.Task;

import java.util.Hashtable;

public class DumpPropertiesTask extends Task {
    @Override
    public void execute() {
        Hashtable t = getProject().getProperties();

        for (Object o : t.keySet()) {
            Object v = t.get(o);
            System.out.println(o + "=" + v);
        }
    }
}
