/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.container.api;

import net.exogeni.orca.shirako.api.ITick;
import net.exogeni.orca.shirako.time.ActorClock;


/**
 * <code>IContainerClock</code> describes the time-related container API.
 */
public interface IContainerClock
{
    /**
     * Returns an instance of the container's clock factory.
     * @return container clock factory
     */
    public ActorClock getActorClock();

    /**
     * Checks if the container clock advances manually.
     * @return true if the container clock advances manually
     */
    public boolean isManualClock();

    /**
     * Advances the container clock with one cycle (only if isManualClock() is true).
     */
    public void tick();

    /**
     * Returns the current cycle.
     * @return current clock cycle
     */
    public long getCurrentCycle();

    /**
     * Stops the clock.
     * @throws Exception in case of error
     */
    public void stop() throws Exception;

    /**
     * Registers an object with the ticker.
     * @param tickable object to register
     */
    public void register(ITick tickable);

    /**
     * Unregisters an object with the ticker.
     * @param tickable object to register
     */
    public void unregister(ITick tickable);
}
