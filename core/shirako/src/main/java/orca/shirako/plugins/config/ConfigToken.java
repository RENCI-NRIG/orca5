package orca.shirako.plugins.config;

import orca.shirako.common.ReservationID;
import orca.util.ResourceType;

public interface ConfigToken {
	ReservationID getReservationID();
	ResourceType getResourceType();
}