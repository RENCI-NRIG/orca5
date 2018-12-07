#!/usr/bin/env python                                                                                                                                                   

import os 
import json 
import logging as LOG
import signal
from os import kill
from signal import alarm, signal, SIGALRM, SIGKILL
from subprocess import *
from subprocess import PIPE, Popen


class Commands:
    @classmethod
    def run_cmd(self, args):
        cmd = args
        LOG.debug("running command: " + " ".join(cmd))
        p = Popen(cmd, stdout=PIPE, stderr=STDOUT)
        retval = p.communicate()[0]

        return retval

    @classmethod
    def run(self, args, cwd=None, shell=False, kill_tree=True, timeout=-1, env=None):
        '''
        Run a command with a timeout after which it will be forcibly
        killed.

        Mostly from Alex Martelli solution (probably from one of his 
        python books) posted on stackoverflow.com
        '''

        class Alarm(Exception):
            pass

        def alarm_handler(signum, frame):
            raise Alarm

        LOG.debug("run: args= " + str(args))
        # p = Popen(args, shell = shell, cwd = cwd, stdout = PIPE, stderr = PIPE, env = env)
        p = Popen(args, stdout=PIPE, stderr=STDOUT)
        if timeout != -1:
            signal(SIGALRM, alarm_handler)
            alarm(timeout)
        try:
            stdout, stderr = p.communicate()
            if timeout != -1:
                alarm(0)
        except Alarm:
            pids = [p.pid]
            if kill_tree:
                pids.extend(self._get_process_children(p.pid))
            for pid in pids:
                # process might have died before getting to this line
                # so wrap to avoid OSError: no such process
                try:
                    kill(pid, SIGKILL)
                except OSError:
                    pass
            return -9, '', ''
        return p.returncode, stdout, stderr

    @classmethod
    def _get_process_children(self, pid):
        p = Popen('ps --no-headers -o pid --ppid %d' % pid, shell=True,
                  stdout=PIPE, stderr=PIPE)
        stdout, stderr = p.communicate()
        return [int(p) for p in stdout.split()]

    @classmethod
    def source(self, script, update=1):
        pipe = Popen(". %s; env" % script, stdout=PIPE, shell=True, env={'PATH': os.environ['PATH']})
        data = pipe.communicate()[0]
        env = dict((line.split("=", 1) for line in data.splitlines()))
        if update:
            os.environ.update(env)
        return env

class NEuca_Quantum_Exception(Exception):
    pass


class NEuca_Quantum_Port_Plugged_In_Exception(Exception):
    pass


class NEuca_Quantum_Network:

    @classmethod
    def create_network(self, tenant_id, net_type, network, vlan_tag, dataplane_network, openflow_network, max_rate=None, burst_rate=None ):
        #name ='vlan:data:101:11111:1111'
        
        # https://github.com/RENCI-NRIG/exogeni/issues/97
        # https://github.com/RENCI-NRIG/orca5/pull/167
        while True:
            network_uuid = NEuca_Quantum_Network.get_network_uuid(tenant_id, vlan_tag, network, net_type)
            if network_uuid is None:
                break
            else:
                LOG.debug("neuca_quantum_common: existing network found for vlan: " + vlan_tag + ", network: " + network)
                LOG.debug("neuca_quantum_common: delete existing network: " + str(network_uuid))
                NEuca_Quantum_Network.delete_network(tenant_id, network_uuid)


        name = str(net_type) + ":" + str(network) + ":" + str(vlan_tag)
        physNetwork = None
        type = "vlan"
        if network == "vlan-data" :
            physNetwork = dataplane_network
        else:
            physNetwork = openflow_network
        
        if max_rate != None:
            name += ":" + str(max_rate)
            if burst_rate != None:
                name += ":" + str(burst_rate)

        # openstack network create --project $tenant_id --provider-network-type $type --provider-physical-network $physNetwork --provider-segment $vlan_tag --disable-port-security $name -fjson
        cmd = ["openstack", "network", "create", "--project", str(tenant_id), "--provider-network-type", type, "--provider-physical-network", physNetwork, "--provider-segment", str(vlan_tag), "--disable-port-security", str(name) , "-fjson" ]

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout        
        
        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))
                      
        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Create Network Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        network_info = json.loads(data_stdout)
        network_uuid = None
        if network_info is not None:
            LOG.debug(str(data_stdout))
            network_uuid = network_info["id"]
        else:
            raise NEuca_Quantum_Exception, "Create Network Failed, bad stdout: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)

        return network_uuid

    @classmethod
    def _neutron_delete_port(self, port_uuid):
        # openstack port delete $port_id
        cmd = ["openstack", "port", "delete", str(port_uuid)]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Delete Port Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

    @classmethod
    def _neutron_remove_port_from_server(self, tenant_id, network_uuid, port_uuid, instance_id):
        # openstack server remove port $instance_id $port_uuid
        cmd = ["openstack", "server", "remove", "port", str(instance_id), str(port_uuid)]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Remove port from server Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        n_uuid, p_uuid, t_id, i_id = NEuca_Quantum_Network.__get_network_and_port_by_port_name_or_id(port_uuid)

        if n_uuid != network_uuid or p_uuid != port_uuid :
            raise NEuca_Quantum_Exception, "Remove port from server Failed, bad stdout reported network uuid does not match uuid: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)

    @classmethod
    def _clean_port(self, tenant_id, network_uuid, port_uuid, instance_id):

        delete_port_e = None
        remove_port_from_server_e = None

        try:
            if instance_id.strip():
                self._neutron_remove_port_from_server(tenant_id, network_uuid, port_uuid, instance_id)
        except Exception as e:
            remove_port_from_server_e = e

        try:
            self._neutron_delete_port(port_uuid)
        except Exception as e:
            delete_port_e = e

        if delete_port_e != None or remove_port_from_server_e != None:
            raise NEuca_Quantum_Exception, "remove_port_from_server exception: " + str(
                remove_port_from_server_e) + ", delete_port_exception: " + str(delete_port_e)

    @classmethod
    def _get_all_ports_for_network(self, tenant_id, network_uuid):
        # openstack port list --project $tenant_id --network $network_uuid -fjson
        cmd = ["openstack", "port", "list", "--project", str(tenant_id), "--network", str(network_uuid), "-fjson"]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Getting show_net_detail Failed, bad error code (" + str(
                rtncode) + ") : " + str(cmd)

        LOG.debug("all ports for network " + str(network_uuid) + ", " + str(data_stdout))

        ports_info = json.loads(data_stdout)
        LOG.debug(str(data_stdout))

        port_list = []
        if ports_info is not None:
            for port in ports_info:
                LOG.debug("port_uuid: " + port["ID"])
                port_list.append(str(port["ID"]))

        LOG.debug('port_list: ' + str(port_list))
        return port_list

    @classmethod
    def _clean_all_ports_for_network(self, tenant_id, network_uuid):
        for port in self._get_all_ports_for_network(tenant_id, network_uuid):
            try:
                n_uuid, p_uuid, t_id, i_id = NEuca_Quantum_Network.__get_network_and_port_by_port_name_or_id(port)
                self._clean_port(tenant_id, network_uuid, port, i_id)
            except Exception as e:
                LOG.error('Could not remove port ' + str(port) + ', from network ' + str(network_uuid) + ': ' + str(e))

    @classmethod
    def delete_network(self, tenant_id, network_uuid):

        try:
            self._clean_all_ports_for_network(tenant_id, network_uuid)
        except Exception as e:
            LOG.error('Could not remove all ports from network ' + str(network_uuid) + ': ' + str(e))
        # openstack network delete $network_uuid
        cmd = ["openstack", "network", "delete", str(network_uuid)]
        rtncode, data_stdout, data_stderr = Commands.run(cmd,
                                                         timeout=60)  # TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Delete Network Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

    @classmethod
    def add_iface_to_network(self, tenant_id, network_uuid, instance_id, instance_name, mac_addr):

        # port_name instance-00000004.fe:16:3e:00:00:02
        port_name = str(instance_name) + "." + str(mac_addr)

        # openstack port create --project $tenant_id --network $network_uuid --enable --disable-port-security --mac-address $mac_addr $port_name -fjson
        cmd = ["openstack", "port", "create", "--project", str(tenant_id), "--network", str(network_uuid), "--enable", "--disable-port-security", "--mac-address", str(mac_addr), port_name, "-fjson"]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Add iface Failed (create_port), bad error code (" + str(
                rtncode) + ") : " + str(cmd)

        port_info = json.loads(data_stdout)
        if port_info is not None:
            LOG.debug(str(data_stdout))
            port_uuid = port_info["id"]
        else:
            raise NEuca_Quantum_Exception, "Add iface Failed (create_port), bad stdout: cmd = " + str(
                cmd) + "\nstdout = " + str(data_stdout)

        # openstack server add port $instance_id $port_id instance-00000004.fe:16:3e:00:00:02
        cmd = ["openstack", "server", "add", "port", str(instance_id), str(port_uuid)]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            self._clean_port(tenant_id, network_uuid, port_uuid, instance_id)
            raise NEuca_Quantum_Exception, "Add iface Failed (add_port_to_server), bad error code (" + str(
                rtncode) + ") : " + str(cmd)

        try:
            n_uuid, p_uuid, t_id, i_id = NEuca_Quantum_Network.__get_network_and_port_by_port_name_or_id(port_uuid)
        except Exception as e:
            self._clean_port(tenant_id, network_uuid, port_uuid, instance_id)
            raise NEuca_Quantum_Exception, "Add iface Failed " + str(e)

        if p_uuid != port_uuid or n_uuid != network_uuid or i_id != instance_id:
            self._clean_port(tenant_id, network_uuid, port_uuid, instance_id)
            raise NEuca_Quantum_Exception, "Add iface Failed, bad stdout reported info does not match supplied info: cmd = " + str(
                cmd) + "\nstdout = " + str(data_stdout)

        return port_uuid

    @classmethod
    def __get_network_and_port_by_port_name_or_id(self, port_name_id):
        network_uuid = None
        port_uuid = None
        tenant_id = None
        instance_id = None
        # openstack port show $port_name_id -fjson
        cmd = ["openstack", "port", "show", str(port_name_id), "-fjson"]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Port show Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        port_info = json.loads(data_stdout)
        LOG.debug(str(data_stdout))
        if port_info is not None:
            port_uuid = port_info["id"]
            network_uuid = port_info["network_id"]
            tenant_id = port_info["project_id"]
            instance_id = port_info["device_id"]
        else:
            raise NEuca_Quantum_Exception, "Port show Failed, bad stdout: cmd = " + str(cmd) + "\nstdout = " + str(
                data_stdout)

        return [network_uuid, port_uuid, tenant_id, instance_id]

    @classmethod
    def remove_iface(self, tenant_id, instance_id, iface_name):
        # get network_uuid and port_uuid

        n_uuid, p_uuid, t_id, i_id  = NEuca_Quantum_Network.__get_network_and_port_by_port_name_or_id(iface_name)

        self._clean_port(tenant_id, n_uuid, p_uuid, i_id)

        return 'OK'


    @classmethod
    def remove_all_vm_ifaces(self, tenant_id, iface_uuids):

        for iface_uuid in iface_uuids:
            n_uuid, p_uuid, t_id, i_id  = NEuca_Quantum_Network.__get_network_and_port_by_port_name_or_id(iface_uuid)

            self._clean_port(tenant_id, n_uuid, p_uuid, i_id)
            return 'OK'

    @classmethod
    def get_network_uuid(self, tenant_id, vlan_tag, network, net_type):

        #openstack network list --project $tenant_id $net_id -fjson
        cmd = ["openstack", "network", "list", "--project", str(tenant_id), "-fjson" ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))
    
        if rtncode != 0:
            raise NEuca_Quantum_Exception, "list_nets_detail failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        foundIt=False
        networks = json.loads(data_stdout)
        network_uuid = None
        name = str(net_type) + ":" + str(network) + ":" + str(vlan_tag)
        LOG.debug("looking for network=" + str(name))
        for n in networks:
            if name in n["Name"]:
                network_uuid = n["ID"]
                foundIt=True
                break

        return network_uuid;

# Upon import, read in the needed OpenStack credentials from one of the right places.
if (os.path.isfile(os.environ['EUCA_KEY_DIR'] + "/novarc")):
    Commands.source(os.environ['EUCA_KEY_DIR'] + "/novarc")
elif (os.path.isfile(os.environ['EUCA_KEY_DIR'] + "/openrc")):
    Commands.source(os.environ['EUCA_KEY_DIR'] + "/openrc")
else:
    pass
