/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.drivers.util;

public class PropertyNames
{
    public static final String NodeID = "unit.nodeID";
    public static final String HostNodeID = "unit.hostNodeID";
    public static final String State = "unit.nodeState";
    public static final String MachineType = "unit.machineType";
    public static final String ResourceType = "unit.resourceType";
    public static final String HostKey = "unit.keys.host";
    public static final String HostPrivateKey = "unit.keys.host.private";
    public static final String UserPublicKey = "unit.keys.user";
    public static final String Memory = "unit.resource.memory";

    // cpu
    public static final String CpuShare = "unit.resource.cpu.weight";
    public static final String CpuExtra = "unit.resource.cpu.extra";

    // bandwidth
    public static final String Bandwidth = "unit.resource.bandwidth.weight";
    public static final String BandwidthExtra = "unit.resource.bandwidth.extra";
    public static final String VmmMemory = "unit.vmm.memory";
    public static final String Computons = "unit.vmm.computons";
    public static final String Serial = "unit.boot.serial";
    public static final String Nic = "unit.boot.nic";
    public static final String Root = "unit.boot.root";
    public static final String NumCpus = "unit.cpu.number";
    public static final String CpuModel = "unit.cpu.model";
    public static final String CpuSpeed = "unit.cpu.speed";
    public static final String CpuCache = "unit.cpu.cache";
    public static final String HostName = "unit.dns.hostName";
    public static final String DnsZone = "unit.dns.zoneName";
    public static final String DnsMXRecord = "unit.dns.mxRecord";
    public static final String NetSpeed = "unit.net.speed";
    public static final String Macs = "unit.net.macs";
    public static final String PrivateIP = "unit.net.privateIP";
    public static final String PrivateNetmask = "unit.net.privateMask";

    // XXX: NEW
    public static final String PrivateGateway = "unit.net.privateGateway";
    public static final String PrivateMac = "unit.net.privateMac";
    public static final String PrivateNetworkInterface = "unit.net.privateInterface";
    public static final String PublicIP = "unit.net.publicIP";
    public static final String PublicNetmask = "unit.net.publicMask";
    public static final String PublicGateway = "unit.net.publicGateway";
    public static final String IPs = "unit.net.ips";
    public static final String Netmasks = "unit.net.masks";
    public static final String Gateways = "unit.net.gateways";
    public static final String Kernel = "unit.os.kernel";
    public static final String Ramdisk = "unit.os.ramdisk";
    public static final String IP = "unit.net.ip";
    public static final String Netmask = "unit.net.mask";
    public static final String Gateway = "unit.net.gateway";
    public static final String IscsiInitiator = "unit.iscsi.initiator";
    public static final String RealHostName = "host.dns.hostName";
    public static final String RealHostIP = "host.net.ip";
    public static final String SliceName = "unit.slice";
    public static final String SiteName = "unit.site";
    public static final String NewMemory = "new.unit.resource.memory";

    // cpu
    public static final String NewCpuShare = "new.unit.resource.cpu.weight";
    public static final String NewCpuExtra = "new.unit.resource.cpu.extra";

    // bandwidth
    public static final String NewBandwidth = "new.unit.resource.bandwidth.weight";
    public static final String NewBandwidthExtra = "new.unit.resource.bandwidth.extra";
    public static final String NewHostName = "new.host.dns.hostName";
    public static final String NewHostIP = "new.host.net.ip";
}