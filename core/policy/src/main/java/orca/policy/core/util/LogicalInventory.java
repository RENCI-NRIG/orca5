/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.policy.core.util;


import orca.util.ID;
import orca.util.ResourceType;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;


/**
 * This class maintains a logical resource inventory of machines
 */
public class LogicalInventory
{
    /**
     * Set of machines
     */
    protected Hashtable<ID, MachineState> inventory;
    protected ResourceType type;
    protected Properties properties;

    /**
     * @return the properties
     */
    public Properties getProperties()
    {
        return this.properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Properties properties)
    {
        this.properties = properties;
    }

    public LogicalInventory()
    {
        inventory = new Hashtable<ID, MachineState>();
    }

    public LogicalInventory(ResourceType type)
    {
        this.type = type;
        inventory = new Hashtable<ID, MachineState>();
    }

    /**
     * Adds a machine to the logical inventory
     * @param machine machine
     */
    public synchronized void addMachine(MachineState machine)
    {
        inventory.put(machine.getId(), machine);
    }

    public synchronized MachineState getMachine(ID id)
    {
        return inventory.get(id);
    }

    /**
     * Reserves a set of resources
     * @param allotment allotment
     * @param maxResources maxResources
     * @param start start
     * @param end end
     * @throws Exception in case of error
     */
    public synchronized void reserve(AllotmentTable allotment, long[] maxResources, Date start,
                                     Date end) throws Exception
    {
        Iterator i = allotment.iterator();

        while (i.hasNext()) {
            AllotmentEntry r = (AllotmentEntry) i.next();
            MachineState machine = inventory.get(r.getId());

            for (int u = 0; u < r.getUnits(); u++) {
                machine.reserve(start, end, maxResources);
            }
        }
    }

    /**
     * Reserves a set of resources
     * @param id id
     * @param maxResources maxResources
     * @param start start
     * @param end end
     * @throws Exception in case of error
     */
    public synchronized void reserve(ID id, long[] maxResources, Date start, Date end)
                              throws Exception
    {
        MachineState machine = inventory.get(id);

        if (machine == null) {
            throw new Exception("No machine with the specified id");
        }

        machine.reserve(start, end, maxResources);
    }

    /**
     * Ticks all logical machines
     * @param time time
     */
    public synchronized void tick(long time)
    {
        Iterator i = iterator();

        while (i.hasNext()) {
            MachineState machine = (MachineState) i.next();
            machine.tick(time);
        }
    }

    /**
     * WARNING: not thread-safe!!!
     * @return Iterator
     */
    public Iterator<MachineState> iterator()
    {
        return inventory.values().iterator();
    }

    public synchronized Hashtable<ID, MachineState> getInvetoryCopy()
    {
        return (Hashtable<ID, MachineState>) inventory.clone();
    }

    public synchronized String dumpStats(long cycle)
    {
        int count = 0;
        double total = 0;
        StringBuffer s = new StringBuffer();
        Iterator i = iterator();
        int used = 0;

        while (i.hasNext()) {
            MachineState machine = (MachineState) i.next();

            if (!machine.isClean(cycle)) {
                used++;
            }
        }

        s.append("(inUse=" + used + ")");
        i = iterator();

        while (i.hasNext()) {
            MachineState machine = (MachineState) i.next();

            if (!machine.isClean(cycle)) {
                total = total + machine.size(cycle);
                s.append(" [M" + count + "=" + machine.dumpStats(cycle) + "]");
                count++;
            }
        }

        String size = Double.toString(total / (double) count);
        s.append("  ");
        s.append("Size=" + size);

        return s.toString();
    }

    public ResourceType getType()
    {
        return type;
    }

    public int getSize()
    {
        return inventory.size();
    }
}
