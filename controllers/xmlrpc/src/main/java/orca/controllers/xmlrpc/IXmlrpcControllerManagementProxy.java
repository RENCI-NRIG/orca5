/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc;


import orca.security.AuthToken;

/**
 *
 * @author anirban
 */
public interface IXmlrpcControllerManagementProxy {

    public void disableController(AuthToken auth) throws Exception;

    public void enableController(AuthToken auth) throws Exception;

    public boolean isControllerEnabled(AuthToken auth) throws Exception;

}
