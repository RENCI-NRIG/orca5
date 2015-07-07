/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage;

import java.util.HashMap;
import java.util.Map;

import orca.util.ID;


public class OrcaConstants
{
    public static final String SiteName = "SiteName";
    public static final String ActorName = "ActorName";
    public static final String RoleHome = "home";
    public static final String RoleUser = "user";
    public static final String RoleBroker = "broker";
    public static final String RoleSite = "site";
    public static final String RoleAdmin = "admin";
    public static final String[] Roles = new String[] { RoleHome, RoleUser, RoleBroker, RoleSite, RoleAdmin };

    /**
     * Successful completion
     */
    public static final int ErrorNone = 0;

    /**
     * Generic error code. Indicates an error has occurred but says nothing
     * about its cause
     */
    public static final int ErrorUnspecified = -1;

    /**
     * Insufficient access privileges
     */
    public static final int ErrorAccessDenied = -1000;

    /**
     * Operation triggered an exception. The problem can be in the inputs
     * supplied to the function or the implementation of the function
     */
    public static final int ErrorException = -2000;

    /**
     * Cannot apply the requested operation to an active actor
     */
    public static final int ErrorActorIsActive = -3000;

    /**
     * At lease one operation failed to complete. To be returned from functions
     * that perform operations in a loop.
     */
    public static final int ErrorAtleastOneFailed = -4000;

    /**
     * The requested feature is not (yet) implemented
     */
    public static final int ErrorNotImplemented = -5000;

    /**
     * Cannot apply the requested operation to an inactive actor
     */
    public static final int ErrorActorIsNotActive = -6000;

    /**
     * The arguments are invalid.
     */
    public static final int ErrorInvalidArguments = -7000;
    public static final int ErrorInvalidActor = -8000;
    public static final int ErrorInvalidReservation = -9000;

    /**
     * Error while performing a database operation
     */
    public static final int ErrorDatabaseError = -10000;

    /**
     * Error inside shirako
     */
    public static int ErrorInternalError = -11000;

    /**
     * Actor does not exist or insufficient privileges to access the actor
     */
    public static int ErrorNoSuchActor = -12000;

    /**
     * Invalid plugin reference
     */
    public static int ErrorNoSuchPlugin = -13000;

    /**
     * Invalid plugin reference
     */
    public static int ErrorInvalidPluginType = -14000;
    public static int ErrorNoSuchSlice = -15000;
    public static final int ErrorNoSuchResourcePool = -15010;
    
    public static int ErrorInvalidSlice = -16000;
    public static int ErrorInvalidReservationStateRemove = -17000;
    public static int ErrorInvalidReservationStateClose = -17100;
    public static int ErrorNoSuchReservation = -17200;
    public static int ErrorSliceExists = -18000;
    public static int ErrorNoSuchBroker = -19000;
    public static int ErrorNoSuchMachine = -20000;
    public static int ErrorNoSuchStorageServer = -20001;
    public static int ErrorNoSuchDevice = -20002;
    public static int ErrorNoSuchInventory = -20003;

    /**
     * Actor does not exist or insufficient privileges to access the actor
     */
    public static int ErrorNoSuchManagementObject = -21000;
    public static int ErrorOperationNotSupported = -22000;
    public static int ErrorCannotCreateServiceStub = -23000;
    public static int ErrorNoSuchNode = -25000;
    public static int ErrorCannotReleaseNode = -25010;
    public static int ErrorCannotCloseNode = -25020;
    public static int ErrorCannotRemoveNode = -25030;

    
    public static int ErrorInvalidCertificate = -30000;
    
    /*
     * Operation-specific error codes. Should be restricted in the range -300 to
     * -400. Prefix each error code with the first letters of the operation
     */

    /*
     * Create resource pool (CRP)
     */
    public static final int CRPErrorPoolExists = -300;
    public static final int CRPErrorTypeExists = -301;
    public static ID ContainerManagmentObjectID = new ID("manager");
	/**
	 * Type code specifying a site authority.
	 */
	public static final int ActorTypeSiteAuthority = 3;
	/**
	 * Type code specifying a broker.
	 */
	public static final int ActorTypeBroker = 2;
	/**
	 * Type code specifying a service manager.
	 */
	public static final int ActorTypeServiceManager = 1;
	/**
	 * Type code specifying all actors.
	 */
	public static final int ActorTypeAll = 0;
	public static final String ProtocolLocal = "local";
	public static final String ProtocolSoapAxis2 = "soapaxis2";
	public static final String ProtocolSoap = "soap";
	public static final String SM = "sm";
	public static final String SERVICE = "service";
	public static final String AGENT = "agent";
	public static final String BROKER = "broker";
	public static final String AUTHORITY = "authority";
	public static final String SITE = "site";
	// not really an actor, but for actor registry purposes
	public static final String CONTROLLER = "controller";
	
	public static final String EventClass = "event.class";

	public static final int AllReservationStates = -1;

	public static final int ReservationStateUnknown = 0;
    public static final int ReservationStateNascent = 1;
    public static final int ReservationStateTicketed = 2;
    public static final int ReservationStateActive = 3;
    public static final int ReservationStateActiveTicketed = 4;
    public static final int ReservationStateClosed = 5;
    public static final int ReservationStateCloseWait = 6;
    public static final int ReservationStateFailed = 7;
    public static final String[] states = {
                                              "", "Nascent", "Ticketed", "Active",
                                              "ActiveTicketed", "Closed", "CloseWait", "Failed"
                                          };
    
    public static String getReservationStateName(int state){
    	if (state > states.length || state <= 0){
    		return "Invalid";
    	}
    	return states[state];
    }
	
    
    public static final int ReservationPendingStateUnknown = 0;
    public static final int ReservationPendingStateNone = 1;
    public static final int ReservationPendingStateTicketing = 2;
    public static final int ReservationPendingStateRedeeming = 3;
    public static final int ReservationPendingStateExtendingTicket = 4;
    public static final int ReservationPendingStateExtendingLease = 5;
    public static final int ReservationPendingStatePriming = 6;
    public static final int ReservationPendingStateBlocked = 7;
    public static final int ReservationPendingStateClosing = 8;
    public static final int ReservationPendingStateProbing = 9;
    public static final int ReservationPendingStateClosingJoining = 10;
    public static final int ReservationPendingStateModifyingLease = 11;
    public static final String[] pendings = {
                                                "", "None", "Ticketing", "Redeeming",
                                                "ExtendingTicket", "ExtendingLease", "Priming",
                                                "Blocked", "Closing", "Probing", "ClosingJoining", "ModifyingLease"
                                            };

    public static String getReservationPendingStateName(int state){
    	if (state > pendings.length || state <= 0){
    		return "Invalid";
    	}
    	
    	return pendings[state];
    }
    
    public static final int ReservationJoinStateNoJoin = 1;
    public static final int ReservationJoinStateBlockedJoin = 2;
    public static final int ReservationJoinStateBlockedRedeem = 3;
    public static final int ReservationJoinStateJoining = 4;
    public static final String[] joinstates = {
                                                  "", "NoJoin", "BlockedJoin",
                                                  "BlockedRedeem", "Joining"
                                              };


    public static String getReservationJoinStateName(int state){
    	if (state > joinstates.length || state <= 0){
    		return "Invalid";
    	}
    	
    	return joinstates[state];
    }

    private static final Map<Integer,String> errorMessages = new HashMap<Integer, String>();
    
    static {
    	errorMessages.put(ErrorNoSuchManagementObject, "Unknown management object");
    }
    
    public static String getErrorMessage(int code) {
    	String msg = errorMessages.get(code);
    	if (msg == null){
    		msg = "See OrcaConstants for details";
    	}
    	return msg;
    }

	public static final int ExtendSameUnits = -1;

	public static final String MODIFY_SUBCOMMAND_PROPERTY="modify.subcommand.";
	public static final String MODIFY_PROPERTY_PREFIX="modify.";
}