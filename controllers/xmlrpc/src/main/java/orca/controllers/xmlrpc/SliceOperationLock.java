package orca.controllers.xmlrpc;

import java.util.concurrent.Semaphore;

/**
 * Objects of this class are used to ensure that slice operations are executed in the order that they arrived (at the
 * XMLRPC interface)
 * 
 * @author ibaldin
 *
 */
public class SliceOperationLock extends Semaphore {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SliceOperationLock() {
        // create a fair semaphore
        super(1, true);
    }
}
