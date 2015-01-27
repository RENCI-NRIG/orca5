/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.tests.core;

import java.util.Properties;

import orca.policy.core.BrokerSimplePolicy;
import orca.policy.core.util.PropertiesManager;
import orca.shirako.api.IActor;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.ILoadSource;
import orca.shirako.api.ISlice;
import orca.shirako.common.ResourceData;
import orca.shirako.common.ResourceType;
import orca.shirako.core.ServiceManager;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.ServiceManagerReservationFactory;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;

import org.apache.log4j.Logger;

/**
 * Very simple demand. Creates demand either once (if
 * <code>oneRequestOnly</code> used) or on every cycle. Only bids for one
 * resource type and the same number of resources and term length every time.
 * @author grit
 */
public class SimpleLoadSource implements ILoadSource {
    public static final String PropertyUnits = "units";
    public static final String PropertyResourceType = "type";
    public static final String PropertyLeaseLength = "leaseLength";
    public static final String PropertyOneRequest = "oneRequest";
    public static final String PropertySliceName = "sliceName";

    /**
     * The amount of time over specific policy decisions the SM must add when
     * communicating with other actors (e.g. redeem() and renew()). Clock skew
     * must be at least one if the SM is ticked after the agent and/or authority
     * At some point in time we may want this to not be static and learn it from
     * what we see in the system, but for now it is static.
     */
    public static final long CLOCK_SKEW = 1;

    /**
     * The slice
     */
    protected ISlice slice;

    /**
     * The service manager
     */
    protected ServiceManager sm;

    /**
     * The clock
     */
    protected ActorClock clock;

    /**
     * The logger
     */
    protected Logger logger;

    /**
     * What the previous cycle was
     */
    protected long previousCycle = -1;

    /**
     * Indicates if the load source is on its first demand or not
     */
    protected boolean firstDemand = true;

    /**
     * Indicates if the SM is creating demand once, or multiple times
     */
    private boolean oneRequestOnly = false;

    /**
     * If the advance time has been set
     */
    protected boolean unsetAdvance = true;

    /**
     * The advance time. Indicates how early the SM needs to bid to a particular
     * broker to make the correct allocation cycle.
     */
    protected long advanceTime = 0;

    /**
     * Number of units the SM wants
     */
    protected int units = 1;

    /**
     * The resource type the SM wants
     */
    protected ResourceType resourceType = new ResourceType(3);

    /**
     * The length of the lease
     */
    protected int leaseLength = 10;

    /**
     * The name of the slice the SM is requesting from
     */
    protected String sliceName = "sampleSlice";

    public SimpleLoadSource() {
    }

    /**
     * Make a bid to the SM. This policy makes one reservation request per
     * bidding cycle.
     * @param cycle the current cycle
     */
    protected void bid(long cycle) {
        if (oneRequestOnly && !firstDemand) {
            return;
        }

        // for now only bid once - checks the renews
        ResourceSet rset = new ResourceSet(units, resourceType);

        // get the advance time from the agent
        // for now - term length is 10 cycles
        Term term = new Term(clock.date(cycle + advanceTime), clock.getMillis(leaseLength));
        IClientReservation c = ServiceManagerReservationFactory.getInstance().create(rset, term, slice, sm.getDefaultBroker());

        // allow this reservation to be renewed
        c.setRenewable(true);
        PropertiesManager.setElasticSize(rset, true);
        PropertiesManager.setElasticTime(rset, true);

        try {
            sm.demand(c);
        } catch (Exception e) {
            logger.error("bid", e);
        }

        firstDemand = false;
    }

    public void configure(Properties p) throws Exception {
        String temp = p.getProperty(PropertyResourceType);

        if (temp != null) {
            resourceType = new ResourceType(temp);
        }

        temp = p.getProperty(PropertyUnits);

        if (temp != null) {
            units = Integer.parseInt(temp);
        }

        temp = p.getProperty(PropertyLeaseLength);

        if (temp != null) {
            leaseLength = Integer.parseInt(temp);
        }

        temp = p.getProperty(PropertyOneRequest);

        if (temp != null) {
            oneRequestOnly = Boolean.parseBoolean(temp);
        }

        temp = p.getProperty(PropertySliceName);

        if (temp != null) {
            sliceName = temp;
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public void initialize() throws Exception {
        // slice = (CodSlice) sm.getSlice(sliceName);
        // if (slice == null) {
        ResourceData rdata = new ResourceData();
        slice = (ISlice) SliceFactory.getInstance().create(sliceName, rdata);
        sm.registerSlice(slice);
        // }
        clock = sm.getActorClock();
        logger = sm.getLogger();
    }

    /**
     * {@inheritDoc}
     */
    public void setActor(IActor serviceManager) {
        this.sm = (ServiceManager) serviceManager;
    }

    protected void setAdvanceTime() {
        advanceTime = BrokerSimplePolicy.ADVANCE_TIME + CLOCK_SKEW;
        unsetAdvance = false;
    }

    /**
     * Set the lease length
     * @param lease length of the lease
     */
    public void setLeaseLength(int lease) {
        this.leaseLength = lease;
    }

    /**
     * Set the lease length
     * @param lease length of the lease
     */
    public void setLeaseLength(Integer lease) {
        this.leaseLength = lease.intValue();
    }

    /**
     * Set one request or multiple
     * @param value <code>true</code> if one request only
     */
    public void setOneRequestOnly(boolean value) {
        this.oneRequestOnly = value;
    }

    /**
     * Set one request or multiple
     * @param value <code>true</code> if one request only
     */
    public void setOneRequestOnly(Boolean value) {
        this.oneRequestOnly = value.booleanValue();
    }

    /**
     * Set the resource type
     * @param resType resource type
     */
    public void setResourceType(ResourceType resType) {
        this.resourceType = resType;
    }

    /**
     * Set the resource type
     * @param resType resource type
     */
    public void setResourceType(String resType) {
        this.resourceType = new ResourceType(resType);
    }

    /**
     * Set the units
     * @param u units
     */
    public void setUnits(int u) {
        this.units = u;
    }

    /**
     * Set the units
     * @param u units
     */
    public void setUnits(Integer u) {
        this.units = u.intValue();
    }

    /**
     * {@inheritDoc}
     */
    public void tick(long cycle) {
        /*
         * Do nothing on the first cycle: not all actors may be fully
         * initialized at this point.
         */
        if (previousCycle == -1) {
            previousCycle = cycle;

            return;
        }

        previousCycle = cycle;

        if (unsetAdvance) {
            setAdvanceTime();
        }

        bid(cycle);
    }
}
