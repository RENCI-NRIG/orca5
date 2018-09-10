/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.core;

import java.util.Properties;

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IPolicy;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.plugins.config.ConfigToken;
import net.exogeni.orca.shirako.time.ActorClock;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.ReservationSet;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.OrcaException;
import net.exogeni.orca.util.persistence.NotPersistent;
import net.exogeni.orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

/**
 * Base class for all policy implementations.
 */
public class Policy implements IPolicy {
    public static final String PropertyGuid = "policyGuid";

    /**
     * Policy guid.
     */
    @Persistent(key = PropertyGuid)
    protected ID guid;

    /**
     * Logger.
     */
    @Persistent(reference = true)
    protected Logger logger;

    /**
     * Actor the policy belongs to.
     */
    @Persistent(reference = true)
    protected IActor actor;

    /**
     * The converter from real to local time
     */
    @Persistent(reference = true)
    protected ActorClock clock;

    /**
     * Initialization status.
     */
    @NotPersistent
    private boolean initialized = false;

    /**
     * Creates a new instance.
     */
    public Policy() {
        this.guid = new ID();
    }

    /**
     * Creates a new instance.
     * 
     * @param actor
     *            actor this policy belongs to
     */
    public Policy(final IActor actor) {
        this.actor = actor;
        this.logger = actor.getLogger();
        this.clock = actor.getActorClock();
        this.guid = new ID();
    }

    /**
     * {@inheritDoc}
     */
    public void close(final IReservation reservation) {
    }

    /**
     * {@inheritDoc}
     */
    public void closed(final IReservation reservation) {
    }

    /**
     * {@inheritDoc}
     */
    public void configurationComplete(String action, ConfigToken token, Properties outProperties) {
    }

    /**
     * Logs the specified error and throws an exception.
     * 
     * @param message
     *            error message
     * 
     * @throws OrcaException in case of error
     */
    protected void error(final String message) throws OrcaException {
        logger.error(message);
        throw new OrcaException(message);
    }

    /**
     * {@inheritDoc}
     */
    public void extend(final IReservation reservation, final ResourceSet resources, final Term term) {
    }

    /**
     * {@inheritDoc}
     */
    public void finish(final long cycle) {
    }

    /**
     * {@inheritDoc}
     */
    public ReservationSet getClosing(final long cycle) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ID getGuid() {
        return guid;
    }

    /**
     * {@inheritDoc}
     * @throws OrcaException in case of error
     */
    public void initialize() throws OrcaException {
        if (!initialized) {
            if (actor == null) {
                throw new OrcaException("Missing actor");
            }

            if (logger == null) {
                logger = actor.getLogger();
            }

            if (logger == null) {
                throw new OrcaException("Missing logger");
            }

            if (clock == null) {
                clock = actor.getActorClock();
            }

            if (clock == null) {
                throw new OrcaException("Missing clock");
            }
            initialized = true;
        }
    }

    /**
     * Logs the specified error and throws an exception.
     * 
     * @param message
     *            e
     * 
     * @throws OrcaException in case of error
     */
    protected void internalError(final String message) throws OrcaException {
        logger.error("Internal error: " + message);
        throw new OrcaException("Internal error: " + message);
    }

    /**
     * Logs an error.
     * 
     * @param message
     *            error message
     */
    protected void logError(final String message) {
        logger.error("Internal mapper error: " + message);
    }

    /**
     * Logs a warning
     * 
     * @param message
     *            warning message
     */
    protected void logWarn(final String message) {
        logger.warn("Internal mapper warning: " + message);
    }

    /**
     * {@inheritDoc}
     */
    public void prepare(final long cycle) {
    }

    /**
     * {@inheritDoc}
     */
    public Properties query(final Properties properties) {
        return new Properties();
    }

    /**
     * {@inheritDoc}
     */
    public void remove(final IReservation reservation) {
    }

    /**
     * {@inheritDoc}
     * @throws Exception in case of error
     */
    public void reset() throws Exception {
    }

    public void recoveryStarting() {
        // noop
    }

    /**
     * {@inheritDoc}
     * @throws Exception in case of error
     */
    public void revisit(final IReservation reservation) throws Exception {
    }
    
    public void recoveryEnded() {
        // noop
    }

    /**
     * {@inheritDoc}
     */
    public void setActor(final IActor actor) {
        this.actor = actor;
    }

}
