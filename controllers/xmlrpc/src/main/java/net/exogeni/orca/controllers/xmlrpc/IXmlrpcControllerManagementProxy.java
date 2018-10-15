/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.exogeni.orca.controllers.xmlrpc;

import net.exogeni.orca.security.AuthToken;

/**
 *
 * @author anirban
 */
public interface IXmlrpcControllerManagementProxy {

    public void disableController(AuthToken auth) throws Exception;

    public void enableController(AuthToken auth) throws Exception;

    public boolean isControllerEnabled(AuthToken auth) throws Exception;

}
