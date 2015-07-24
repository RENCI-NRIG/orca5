/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent;

import java.util.Properties;

import javax.activation.DataHandler;

import orca.drivers.DriverId;
import orca.drivers.util.DriverScriptExecutionResult;
import orca.nodeagent.documents.DriverElement;
import orca.nodeagent.documents.DriverObjectRequestElement;
import orca.nodeagent.documents.DriverRequestElement;
import orca.nodeagent.documents.GetServiceKeyElement;
import orca.nodeagent.documents.GetServiceKeyResultElement;
import orca.nodeagent.documents.RegisterAuthorityKeyElement;
import orca.nodeagent.documents.RegisterAuthorityKeyResultElement;
import orca.nodeagent.documents.RegisterKeyElement;
import orca.nodeagent.documents.RegisterKeyResultElement;
import orca.nodeagent.documents.ResultElement;
import orca.nodeagent.documents.ScriptElement;
import orca.nodeagent.documents.ScriptResultElement;
import orca.nodeagent.documents.TestFuncElement;
import orca.nodeagent.documents.TestFuncResultElement;
import orca.nodeagent.documents.UnregisterKeyElement;
import orca.nodeagent.documents.UnregisterKeyResultElement;
import orca.nodeagent.util.Serializer;

import org.apache.log4j.Logger;


public class NodeAgentServiceSkeleton
{
    protected NodeAgentService service = NodeAgentService.getInstance();
    protected Logger logger = service.getLogger();

    public NodeAgentServiceSkeleton()
    {
    }

    /**
     * Executes the specified script
     * @param arg
     * @return
     */
    public ScriptResultElement executeScript(ScriptElement arg)
    {
        logger.info("Received an executeScript request");

        String scriptName = arg.getScript();
        String arguments = arg.getArguments();

        if (scriptName == null) {
            logger.warn("No script name specified in request");

            return sendExecuteScriptResult(NodeAgentConstants.CodeInvalidArguments, "", "");
        }

        DriverScriptExecutionResult r = service.executeScript(scriptName, arguments);

        return sendExecuteScriptResult(r.code, r.stdout, r.stderr);
    }

    /**
     * Installs a driver.
     * @param arg
     * @return
     */
    public ResultElement installDriver(DriverElement arg)
    {
        logger.info("Received an install driver request");

        String className = arg.getClassName();
        String id = arg.getDriverId();

        // String path = arg.getPath();
        DataHandler pkg = arg.getPkg();

        if ((className == null) || (id == null)) {
            logger.warn("Invalid arguments");

            return sendResult(NodeAgentConstants.CodeInvalidArguments, null);
        }

        int code = service.installDriver(new DriverId(id), className, pkg);

        return sendResult(code, null);
    }

    /**
     * Upgrades an already installed driver
     * @param arg
     * @return
     */
    public ResultElement upgradeDriver(DriverElement arg)
    {
        logger.info("Received an upgrade driver request");

        String className = arg.getClassName();
        String id = arg.getDriverId();

        // String path = arg.getPath();
        DataHandler pkg = arg.getPkg();

        if ((className == null) || (id == null)) {
            logger.warn("Invalid arguments");

            return sendResult(NodeAgentConstants.CodeInvalidArguments, null);
        }

        int code = service.upgradeDriver(new DriverId(id), className, pkg);

        return sendResult(code, null);
    }

    /**
     * Uninstalls an installed driver
     * @param arg
     * @return
     */
    public ResultElement uninstallDriver(DriverElement arg)
    {
        logger.info("Received an uninstall driver request");

        String id = arg.getDriverId();

        if (id == null) {
            logger.warn("Invalid arguments");

            return sendResult(NodeAgentConstants.CodeInvalidArguments, null);
        }

        int code = service.uninstall(new DriverId(id));

        return sendResult(code, null);
    }

    /**
     * Invokes an action on a driver
     * @param arg
     * @return
     */
    public ResultElement executeDriver(DriverRequestElement arg)
    {
        logger.info("Received an executeDriver request");

        String strDriverId = arg.getDriverId();
        String actionId = arg.getActionId();
        Properties p = Serializer.serialize(arg.getProperties());

        if ((strDriverId == null) || (actionId == null)) {
            logger.warn("Invalid arguments");

            return sendResult(NodeAgentConstants.CodeInvalidArguments, null);
        }

        DriverId driverId = new DriverId(strDriverId);
        Properties out = new Properties();

        int result = service.executeDriver(driverId, actionId, p, out);

        return sendResult(result, out);
    }

    /**
     * Invokes an action on a driver
     * @param arg
     * @return
     */
    public ResultElement executeObjectDriver(DriverObjectRequestElement arg)
    {
        logger.info("Received an executeDriver request");

        String strDriverId = arg.getDriverId();
        String objectId = arg.getObjectId();
        String actionId = arg.getActionId();
        Properties p = Serializer.serialize(arg.getProperties());

        if ((strDriverId == null) || (actionId == null)) {
            logger.warn("Invalid arguments");

            return sendResult(NodeAgentConstants.CodeInvalidArguments, null);
        }

        DriverId driverId = new DriverId(strDriverId);
        Properties out = new Properties();

        int result;

        if (objectId == null) {
            result = service.executeDriver(driverId, actionId, p, out);
        } else {
            result = service.executeObjectDriver(driverId, objectId, actionId, p, out);
        }

        return sendResult(result, out);
    }

    /**
     * Ionut's test function
     * @param in - just an int
     * @return - just an int
     */
    public TestFuncResultElement testFunc(TestFuncElement in)
    {
        logger.debug("Param1 received in testFunc " + in.getTestFuncElement());

        int ret = in.getTestFuncElement() + 10;
        TestFuncResultElement trf = new TestFuncResultElement();
        trf.setTestFuncResultElement(ret);
        logger.debug("Value returned " + trf.getTestFuncResultElement());

        String t = orca.nodeagent.RootPathResolver.getRoot();
        logger.debug("RealBase : " + t);

        return trf;
    }

    /**
     * registers a public key/certificate with the server
     * @param rk
     * @return
     */
    public RegisterKeyResultElement registerKey(RegisterKeyElement rk)
    {
        logger.info("Received registerKey request");

        String alias = rk.getAlias();
        byte[] certificateEncoding = rk.getPublickey();

        int result = service.registerKey(alias, certificateEncoding);

        RegisterKeyResultElement rkr = new RegisterKeyResultElement();
        rkr.setRegisterKeyResultElement(result);

        return rkr;
    }

    /**
     * unregister a public key/certificate with the server
     * @param urk
     * @return
     */
    public UnregisterKeyResultElement unregisterKey(UnregisterKeyElement urk)
    {
        logger.info("Received an unregisterKey request");

        String alias = urk.getAlias();

        int result = service.unregisterKey(alias);

        UnregisterKeyResultElement urkr = new UnregisterKeyResultElement();
        urkr.setUnregisterKeyResultElement(result);

        return urkr;
    }

    /**
     * register the first key/certificate with the server
     * @param rfk
     * @return
     */

    /*
     * public RegisterFirstKeyResultElement
     * registerFirstKey(RegisterFirstKeyElement rfk) { logger.info("Received
     * registerFirstKey request"); String alias = rfk.getAlias(); byte[]
     * certificateEncoding = rfk.getPublickey(); String password =
     * rfk.getPassword(); int result = service.registerFirstKey(password, alias,
     * certificateEncoding); RegisterFirstKeyResultElement rfkr = new
     * RegisterFirstKeyResultElement();
     * rfkr.setRegisterFirstKeyResultElement(result); return rfkr; }
     */

    /**
     * register the authority key with the service this should be the first
     * function called
     */
    public RegisterAuthorityKeyResultElement registerAuthorityKey(RegisterAuthorityKeyElement rak)
    {
        logger.info("Received registerAuthorityKey request");

        String alias = rak.getAlias();
        byte[] certificateEnconding = rak.getCertificate();
        byte[] message = rak.getRequest();
        byte[] signature = rak.getSignature();

        RegisterAuthorityKeyResultElement gskre = service.registerAuthorityKey(alias,
                                                                               certificateEnconding,
                                                                               message,
                                                                               signature);

        return gskre;
    }

    public GetServiceKeyResultElement getServiceKey(GetServiceKeyElement gske)
    {
        GetServiceKeyResultElement gskre = service.getServiceKey();

        return gskre;
    }

    /*
     * ====================================================================
     * Helper functions
     * ====================================================================
     */
    protected ResultElement sendResult(int code, Properties p)
    {
        ResultElement result = new ResultElement();
        result.setCode(code);
        result.setProperties(Serializer.serialize(p));

        return result;
    }

    protected ScriptResultElement sendExecuteScriptResult(int code, String stdout, String stderr)
    {
        String message = NodeAgentConstants.getMessage(code);
        ScriptResultElement result = new ScriptResultElement();
        result.setCode(code);
        result.setMessage(message);
        result.setStdOut(stdout);
        result.setStdError(stderr);

        return result;
    }
}