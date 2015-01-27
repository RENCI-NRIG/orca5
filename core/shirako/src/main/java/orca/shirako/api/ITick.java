/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;

import orca.shirako.kernel.interfaces.ITicker;


/**
 * <code>ITick</code> defines the interface for objects that can be
 * periodically "ticked". A tick is a timer interrupt. Ticks are identified by a
 * cycle number. Cycle numbers increase monotonically with every tick. The
 * interval between two consecutive ticks is the cycle length. Cycle length is
 * fixed and preconfigured at system boot time.
 * <p>
 * Implementors of this interface have to take special care to ensure that the
 * code in {@link #externalTick(long)} is efficient, and does not block
 * unnecessarily or indefinitely. Blocking inside external tick may have
 * significant consequences depending on how ticks are actually delivered.
 * </p>
 * <p>
 * It is not guaranteed that cycles passed into two consecutive invocations of
 * {@link #externalTick(long)} will differ with by exactly 1 cycle. It is
 * possible for ticks to be delivered not on consecutive cycles. Implementations
 * of this interface have to take special care to ensure that they handle
 * correctly skipped ticks: e.g., remember the argument of the last invocation
 * and perform or operations scheduled to be executed between the last and the
 * current cycle.
 * </p>
 * @see ITicker
 */
public interface ITick
{
    /**
     * Processes a timer interrupt (a tick).
     *
     * @param cycle cycle number
     *
     * @throws Exception if a critical error occurs while processing the timer
     *         interrupt. In general, the implementor must catch exceptions
     *         internally and only pass exceptions up the call stack when
     *         critical conditions occur.
     */
    public void externalTick(long cycle) throws Exception;
    
    public String  getName();
}