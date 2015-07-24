/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.ben.interdomain;

import orca.controllers.ben.interdomain.InterDomainControllerManagerObject.ResultInterDomainRequestMng;
import orca.manage.extensions.api.beans.ResultMng;
import orca.manage.extensions.api.beans.ResultStringMng;
import orca.security.AuthToken;

/**
 *
 * @author anirban
 */
public interface IInterDomainControllerManagementProxy {

    ResultStringMng addRequest(String ndl, String start, String end, AuthToken caller) throws Exception;

    ResultInterDomainRequestMng getRequests(AuthToken caller) throws Exception;

    ResultInterDomainRequestMng getRequests(String id, AuthToken caller) throws Exception;

    ResultMng closeRequest(String id, AuthToken caller) throws Exception;

}
