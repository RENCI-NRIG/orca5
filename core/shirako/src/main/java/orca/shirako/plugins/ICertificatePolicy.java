package orca.shirako.plugins;

import orca.shirako.core.Ticket;

public interface ICertificatePolicy
{
    /**
     * Name for the container service.
     */
    public static final String ContainerServiceName = "container";

    /**
     * Deals with a new ticket.
     * @param ticket
     * @throws Exception
     */
	public void  onUpdateTicket(Ticket ticket) throws Exception;
}  