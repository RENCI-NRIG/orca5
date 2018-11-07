package orca.shirako.container;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import orca.security.AuthToken;
import orca.shirako.api.IEvent;
import orca.shirako.api.IEventFilter;

public class EventSubscription extends AEventSubscription {
	public static final int MAX_QUEUE_SIZE = 1000;
	/**
	 * Max time between drain calls. The subscription is considered
	 * abandoned if the time between drain calls exceeds this limit.
	 */
	public static final int MAX_IDLE_TIME = 5 * 60* 1000;
	private final ArrayList<IEvent> queue;
	private long lastDrainTimestamp;
	
	
	public EventSubscription(AuthToken token, IEventFilter... optFilters) {
		super(token, optFilters);
		this.queue = new ArrayList<IEvent>();
		this.lastDrainTimestamp = System.currentTimeMillis();
	}
	

	public void deliverEvent(IEvent event){
		if (!matches(event)){
			return;
		}
		synchronized(queue){
			if (queue.size() == MAX_QUEUE_SIZE){
				queue.remove(0);
			}
			queue.add(event);
			queue.notify();
		}
	}
	
	public List<IEvent> drainEvents(int timeout) {
		lastDrainTimestamp = System.currentTimeMillis();
		ArrayList<IEvent> result = new ArrayList<IEvent>();
		synchronized(queue){
			if (queue.size() == 0) {
				try {
					queue.wait(timeout);
				} catch (InterruptedException e){
				}
			}
			Iterator<IEvent> iter = queue.iterator();
			while (iter.hasNext()){
				result.add(iter.next());
				iter.remove();
			}			
		}
		return result;
	}
	
	public boolean isAbandoned() {
		return (System.currentTimeMillis()-lastDrainTimestamp > MAX_IDLE_TIME);		
	}
}