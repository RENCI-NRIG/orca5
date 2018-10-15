package net.exogeni.orca.shirako.plugins.config;

import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.util.ResourceType;

public interface ConfigToken {
	ReservationID getReservationID();
	ResourceType getResourceType();
}
