#!/usr/bin/env python                                                                                                                                                   

import os
import sys
import logging as LOG
import logging.handlers
import signal
import string
import time
import traceback

from subprocess import *
from os import kill
from signal import alarm, signal, SIGALRM, SIGKILL
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
    def run(self, args, cwd = None, shell = False, kill_tree = True, timeout = -1, env = None):
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
        #p = Popen(args, shell = shell, cwd = cwd, stdout = PIPE, stderr = PIPE, env = env)
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
        p = Popen('ps --no-headers -o pid --ppid %d' % pid, shell = True,
                  stdout = PIPE, stderr = PIPE)
        stdout, stderr = p.communicate()
        return [int(p) for p in stdout.split()]


class NEuca_Quantum_Exception(Exception):
    pass

class NEuca_Quantum_Port_Plugged_In_Exception(Exception):
    pass


class NEuca_Quantum_Network:


    @classmethod
    def create_network(self, tenant_id, net_type, network, vlan_tag, max_rate=None, burst_rate=None ):
        #name ='vlan:data:101:11111:1111'
        name = str(net_type) + ":" + str(network) + ":" + str(vlan_tag)
        
        if max_rate != None:
            name += ":" + str(max_rate)
            if burst_rate != None:
                name += ":" + str(burst_rate)

        #quantum create_net $tenant_id $name
        cmd = ["quantum", "create_net", str(tenant_id), str(name) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout        
        
        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))
                      
        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Create Network Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        data_split = data_stdout.split()
        LOG.debug(str(data_split))
        if len(data_split) >= 8:
            network_uuid = data_split[7].strip()
        else:
            raise NEuca_Quantum_Exception, "Create Network Failed, bad stdout: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)

        return network_uuid

    @classmethod
    def _quantum_delete_port(self, tenant_id, network_uuid, port_uuid):
        #quantum delete_port $tenant_id $net_id $port_id
        cmd = ["quantum", "delete_port", str(tenant_id), str(network_uuid), str(port_uuid) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout 

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
             raise NEuca_Quantum_Exception, "Delete Port Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)
        
        if len(data_stdout) >= 1:
            line = data_stdout.split('\n')[0]
        if line == 'Command failed with error code: 409':
            raise NEuca_Quantum_Port_Plugged_In_Exception, "Delete Port Failed, A resource is currently attached to the logical port: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)

        data_split = data_stdout.split()
        LOG.debug(str(data_split))
        if len(data_split) >= 13:
            reported_port_uuid = data_split[5].strip()
            reported_network_uuid = data_split[9].strip()
            reported_tenant_id = data_split[12].strip()
        else:
            raise NEuca_Quantum_Exception, "Delete Port Failed, bad stdout: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)

        if reported_network_uuid != network_uuid or reported_port_uuid != port_uuid or reported_tenant_id != tenant_id:
            raise NEuca_Quantum_Exception, "Delete Port Failed, bad stdout reported network uuid does not match uuid: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)

    @classmethod
    def _quantum_unplug_iface(self, tenant_id, network_uuid, port_uuid):
        #quantum delete_port $tenant_id $net_id $port_id                                                                                                                                                                                  
        cmd = ["quantum", "unplug_iface", str(tenant_id), str(network_uuid), str(port_uuid) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
             raise NEuca_Quantum_Exception, "Unplug iface Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        data_split = data_stdout.split()
        LOG.debug(str(data_split))
        if len(data_split) >= 12:
            reported_port_uuid = data_split[4].strip()
            reported_network_uuid = data_split[8].strip()
            reported_tenant_id = data_split[11].strip()
        else:
            raise NEuca_Quantum_Exception, "Unplug iface Failed, bad stdout: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)

        if reported_network_uuid != network_uuid or reported_port_uuid != port_uuid or reported_tenant_id != tenant_id:
            raise NEuca_Quantum_Exception, "Unplug iface Failed, bad stdout reported network uuid does not match uuid: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)



    @classmethod
    def _clean_iface(self, tenant_id, network_uuid, port_uuid):

        delete_port_e = None
        unplug_iface_e = None

        try:
            self._quantum_unplug_iface(tenant_id, network_uuid, port_uuid)
        except Exception as e:
            unplug_iface_e = e
        
        try:
            self._quantum_delete_port(tenant_id, network_uuid, port_uuid)
        except:
            delete_port_e = e
 
        
        if delete_port_e != None or unplug_iface_e != e:
            raise NEuca_Quantum_Exception, "unplug_iface exception: " + str(unplug_iface_e) + ", delete_port_exception: " + str(delete_port_e) 

    @classmethod
    def _get_all_ifaces_for_network(self, tenant_id, network_uuid):
	
        cmd = ["quantum", "list_ports", str(tenant_id), str(network_uuid) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
             raise NEuca_Quantum_Exception, "Getting show_net_detail Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

	LOG.debug("all ifaces for network " + str(network_uuid) + ", " + str(data_stdout))

        data_split = data_stdout.split()
        LOG.debug(str(data_split))
 
	prev=''
	prev_prev=''
        port_list = []
	for token in data_split:
           LOG.debug('prev_prev == ' + str(prev_prev) + ' and prev == ' + str(prev) + ', token = ' + str(token))
           if prev_prev == 'Logical' and prev == 'Port:':
	      LOG.debug("a_port : " + str(token))
              port_list.append(str(token))
           
           prev_prev = prev
           prev = token


        LOG.debug('port_list: ' + str(port_list))
	return port_list


    @classmethod
    def _clean_all_ifaces_for_network(self, tenant_id, network_uuid):
        for iface in self._get_all_ifaces_for_network(tenant_id, network_uuid):
	   try:
	      self._clean_iface(tenant_id, network_uuid, iface)
           except Exception as e:
              LOG.error('Could not clean iface ' + str(iface) + ', from network ' + str(network_uuid) + ': ' + str(e))

    @classmethod
    def delete_network(self, tenant_id, network_uuid):

	try:
            self._clean_all_ifaces_for_network(tenant_id, network_uuid)
	except Exception as e:
              LOG.error('Could not all ifaces from network ' + str(network_uuid) + ': ' + str(e))



        #quantum delete_net neuca ee34c5e1-1cd7-41b3-bf12-365e43cfa50f
        cmd = ["quantum", "delete_net", str(tenant_id), str(network_uuid) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                           
                              

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Delete Network Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        data_split = data_stdout.split()
        LOG.debug(str(data_split))
        if len(data_split) >= 6:
            reported_network_uuid = data_split[5].strip()
        else:
            raise NEuca_Quantum_Exception, "Delete Network Failed, bad stdout: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)

        if reported_network_uuid != network_uuid:
            raise NEuca_Quantum_Exception, "Delete Network Failed, bad stdout reported network uuid does not match uuid.  reported_network_uuid = " + str(reported_network_uuid) +      ",  network_uuid=" +  str(network_uuid) +   ", cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)


    @classmethod
    def add_iface_to_network(self, tenant_id, network_uuid, instance_name, mac_addr):

        #iface_name instance-00000004.fe:16:3e:00:00:02
        iface_name = str(instance_name) + "." + str(mac_addr) 

        #quantum create_port $tenant_id $net_id
        cmd = ["quantum", "create_port", str(tenant_id), str(network_uuid) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "Add iface Failed (create_port), bad error code (" + str(rtncode) + ") : " + str(cmd)

        data_split = data_stdout.split()
        LOG.debug(str(data_split))
        if len(data_split) >= 7:
            port_uuid = data_split[6].strip()
        else:
            raise NEuca_Quantum_Exception, "Add iface Failed (create_port), bad stdout: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)


        #quantum quantum plug_iface $tenant_id $net_id $port_id instance-00000004.fe:16:3e:00:00:02
        cmd = ["quantum", "plug_iface", str(tenant_id), str(network_uuid), str(port_uuid), str(iface_name) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout 

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            self._clean_iface(tenant_id, network_uuid, port_uuid)
            raise NEuca_Quantum_Exception, "Add iface Failed (plug_iface), bad error code (" + str(rtncode) + ") : " + str(cmd)

        data_split = data_stdout.split()
        LOG.debug(str(data_split))
        if len(data_split) >= 14:
            reported_iface_name = data_split[2].strip()
            reported_port_uuid = data_split[6].strip()
            reported_network_uuid = data_split[10].strip()
            reported_tenant_id = data_split[13].strip()
        else:
            self._clean_iface(tenant_id, network_uuid, port_uuid)
            raise NEuca_Quantum_Exception, "Add iface Failed (plug_iface), bad stdout: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)


        if reported_iface_name != iface_name or reported_port_uuid != port_uuid or reported_network_uuid != network_uuid or reported_tenant_id != tenant_id:
            self._clean_iface(tenant_id, network_uuid, port_uuid)
            raise NEuca_Quantum_Exception, "Delete Network Failed, bad stdout reported info does not match supplied info: cmd = " + str(cmd) + "\nstdout = " + str(data_stdout)

        return port_uuid

    @classmethod
    def __remove_iface_from_network(self, tenant_id, network_uuid, port_uuid):
        
        self._clean_iface(tenant_id, network_uuid, port_uuid)
        
        return 'OK'


    @classmethod
    def __get_iface(self, tenant_id, network_id, port_id):
        #quantum show_iface tenant_id network port                                                                                                                                               #parse output to get interface name  
        
        cmd = ["quantum", "show_iface", str(tenant_id), str(network_id), str(port_id) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60)

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "show_iface failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        lines = data_stdout.split('\n')
        for line in lines:
            line = line.split()
            if len(line) >= 2 and line[0].strip() == "interface:":
                iface_id = line[1].strip()
                break
              
        return iface_id



    @classmethod
    def __get_ports(self, tenant_id, network_id):
        #quantum list_ports tenant_id network_id
        #parse and return list of ports
        ports=[]

        cmd = ["quantum", "list_ports", str(tenant_id), str(network_id) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) 

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "list_ports failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        lines = data_stdout.split('\n')
        for line in lines:
            line = line.split()
            if len(line) >= 3 and line[0].strip() == "Logical" and line[1].strip() == "Port:":
                port_uuid = line[2].strip()
                ports.append(port_uuid)

        return ports



    @classmethod
    def __get_networks(self, tenant_id):
        networks=[]

        #quantum list_nets geni-orca 
        #parse to get each network id       
        cmd = ["quantum", "list_nets", str(tenant_id) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                                            

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "list_nets failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        foundIt=False
        lines = data_stdout.split('\n')
        for line in lines:
            line = line.split()
            if len(line) >= 3 and line[0].strip() == "Network" and line[1].strip() == "ID:":
                network_uuid = line[2].strip()
                networks.append(network_uuid)
                
        return networks


    @classmethod
    def __get_network_and_port_from_iface(self, tenant_id, iface_name):
        network_uuid=None
        port_uuid=None

        LOG.debug("PRUTH: __get_network_and_port_from_iface: start")
        for network_id in NEuca_Quantum_Network.__get_networks(tenant_id):
            LOG.debug("PRUTH: __get_network_and_port_from_iface: network_id: " + str(network_id))
            for port_id in NEuca_Quantum_Network.__get_ports(tenant_id,network_id):
                LOG.debug("PRUTH: __get_network_and_port_from_iface: port_id: " + str(port_id))
                iface = NEuca_Quantum_Network.__get_iface(tenant_id, network_id, port_id)
                LOG.debug("PRUTH: __get_network_and_port_from_iface: iface_name: " + str(iface_name))
                if iface == iface_name:
                    network_uuid = network_id
                    port_uuid = port_id
                    break
            if network_uuid != None and port_uuid != None:
                break
            
        return [network_uuid,port_uuid]
        

    @classmethod
    def remove_iface(self, tenant_id, iface_name):
        #get network_uuid and port_uuid
        
        network_uuid,port_uuid = NEuca_Quantum_Network.__get_network_and_port_from_iface(tenant_id, iface_name)
        LOG.debug("PRUTH: remove_iface: network_uuid: " + str(network_uuid) + ", port_uuid: " + str(port_uuid))


        self._clean_iface(tenant_id, network_uuid, port_uuid)

        return 'OK'


    @classmethod
    def get_network_uuid_for_port(self, tenant_id, iface_uuid):
        #quantum  $tenant_id $net_id 

        cmd = ["quantum", "list_nets_detail", str(tenant_id) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise NEuca_Quantum_Exception, "list_nets_detail failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

        foundIt=False
        lines = data_stdout.split('\n')
        for line in lines:
            line = line.split()
            if len(line) >= 5:
                network_uuid = line[4].strip()

                ports_cmd = ["quantum", "list_ports", str(tenant_id), str(network_uuid) ]
                ports_rtncode,ports_data_stdout,ports_data_stderr = Commands.run(ports_cmd, timeout=60) #TODO: needs real timeout                                                                                 

                LOG.debug("ports_rtncode: " + str(ports_rtncode))
                LOG.debug("ports_data_stdout: " + str(ports_data_stdout))
                LOG.debug("ports_data_stderr: " + str(ports_data_stderr))

                if ports_rtncode != 0:
                    raise NEuca_Quantum_Exception, "list_ports failed, bad error code (" + str(ports_rtncode) + ") : " + str(ports_cmd)

                if iface_uuid in ports_data_stdout: 
                    foundIt = True
                    break
                
                

        if foundIt:
           return network_uuid;
        else:
           return None

    @classmethod
    def remove_all_vm_ifaces(self, tenant_id, iface_uuids):

        for iface_uuid in iface_uuids:
            network_uuid = NEuca_Quantum_Network.get_network_uuid_for_port(tenant_id, iface_uuid)
            if not network_uuid == None:
                NEuca_Quantum_Network.__remove_iface_from_network(tenant_id, network_uuid, iface_uuid)
            else:
                LOG.debug("could not find network_uuid for iface " + iface_uuid)
                return 'OK'


    @classmethod
    def get_network_uuid(self, tenant_id, vlan_tag, switch_name):

        #quantum  $tenant_id $net_id
        cmd = ["quantum", "list_nets_detail", str(tenant_id) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))
	
        if rtncode != 0:
            raise NEuca_Quantum_Exception, "list_nets_detail failed, bad error code (" + str(rtncode) + ") : " + str(cmd)

	foundIt=False
        lines = data_stdout.split('\n')
	for line in lines:
            line = line.split()
            if len(line) >= 5:
                network_name = line[1].strip()
                network_uuid = line[4].strip()	            
	
  	        network_info = network_name.split(":")
	        if len(network_info) >= 3:
                   if network_info[1] == switch_name and network_info[2] == str(vlan_tag):
                      foundIt=True
                      break

        if foundIt:
	   return network_uuid;
        else: 
	   return None
   



    




