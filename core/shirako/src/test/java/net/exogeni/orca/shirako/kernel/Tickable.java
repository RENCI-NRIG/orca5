/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.kernel;

import net.exogeni.orca.shirako.api.ITick;



/**
 * An implementation of <code>ITick</code> to be used in testing.
 */
public class Tickable implements ITick
{
    /**
     * Counter for number of invocations.
     */
    protected long counter = 0;

    public Tickable()
    {
    }

    public void externalTick(long cycle) throws Exception
    {
        counter++;
    }

    public long getCounter()
    {
        return counter;
    }
    
    public String getName() {
        return null;
    }
}
