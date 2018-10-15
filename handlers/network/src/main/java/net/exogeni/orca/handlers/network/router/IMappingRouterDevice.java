package net.exogeni.orca.handlers.network.router;

import net.exogeni.orca.handlers.network.core.CommandException;
import net.exogeni.orca.handlers.network.router.IRouterDevice;

/**
 * 
 * @author ibaldin
 *
 *         A router device capable of remapping VLAN tags
 * 
 */
public interface IMappingRouterDevice extends IRouterDevice {

    public void mapVLANs(String sourceTag, String destinationTag, String port) throws CommandException;

    public void unmapVLANs(String sourceTag, String destinationTag, String port) throws CommandException;

}
