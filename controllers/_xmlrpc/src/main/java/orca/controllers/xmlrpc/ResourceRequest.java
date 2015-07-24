package orca.controllers.xmlrpc;

import java.util.Iterator;
import java.util.List;

import orca.shirako.api.IServiceManagerReservation;
import orca.util.ID;

public class ResourceRequest {
    public ID requestId;
    public List<IServiceManagerReservation> listInterDomainReservations;
    public boolean closed = false;

    public boolean isActive() {

        boolean isActive = true; // initialize true since this is an &&
        // operation

        Iterator<IServiceManagerReservation> it = listInterDomainReservations.iterator();
        while (it.hasNext()) {
            IServiceManagerReservation currReservation = (IServiceManagerReservation) it.next();
            isActive = (isActive && currReservation.isActive());
        }

        return isActive;

    }

    public boolean isTerminal() {

        boolean isTerminal = true; // initialize true since this is an &&
        // operation

        Iterator<IServiceManagerReservation> it = listInterDomainReservations.iterator();
        while (it.hasNext()) {
            IServiceManagerReservation currReservation = (IServiceManagerReservation) it.next();
            isTerminal = (isTerminal && currReservation.isTerminal());
        }

        return isTerminal;

    }

    public boolean isClosed() {

        boolean isClosed = true; // initialize true since this is an &&
        // operation

        Iterator<IServiceManagerReservation> it = listInterDomainReservations.iterator();
        while (it.hasNext()) {
            IServiceManagerReservation currReservation = (IServiceManagerReservation) it.next();
            isClosed = (isClosed && currReservation.isClosed());
        }

        return isClosed;

    }

    public boolean hasAtLeastOneTerminal() {

        boolean hasAtLeastOneTerminal = false; // initialize false since
        // this is an || operation

        Iterator<IServiceManagerReservation> it = listInterDomainReservations.iterator();
        while (it.hasNext()) {
            IServiceManagerReservation currReservation = (IServiceManagerReservation) it.next();
            hasAtLeastOneTerminal = (hasAtLeastOneTerminal || currReservation.isTerminal());
        }

        return hasAtLeastOneTerminal;

    }

    public boolean hasAtLeastOneFailed() {

        boolean hasAtLeastOneFailed = false; // initialize false since this
        // is an || operation

        Iterator<IServiceManagerReservation> it = listInterDomainReservations.iterator();
        while (it.hasNext()) {
            IServiceManagerReservation currReservation = (IServiceManagerReservation) it.next();
            hasAtLeastOneFailed = (hasAtLeastOneFailed || currReservation.isFailed());
        }

        return hasAtLeastOneFailed;

    }
}
