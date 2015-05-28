/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.client;

import java.io.FileOutputStream;

import orca.nodeagent.NodeAgentServiceStub;
import orca.nodeagent.documents.GetServiceKeyElement;
import orca.nodeagent.documents.GetServiceKeyResultElement;
import orca.nodeagent.drivers.DriverBaseTask;

import org.apache.axis2.util.Base64;
import org.apache.tools.ant.BuildException;


public class GetServiceKeyTask extends DriverBaseTask
{
    /**
     * Property to store the resulting certificate (base64-encoded)
     */
    protected String certificateProperty;

    /**
     * File to store the resulting certificate
     */
    protected String certificateFile;

    public void execute() throws BuildException
    {
        super.execute();

        try {
            if ((certificateProperty == null) && (certificateFile == null)) {
                throw new Exception("Must speciffy an output");
            }

            GetServiceKeyElement gske = new GetServiceKeyElement();

            NodeAgentServiceStub stub = getStub();
            GetServiceKeyResultElement gskre = stub.getServiceKey(gske);

            int retCode = gskre.getCode();

            if (retCode == 0) {
                byte[] encodedCert = gskre.getKey();

                if (certificateProperty != null) {
                    String result = Base64.encode(encodedCert);
                    getProject().setProperty(certificateProperty, result);
                } else {
                    FileOutputStream fs = new FileOutputStream(certificateFile);
                    fs.write(encodedCert);
                    fs.close();
                }
            }

            setResult(retCode);
        } catch (Exception e) {
            logger.error("", e);
            throw new BuildException("An error occurred: ", e);
        }
    }

    public void setCertificateProperty(String value)
    {
        this.certificateProperty = value;
    }

    public void setCertificateFile(String value)
    {
        this.certificateFile = value;
    }
}