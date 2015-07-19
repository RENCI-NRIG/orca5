package orca.shirako.plugins.substrate;

import orca.shirako.api.IReservation;
import orca.shirako.api.IShirakoPlugin;
import orca.shirako.core.Unit;

public interface ISubstrate extends IShirakoPlugin {
    /**
     * Returns the substrate database.
     * @return
     */
    public ISubstrateDatabase getSubstrateDatabase();

    /**
     * Transfers in a new unit into the substrate. Transfer in involves
     * configuring the unit so that it is part of the substrate.
     * @param r reservation that contains the unit
     * @param unit unit to be transferred
     */
    public void transferIn(IReservation r, Unit unit);

    /**
     * Transfers an existing unit out of the substrate. Transfer out involves
     * unconfiguring the unit, so that it is no longer part of the substrate.
     * @param r reservation that contains the unit
     * @param unit unit to be transferred
     */
    public void transferOut(IReservation r, Unit unit);
    
    /**
     * Modifies an existing unit that is already part of the substrate.
     * @param r reservation that contains the unit
     * @param unit unit to be modified.
     */
    public void modify(IReservation r, Unit unit);

    /**
     * Updates only the properties of an existing unit that is already part of the substrate.
     * @param r reservation that contains the unit
     * @param unit unit to be modified.
     */
	public void updateProps(IReservation reservation, Unit u);
	
}
