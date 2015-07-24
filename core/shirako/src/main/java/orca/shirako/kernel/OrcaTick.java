/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.kernel;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import orca.shirako.api.ITick;
import orca.shirako.container.Globals;

public class OrcaTick extends Tick
{
    /**
     * Timer object.
     */
    protected Timer timer;
    /**
     * Table of wrappers, one per subscriber.
     */
    protected HashSet<ITick> toTick;

    protected long currentCycle;
    
    public OrcaTick() {
    	toTick = new HashSet<ITick>();
    }
    
    public synchronized void addTickable(final ITick tickable){
    	toTick.add(tickable);
    }

    public synchronized void removeTickable(final ITick tickable){
    	toTick.remove(tickable);
    }

    protected synchronized void startWorker(){
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TickNotifier(), 0, cycleMillis);
    }

    protected synchronized void stopWorker(){
        if (timer != null){
        	timer.cancel();
        	timer = null;
        }
    }
    
    protected synchronized void nextTick() {
        long now = System.currentTimeMillis();
        currentCycle = clock.cycle(now);
        
        logger.trace("Clock interrupt: now=" + now + " cycle=" + currentCycle);
        
    	if (!manual && timer == null) {return;}
    	for (ITick t : toTick){
    		try {
    			logger.trace("Delivering external tick to " + t.getName() + " cycle=" + currentCycle);
    			t.externalTick(currentCycle);
    		} catch (Exception e){
    			Globals.Log.error("Unexpected error while delivering tick notification for " + t.getName(), e);
    		}
    	}
    }
    
    private class TickNotifier extends TimerTask {
		@Override
		public void run() {
			nextTick();
		}    	
    }
}
