package net.exogeni.orca.shirako.plugins;

import net.exogeni.orca.shirako.core.Ticket;

public interface ICertificatePolicy
{
    /**
     * Name for the container service.
     */
    public static final String ContainerServiceName = "container";

    /**
     * Deals with a new ticket.
     * @param ticket ticket
     * @throws Exception in case of error
     */
	public void  onUpdateTicket(Ticket ticket) throws Exception;
}  
