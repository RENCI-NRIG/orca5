/**
 * (c) Copyright 2008, RENCI
 * All rights reserved.
 * [See end of file]
 * $Id: NdlCreator.java,v 1.0 2008/12/12 09:30:07 der Exp $

 */

package orca.network;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.*;

import java.io.PrintWriter;

import org.apache.log4j.Logger;

import orca.network.policyhelpers.JniLoader;
import orca.network.policyhelpers.SystemNativeError;

public class NdlCreator {

    public native void print();

    public native SystemNativeError system(String commandline);

    private static final Logger log = Logger.getLogger(Ndl.class);

    public NdlCreator() {
        JniLoader loader = new JniLoader("syscall");

        if (loader != null) {
            SystemNativeError error = loader.loadJni();
            String message = "Error loading JNI: " + error.getMessage() + " (" + error.getErrno() + ")";

            if (error.getAdditional() != null && !error.getAdditional().isEmpty())
                message += ": " + error.getAdditional();
            if (error.getErrno() != 0)
                log.error(message);
        }
    }

    public void create() {
        System.out.println("From the C++\n");
        print();
        // create an empty graph
        Model model = ModelFactory.createDefaultModel();

        // create the resource
        Resource r = model.createResource();

        // add the property
        r.addProperty(RDFS.label, model.createLiteral("chat", "en"))
                .addProperty(RDFS.label, model.createLiteral("chat", "fr"))
                .addProperty(RDFS.label, model.createLiteral("<em>chat</em>", true));

        r = model.createResource();

        r.addProperty(RDFS.label, "11").addLiteral(RDFS.label, 11);

        // write out the graph
        model.write(new PrintWriter(System.out));

    }
}
