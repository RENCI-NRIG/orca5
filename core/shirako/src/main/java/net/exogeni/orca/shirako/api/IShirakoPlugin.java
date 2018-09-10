/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.api;

import java.util.Properties;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.common.delegation.IResourceTicketFactory;
import net.exogeni.orca.shirako.plugins.config.Config;
import net.exogeni.orca.shirako.util.ResourceData;
import net.exogeni.orca.util.Initializable;
import net.exogeni.orca.util.KeystoreManager;
import net.exogeni.orca.util.OrcaException;
import net.exogeni.orca.util.persistence.Persistable;
import net.exogeni.orca.util.persistence.Recoverable;

import org.apache.log4j.Logger;

/**
 * <code>IShirakoPlugin</code> defines the interface for linking/injecting
 * functionality to the Shirako leasing code. This interface can be used to link
 * other systems to the core, for example to add support for leasing to a
 * cluster management system.
 * <p>
 * These methods are upcalled as various events occur. All implementations of
 * this class must have a constructor that takes no arguments, and set methods
 * for their attributes.
 */
public interface IShirakoPlugin extends Initializable, Persistable, Recoverable {
    /**
     * Processes a list of configuration properties. This method is called by
     * the configuration engine using reflection.
     * 
     * @param p
     *            configuration properties
     * @throws Exception in case of error
     */
    public void configure(Properties p) throws Exception;

    /**
     * Performs initialization steps that require that the actor has been added
     * to the container.
     * 
     * @throws Exception
     *             if a critical error has occurred
     */
    public void actorAdded() throws Exception;

    /**
     * Initializes the actor key store. Called early in the initialization
     * process.
     * 
     * @param actor
     *            the actor object (Note that at this stage the plugin itself
     *            does not have access to the actor object and the method MUST
     *            use the actor object being passed).
     * 
     * @throws Exception in case of error
     */
    public void initializeKeyStore(IActor actor) throws Exception;

    /**
     * Informs the plugin that recovery is about to start.
     */
    public void recoveryStarting();
    
    /**
     * Rebuilds plugin state associated with a restored reservation. Called once
     * for each restored reservation.
     * 
     * @param reservation
     *            restored reservation
     * 
     * @throws OrcaException
     *             if rebuilding state fails
     */
    public void revisit(IReservation reservation) throws OrcaException;

    /**
     * Rebuilds plugin state associated with a restored slice. Called once for
     * each restored slice.
     * 
     * @param slice
     *            restored slice
     * 
     * @throws OrcaException
     *             if rebuilding state fails
     */
    public void revisit(ISlice slice) throws OrcaException;

    /**
     * Informs the plugin that recovery has completed.
     */
    public void recoveryEnded();
    
    /**
     * Restarts any pending configuration actions for the specified reservation
     * 
     * @param reservation
     *            reservation
     * 
     * @throws Exception
     *             if restarting actions fails
     */
    public void restartConfigurationActions(IReservation reservation) throws Exception;

    /**
     * Creates a new slice.
     * 
     * @param sliceID
     *            guid for the slice
     * @param name
     *            name for the slice
     * @param properties
     *            properties for the slice
     * @param other
     *            other relevant information
     * 
     * @return a slice object
     * 
     * @throws Exception in case of error
     */
    public ISlice createSlice(SliceID sliceID, String name, ResourceData properties, Object other)
            throws Exception;

    /**
     * Releases any resources held by the slice. Note: the database record will
     * not be removed.
     * 
     * @param slice
     *            the slice
     * 
     * @throws Exception
     *             if releasing resources fails
     */
    public void releaseSlice(ISlice slice) throws Exception;

    /**
     * Validates an incoming reservation request
     * 
     * @param reservation
     *            The reservation
     * @param auth
     *            AuthToken of the caller
     * 
     * @return True if the validation succeeds
     * 
     * @throws Exception in case of error
     */
    // XXX: not needed. rework the code to get rid of it
    public boolean validateIncoming(IReservation reservation, AuthToken auth) throws Exception;

    /**
     * Sets the actor. Note: the actor has to be fully initialized.
     * 
     * @param actor
     *            the actor
     */
    public void setActor(IActor actor);

    /**
     * Returns the actor associated with the plugin.
     * 
     * @return actor associated with the plugin
     */
    public IActor getActor();

    /**
     * Sets the logger.
     * 
     * @param logger
     *            instance
     */
    public void setLogger(Logger logger);

    /**
     * Returns the logger.
     * 
     * @return logger instance
     */
    public Logger getLogger();

    /**
     * Sets the actor's database instance.
     * 
     * @param db
     *            database instance
     */
    public void setDatabase(IDatabase db);

    /**
     * Obtains the actor's database instance.
     * 
     * @return database instance
     */
    public IDatabase getDatabase();

    /**
     * Sets the handler engine.
     * 
     * @param config handler engine
     */

    public void setConfig(Config config);

    /**
     * Returns the handler engine.
     * 
     * @return handler engine
     */
    public Config getConfig();

    /**
     * Sets the ticket factory.
     * 
     * @param ticketFactory ticketFactory
     */
    public void setTicketFactory(IResourceTicketFactory ticketFactory);

    /**
     * Obtains the ticket factory.
     * 
     * @return ticket factory
     */
    public IResourceTicketFactory getTicketFactory();

    /**
     * Returns the actor keystore manager.
     * 
     * @return keystore manager
     */
    public KeystoreManager getKeyStore();

    /**
     * Returns the configuration properties list passed at instantiation time to
     * this plugin.
     * 
     * @return configuration properties list
     */
    public Properties getConfigurationProperties();
}
