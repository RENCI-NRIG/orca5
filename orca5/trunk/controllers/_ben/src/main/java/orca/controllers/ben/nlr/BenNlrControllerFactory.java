/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.controllers.ben.nlr;

import orca.controllers.ben.BenController;
import orca.controllers.ben.BenControllerFactory;
import orca.controllers.ben.BenControllerManagerObject;

public class BenNlrControllerFactory extends BenControllerFactory
{
    @Override
    protected BenController makeController() {
        return new BenNlrController();
    }
    
    @Override
    protected BenControllerManagerObject makeManagerObject() {
        return new BenNlrControllerManagerObject(cont, actor);
    }
    
    @Override
    public String getPortalPluginClassName() {
        return BenNlrControllerPortalPlugin.class.getCanonicalName();
    }
}
