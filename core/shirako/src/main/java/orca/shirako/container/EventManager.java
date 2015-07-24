package orca.shirako.container;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import orca.security.AuthToken;
import orca.shirako.api.IEvent;
import orca.shirako.api.IEventFilter;
import orca.shirako.api.IEventHandler;
import orca.shirako.util.TestException;
import orca.util.ID;

public class EventManager {
    private final HashMap<ID, AEventSubscription> subscriptions;

    public EventManager() {
        subscriptions = new HashMap<ID, AEventSubscription>();
    }

    public synchronized void clearSubscriptions() {
        subscriptions.clear();
    }

    public synchronized ID createSubscription(AuthToken token, IEventFilter... filters)
            throws EventManagerException {
        if (token == null) {
            throw new EventManagerException("Invalid token");
        }

        EventSubscription s = new EventSubscription(token, filters);
        subscriptions.put(s.getSubscriptionID(), s);
        return s.getSubscriptionID();
    }

    public synchronized ID createSubscription(IEventHandler handler, AuthToken token,
            IEventFilter... filters) throws EventManagerException {
        if (token == null) {
            throw new EventManagerException("Invalid token");
        }

        SynchronousEventSubscription s = new SynchronousEventSubscription(handler, token, filters);
        subscriptions.put(s.getSubscriptionID(), s);
        return s.getSubscriptionID();
    }

    public synchronized ID createPrivilegedSubscription(IEventHandler handler,
            IEventFilter... filters) throws EventManagerException {
        SynchronousPrivilegedEventSubscription s = new SynchronousPrivilegedEventSubscription(handler,
                filters);
        subscriptions.put(s.getSubscriptionID(), s);
        return s.getSubscriptionID();
    }

    public synchronized ID addEventFilter(ID subscriptionID, IEventFilter filter, AuthToken token)
            throws EventManagerException {
        AEventSubscription s = getSubscription(subscriptionID, token);
        return s.addEventFilter(filter);
    }

    public synchronized void deleteEventFilter(ID subscriptionID, ID filterID, AuthToken token)
            throws EventManagerException {
        AEventSubscription s = getSubscription(subscriptionID, token);
        s.deleteEventFilter(filterID);
    }

    public synchronized void deleteSubscription(ID id, AuthToken token)
            throws EventManagerException {
        getSubscription(id, token);
        subscriptions.remove(id);
    }

    private synchronized AEventSubscription getSubscription(ID subscriptionID, AuthToken token)
            throws EventManagerException {
        AEventSubscription s = subscriptions.get(subscriptionID);
        if (s == null) {
            throw new EventManagerException("Invalid subscription");
        }
        if (!s.hasAccesss(token)) {
            throw new EventManagerException("Access denied");
        }
        return s;
    }

    public List<IEvent> drainEvents(ID subscriptionID, AuthToken token, int timeout)
            throws EventManagerException {
        AEventSubscription s = getSubscription(subscriptionID, token);
        return s.drainEvents(timeout);
    }

    public synchronized void dispatchEvent(IEvent event) {
        try {
            Iterator<Entry<ID, AEventSubscription>> it = subscriptions.entrySet().iterator();
            while (it.hasNext()) {
                AEventSubscription s = it.next().getValue();
                if (s.isAbandoned()) {
                    it.remove();
                } else {
                    s.deliverEvent(event);
                }
            }
        } catch (TestException e) {
            Globals.Log.warn("Propagating test exception: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            Globals.Log.error("Could not dispatch event", e);
        }
    }
}