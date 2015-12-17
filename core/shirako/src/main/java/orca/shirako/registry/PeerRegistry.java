/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.registry;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IShirakoPlugin;
import orca.shirako.proxies.Proxy;
import orca.shirako.util.MapSet;
import orca.util.ID;
import orca.util.Initializable;
import orca.util.OrcaException;

import org.apache.log4j.Logger;


public class PeerRegistry implements Initializable
{
    /**
     * A hash table of proxies to brokers. Persisted to the data store.
     */
    protected Hashtable<ID, IBrokerProxy> brokers;
    protected IBrokerProxy defaultBroker;

    /**
     * Cache of objects per actor name
     */
    protected MapSet cache;
    protected IShirakoPlugin spi;
    protected Logger logger;
    private boolean initialized = false;

    public PeerRegistry()
    {
        brokers = new Hashtable<ID, IBrokerProxy>();
        cache = new MapSet();
    }

    public void actorAdded() throws Exception
    {
        loadFromDB();
    }

    public void addBroker(IBrokerProxy broker)
    {
        synchronized (this) {
            brokers.put(broker.getIdentity().getGuid(), broker);

            if (defaultBroker == null) {
                defaultBroker = broker;
            }
        }

        try {
            spi.getDatabase().addBroker(broker);

            logger.info("Added " + broker.getName() + " as broker");
        } catch (Exception e) {
            spi.getLogger().error("Error while adding broker: ", e);
        }
    }

    public synchronized IBrokerProxy getBroker(ID guid)
    {
        return (IBrokerProxy) brokers.get(guid);
    }

    public synchronized IBrokerProxy[] getBrokers()
    {
        IBrokerProxy[] result = null;

        if (brokers.size() > 0) {
            result = new IBrokerProxy[brokers.size()];

            int i = 0;
            Iterator<IBrokerProxy> iter = brokers.values().iterator();

            while (iter.hasNext()) {
                result[i++] = iter.next();
            }
        }

        return result;
    }

    public synchronized IBrokerProxy getDefaultBroker()
    {
        return defaultBroker;
    }

    public void initialize() throws OrcaException
    {
        if (!initialized) {
            if (spi == null) {
                throw new OrcaException("missing plugin");
            }

            logger = spi.getLogger();
        }
    }

    protected void loadFromDB() throws Exception
    {
        Vector v = spi.getDatabase().getBrokers();
        
        if (v != null) {
            for (int i = 0; i < v.size(); i++) {
                Properties p = (Properties) v.get(i);
                IBrokerProxy agent = (IBrokerProxy) Proxy.getProxy(p);
                brokers.put(agent.getIdentity().getGuid(), agent);

                if (i == 0) {
                    defaultBroker = agent;
                }
            }
        }
    }

    public void removeBroker(IBrokerProxy broker) throws Exception
    {
        synchronized (this) {
            brokers.remove(broker.getIdentity());
        }

        spi.getDatabase().removeBroker(broker);
    }

    public void setSlicesPlugin(IShirakoPlugin spi)
    {
        this.spi = spi;
    }
}