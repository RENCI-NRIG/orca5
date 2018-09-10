package net.exogeni.orca.manage;

import java.util.List;

import net.exogeni.orca.manage.beans.EventMng;
import net.exogeni.orca.util.ID;

import org.apache.log4j.Logger;

public class LocalEventManager {
	public static final int TIMEOUT = 10000; // 10 seconds
	public static final Logger logger = Logger.getLogger(LocalEventManager.class);
	private IOrcaActor actor;
	private ID subscriptionID;
	private IOrcaEventHandler handler;
	private Thread thread;
	private volatile boolean go;
	
	public LocalEventManager(IOrcaActor actor, IOrcaEventHandler handler) {
		this.actor = actor.clone();
		this.handler = handler;
	}

	public void start() {
		Runnable r = new Runnable() {
			public void run() {
				logger.debug("Creating event subscription");
				subscriptionID = actor.createEventSubscription();
				if (subscriptionID == null){
					logger.error("Could not create event subscription: "+ actor.getLastError());
					handler.error(actor.getLastError());
					return;
				}
				logger.debug("Created event subscription: " + subscriptionID);
	
				while (go){
					logger.debug("Draining events");
					List<EventMng> events = actor.drainEvents(subscriptionID, TIMEOUT);
					if (events == null){
						logger.error("Could not drain events: " + actor.getLastError());
						handler.error(actor.getLastError());
						return;
					}
					logger.debug("Received: " + events.size() + " events");
					for (EventMng e : events){
						handler.handle(e);
					}
				}
				logger.debug("Deleting event subscription: " + subscriptionID);
				actor.deleteEventSubscription(subscriptionID);
			}
		};
		
		go = true;
		thread = new Thread(r, "LocalEventManager");
		thread.setDaemon(true);
		thread.start();
	}
	
	public void stop() {
		if (thread == null) {return;}
		go = false;
		try {
			thread.join(2*TIMEOUT);
		} catch (InterruptedException e){
			throw new RuntimeException(e);
		}
		
		if (thread.isAlive()){
			throw new RuntimeException("Could not stop the event manager thread");
		}
	}
}
