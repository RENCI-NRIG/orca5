package orca.shirako.plugins.substrate;

import java.util.Properties;
import java.util.Vector;

import orca.shirako.api.IDatabase;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.UnitID;
import orca.shirako.core.Unit;
import orca.util.OrcaException;

public interface ISubstrateDatabase extends IDatabase {
    public void addUnit(Unit u) throws Exception;

    public void updateUnit(Unit u) throws Exception;

    public void removeUnit(UnitID uid) throws Exception;

    public Vector<Properties> getUnit(UnitID uid) throws Exception;

    public Vector<Properties> getUnits(ReservationID rid) throws OrcaException;
    
    public Vector<Properties> getInventory() throws Exception;
    public Vector<Properties> getInventory(SliceID sliceID) throws Exception;
    public void transfer(UnitID unit, SliceID sliceID) throws Exception;
    public void untransfer(UnitID unit) throws Exception;
}
