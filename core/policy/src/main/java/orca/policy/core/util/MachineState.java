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
import orca.util.UnitsList;

import java.util.Date;


/**
 * <code>MachineState</code> represents the logical state of a virtual machine
 * monitor.
 */
public class MachineState
{
    /**
     * The units in the resource entry. The number indicates priority (0 is
     * highest) for the comparator.
     */
    public static final int CpuUnits = 0;
    public static final int MemoryUnits = 1;
    public static final int BandwidthUnits = 2;
    public static final int StorageUnits = 3;

    /**
     * The logical id of this machine.
     */
    protected ID id;

    /**
     * The calendar for CPU.
     */
    protected UnitsList cpu;

    /**
     * The calendar for memory.
     */
    protected UnitsList memory;

    /**
     * The calendar for bandwidth.
     */
    protected UnitsList bandwidth;

    /**
     * The calendar for storage.
     */
    protected UnitsList storage;
    protected long totalCpu;
    protected long totalMemory;
    protected long totalBandwidth;
    protected long totalStorage;
    protected Date start;
    protected Date end;

    /**
     * Creates a new instance.
     * @param start start time
     * @param end end time
     * @param id logical identifier
     * @param cpu cpu units
     * @param memory memory units
     * @param bandwidth bandwidth units
     * @param storage storage units
     */
    public MachineState(final Date start, final Date end, final ID id, final long cpu,
                        final long memory, final long bandwidth, final long storage)
    {
        long startTime = start.getTime();
        long endTime = end.getTime();

        this.start = start;
        this.end = end;
        this.id = id;

        this.cpu = new UnitsList(startTime, endTime, cpu);
        this.memory = new UnitsList(startTime, endTime, memory);
        this.bandwidth = new UnitsList(startTime, endTime, bandwidth);
        this.storage = new UnitsList(startTime, endTime, storage);
        this.totalCpu = cpu;
        this.totalMemory = memory;
        this.totalBandwidth = bandwidth;
        this.totalStorage = storage;
    }

    public synchronized String dumpStats(long cycle)
    {
        String s = cpu.getMinUnits(cycle, cycle + 1) + "::" + memory.getMinUnits(cycle, cycle + 1) +
                   "::" + bandwidth.getMinUnits(cycle, cycle + 1) + "::" +
                   storage.getMinUnits(cycle, cycle + 1);

        return s;
    }

    /**
     * Return the bandwidth list
     *
     * @return
     */
    public UnitsList getBandwidth()
    {
        return bandwidth;
    }

    public UnitsList getBandwidthCopy()
    {
        return bandwidth.copy();
    }

    /**
     * Return the cpu list
     *
     * @return
     */
    public UnitsList getCpu()
    {
        return cpu;
    }

    public UnitsList getCPUCopy()
    {
        return cpu.copy();
    }

    /**
     * Return the machine's id
     *
     * @return
     */
    public ID getId()
    {
        return id;
    }

    /**
     * Return the memory list
     *
     * @return
     */
    public UnitsList getMemory()
    {
        return memory;
    }

    public UnitsList getMemoryCopy()
    {
        return memory.copy();
    }

    /**
     * Return the storage list
     *
     * @return
     */
    public UnitsList getStorage()
    {
        return storage;
    }

    public UnitsList getStorageCopy()
    {
        return storage.copy();
    }

    /**
     *
     * DOCUMENT ME!
     *
     * @return the totalBandwidth
     */
    public long getTotalBandwidth()
    {
        return this.totalBandwidth;
    }

    /**
     *
     * DOCUMENT ME!
     *
     * @return the totalCpu
     */
    public long getTotalCpu()
    {
        return this.totalCpu;
    }

    /**
     *
     * DOCUMENT ME!
     *
     * @return the totalMemory
     */
    public long getTotalMemory()
    {
        return this.totalMemory;
    }

    /**
     *
     * DOCUMENT ME!
     *
     * @return the totalStorage
     */
    public long getTotalStorage()
    {
        return this.totalStorage;
    }

    public synchronized boolean isClean(long cycle)
    {
        // if (cpu.getMinUnits(cycle, cycle) == totalCpu &&
        // memory.getMinUnits(cycle, cycle) == totalMemory &&
        // bandwidth.getMinUnits(cycle, cycle) == totalBandwidth) {
        if ((cpu.getMinUnits(cycle, cycle + 1) == totalCpu) &&
                (memory.getMinUnits(cycle, cycle + 1) == totalMemory) &&
                (bandwidth.getMinUnits(cycle, cycle + 1) == totalBandwidth) &&
                (storage.getMinUnits(cycle, cycle + 1) == totalStorage)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Releases resources from this machine
     *
     * @param start
     * @param end
     * @param r
     *
     * @throws Exception
     */
    public void release(Date start, Date end, ResourceEntry r) throws Exception
    {
        long startTime = start.getTime();
        long endTime = end.getTime();

        cpu.release(startTime, endTime, r.getUnits(CpuUnits));
        memory.release(startTime, endTime, r.getUnits(MemoryUnits));
        bandwidth.release(startTime, endTime, r.getUnits(BandwidthUnits));
        storage.release(startTime, endTime, r.getUnits(StorageUnits));
    }

    public void releaseConditional(Date start, Date end, ResourceEntry r) throws Exception
    {
        long startTime = start.getTime();
        long endTime = end.getTime();

        cpu.releaseConditional(startTime, endTime, r.getUnits(CpuUnits));
        memory.releaseConditional(startTime, endTime, r.getUnits(MemoryUnits));
        bandwidth.releaseConditional(startTime, endTime, r.getUnits(BandwidthUnits));
        storage.releaseConditional(startTime, endTime, r.getUnits(StorageUnits));
    }

    /**
     * Reserves resources from this machine. Assumes same order as space().
     *
     * @param start
     * @param end
     * @param maxResources
     *
     * @throws Exception
     */
    public synchronized void reserve(Date start, Date end, long[] maxResources)
                              throws Exception
    {
        long startTime = start.getTime();
        long endTime = end.getTime();
        cpu.reserve(startTime, endTime, maxResources[CpuUnits]);
        memory.reserve(startTime, endTime, maxResources[MemoryUnits]);
        bandwidth.reserve(startTime, endTime, maxResources[BandwidthUnits]);
        storage.reserve(startTime, endTime, maxResources[StorageUnits]);
    }

    /**
     * Reserves resources from this machine
     *
     * @param start
     * @param end
     * @param r
     *
     * @throws Exception
     */
    public synchronized void reserve(Date start, Date end, ResourceEntry r)
                              throws Exception
    {
        long startTime = start.getTime();
        long endTime = end.getTime();

        cpu.reserve(startTime, endTime, r.getUnits(CpuUnits));
        memory.reserve(startTime, endTime, r.getUnits(MemoryUnits));
        bandwidth.reserve(startTime, endTime, r.getUnits(BandwidthUnits));
        storage.reserve(startTime, endTime, r.getUnits(StorageUnits));
    }

    public synchronized double size(long cycle)
    {
        double result = 1.0;

        if (!isClean(cycle)) {
            double cpusize = (double) (cpu.getMinUnits(cycle, cycle + 1)) / (double) totalCpu;
            double memorysize = (double) (memory.getMinUnits(cycle, cycle + 1)) / (double) totalMemory;
            double bandwidthsize = (double) (bandwidth.getMinUnits(cycle, cycle + 1)) / (double) totalBandwidth;
            double iosize = (double) (storage.getMinUnits(cycle, cycle + 1)) / (double) totalStorage;
            result = result * cpusize * memorysize * bandwidthsize * iosize;
        } else {
            result = 0.0;
        }

        return result;
    }

    /**
     * Tick each of the resources for cleaning
     *
     * @param time
     */
    public synchronized void tick(long time)
    {
        cpu.tick(time);
        memory.tick(time);
        bandwidth.tick(time);
        storage.tick(time);
    }

    /**
     * Returns the start time for the record.
     * @return start time
     */
    public Date getStart()
    {
        return start;
    }

    /**
     * Returns the end time for the record.
     * @return end time
     */
    public Date getEnd()
    {
        return end;
    }
}