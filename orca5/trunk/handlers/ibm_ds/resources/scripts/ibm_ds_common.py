#!/usr/bin/env python                                                                                                                                                   

import os
import sys
import logging as LOG
import logging.handlers
import signal
import string
import time
import traceback
import re

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


class IBM_DS_Exception(Exception):
    pass


class IBM_DS:

    @classmethod
    def get_host_info(self, ip, password, host_label):
        #Returns the list of the luns accessible by a host or host group.
        #host argument is a host or hostGroup in list [ 'Host', host_labe; ] OR  [ 'Host Group', host_group_label ] (i.e. what is returned by IBM_DS.get_mapping_by_target_name()  

        host_label = host_label[:30] 

        LOG.debug('get_initiator_labels_for_host(): ')
        LOG.debug('ip: ' + str(ip))
        LOG.debug('password: ' + str(password))
        LOG.debug('host_label (label truncated to 30 chars): ' + str(host_label))


        #sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "show storageSubsystem lunMappings hostgroup  [ \"G-A_SLICE-GUID--SLICE-GUID--SL\" ] ; " 
        iscsi_cmd = 'show storageSubsystem hostTopology ;' 
        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            LOG.error("Failed count_lun_mappings, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout) + ", stderr: " + str(data_stderr))
            return None

        info = {}
        initiators = []

        found_section= False
        lines = data_stdout.split('\n')
        initiator = {}
        curr_group_label = None
        curr_group_section_counter = 2
        for line in lines:
            line = re.split('  +', line.strip())
            LOG.debug('Line: ' + str(line))

            #find the host section 
            if found_section == True:                     
                if len(line) == 2  and line[0].strip() == 'Initiator:':
                    LOG.debug('adding Initiator: ' + line[1].strip())
                    initiator['Initiator'] = line[1].strip() 
                elif len(line) == 2  and line[0].strip() == 'Label:':
                    LOG.debug('adding Label: ' + line[1].strip())
                    initiator['Label'] = line[1].strip() 
                    initiators.append(initiator)
                    initiator = {}
                    
                elif ( len(line) == 2 or len(line) == 3) and line[0].strip() in [ 'Host type:' , 'Interface type:' , 'Alias:', 'CHAP secret:' ]:
                    LOG.debug('skipping ' + line[0].strip() + ': ' + line[1].strip())
                    continue;
                else:
                    break

            if len(line) == 2 and line[0].strip() == 'Host:' and line[1].strip() == str(host_label):
                LOG.debug('Found Section');
                found_section = True


            #keep track of Host Group sections
            if len(line) == 1 and line[0].strip() == '':
                LOG.debug('len(line) == 1 and line[0].strip() == "": curr_group_section_counter = ' + str(curr_group_section_counter))
                curr_group_section_counter = curr_group_section_counter + 1


            if curr_group_section_counter >= 2:
                LOG.debug('curr_group_section_counter >= 2 : curr_group_label = ' + str(curr_group_label))
                curr_group_label = None

            if len(line) == 2  and line[0].strip() == 'Host Group:':
                LOG.debug("len(line) == 2  and line[0].strip() == 'Host Group:': line[1].strip() = " + line[1].strip() )
                curr_group_label = line[1].strip()
                curr_group_section_counter = 0

                
        info['Initiators'] = initiators
        info['Host Group'] = curr_group_label

        return info



    @classmethod
    def get_lun_mappings(self,
                      ip,
                      password,
                      host):  
        
        #Returns the list of the luns accessible by a host or host group.
        #host argument is a host or hostGroup in list [ 'Host', host_labe; ] OR  [ 'Host Group', host_group_label ] (i.e. what is returned by IBM_DS.get_mapping_by_target_name() 

        #host_label = host_label[:30]

        LOG.debug('get_lun_mappings(): ')
        LOG.debug('ip: ' + str(ip))
        LOG.debug('password: ' + str(password))
        LOG.debug('host (label truncated to 30 chars): ' + str(host))

        if len(host) != 2:
            LOG.error("Failed count_lun_mappings, host argument in wrong format. Expected [ 'Host', host_label ] OR  [ 'Host Group', host_group_label ].  Recieved:  " + str(host) )
            return None

        #trucate host name if necessary
        host[1] = host[1][:30]
        
        luns = []

        if host[0] == 'Host':
            iscsi_cmd = 'show storageSubsystem lunMappings host ["' + str(host[1])  + '"]  ;'
        elif host[0] == 'Host Group':
            iscsi_cmd = 'show storageSubsystem lunMappings hostGroup ["' + str(host[1])  + '"]  ;'
        else:
            LOG.error("Failed count_lun_mappings, host argument in wrong format. Expected [ 'Host', host_label ] OR  [ 'Host Group', host_group_label ].  Recieved:  " + str(host) )
            return None

        #sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "show storageSubsystem lunMappings hostgroup  [ \"G-A_SLICE-GUID--SLICE-GUID--SL\" ] ; "
        #iscsi_cmd = 'show storageSubsystem lunMappings host ["' + str(host_label)  + '"]  ;'
        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                                                                                                   

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            LOG.error("Failed count_lun_mappings, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout) + ", stderr: " + str(data_stderr))
            return luns

        found_section= False
        lines = data_stdout.split('\n')
        for line in lines:
            #line = line.strip().split('  ')
            line = re.split('  +', line.strip())
            LOG.debug('Line: ' + str(line))
            
            if found_section == True:
                if len(line) >= 5:
                    accessible_split = line[3].strip().split()
                    #LOG.debug('accessibled_split = ' + str(accessible_split))
                    if len(accessible_split) == 2 and accessible_split[0].strip() == 'Host':
                        accessible_by = { 'type':'Host' , 'label':accessible_split[1].strip() }
                    elif len(accessible_split) == 3 and accessible_split[0].strip() == 'Host' and accessible_split[1].strip() == 'Group':
                        accessible_by = { 'type':'Host Group' , 'label':accessible_split[2].strip() }
                    else:
                        LOG.error("In count_lun_mappings, accessible by is not sane, skipping. " + str(accessible_split))
                        continue
                    current_lun = { 'Logical Drive Name':line[0].strip() , 'LUN':line[1].strip() , 'Controller':line[2].strip() , 'Accessible by':accessible_by , 'Logical Drive status':line[4].strip() }
                    luns.append(current_lun)
                else:
                    break
            
            if len(line) >= 5 and line[0].strip() == 'Logical Drive Name' and line[1].strip() == 'LUN' and line[2].strip() == 'Controller' and line[3].strip() == 'Accessible by' and line[4].strip() == 'Logical Drive status': 
                found_section = True

        LOG.debug('get_lun_mappings returning luns = ' + str(luns))
        return luns


    @classmethod
    def host_group_exists(self,
                          ip,
                          password,
                          target_group_label):
       
        target_group_label = target_group_label[:30]
       
        LOG.debug('host_group_exists(): ')
        LOG.debug('ip: ' + str(ip))
        LOG.debug('password: ' + str(password))
        LOG.debug('target_group_label (truncated to 30 chars): ' + str(target_group_label))
       
        iscsi_cmd = 'show storagesubsystem hostTopology  ;'
        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]
       
        LOG.debug('command: ' + str(cmd))
       
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                       
       
        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))
       
        if rtncode != 0:
            LOG.error("Failed check for host_group_existance, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout) + ", stderr: " + str(data_stderr))
            return False
       
        lines = data_stdout.split('\n')
        for line in lines:
            line = line.strip().split(':',1)
            LOG.debug('Line: ' + str(line))
            if len(line) >= 2 and line[0].strip() == 'Host Group' and line[1].strip() == str(target_group_label):
                LOG.debug('Returning True')
                return True

        LOG.debug('Returning False (end)')

        return False

    @classmethod
    def host_exists(self,
                          ip,
                          password,
                          target_host_label):

        target_host_label = target_host_label[:30]

        LOG.debug('host_exists(): ')
        LOG.debug('ip: ' + str(ip))
        LOG.debug('password: ' + str(password))
        LOG.debug('target_host_label (truncated to 30 chars): ' + str(target_host_label))

        iscsi_cmd = 'show storagesubsystem hostTopology  ;'
        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                                                                                                   

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            LOG.error("Failed check for host label existance, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout) + ", stderr: " + str(data_stderr))
            return False

        lines = data_stdout.split('\n')
        for line in lines:
            line = line.strip().split(':',1)
            LOG.debug('Line: ' + str(line))
            if len(line) >= 2 and line[0].strip() == 'Host' and line[1].strip() == str(target_host_label):
                LOG.debug('Returning True')
                return True

        LOG.debug('Returning False (end)')

        return False

    @classmethod
    def initiator_exists(self,
                          ip,
                          password,
                          initiator_iqn):


        LOG.debug('initiator_exists(): ')
        LOG.debug('ip: ' + str(ip))
        LOG.debug('password: ' + str(password))
        LOG.debug('initiator_iqn (truncated to 30 chars): ' + str(initiator_iqn))

        iscsi_cmd = 'show storagesubsystem hostTopology  ;'
        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout 

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            LOG.error("Failed check for initiator existance, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout) + ", stderr: " + str(data_stderr))
            return False

        lines = data_stdout.split('\n')
        for line in lines:
            line = line.strip().split(':',1)
            LOG.debug('Line: ' + str(line))
            if len(line) >= 2 and line[0].strip() == 'Initiator' and line[1].strip() == str(initiator_iqn):
                LOG.debug('Returning True')
                return True

        LOG.debug('Returning False (end)')

        return False

    @classmethod
    def get_mapping_by_target_name(self,
                          ip,
                          password,
                          target_name):


        LOG.debug('get_host_mapping_by_target_name(): ')
        LOG.debug('ip: ' + str(ip))
        LOG.debug('password: ' + str(password))
        LOG.debug('target_name: ' + str(target_name))

        #sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "show logicalDrive [ \"A-LUN-GUID--LUN-GUID--LUN-GUID\" ] ; "
        iscsi_cmd = 'show logicalDrive ["' + str(target_name)  + '"]  ;'
        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                                                                                                   

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            LOG.error("Failed check for get_host_mapping_by_target_name (probably no such target), bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout) + ", stderr: " + str(data_stderr))
            return None

        lines = data_stdout.split('\n')
        for line in lines:
            line = line.strip().split(':',1)
            LOG.debug('Line: ' + str(line))
            if len(line) >= 2 and line[0].strip() == 'Accessible By':
                mapping = line[1].strip().split()
                if len(mapping) >= 3 and mapping[0] == 'Host' and mapping[1] == 'Group':
                    return [ 'Host Group', mapping[2] ]
                elif len(mapping)>= 2 and mapping[0] == 'Host':
                    return [ 'Host', mapping[1] ]
                else:
                    break
            
        LOG.debug('No mapping found for target ' + str(target_name))
        return None


    #Creates the logical drive on the ibm_ds.  
    #   Does not create any initiators or associate initiators with the drive 
    @classmethod
    def __create_logical_drive(self,
                               pool,
                               raid_level,
                               owner,
                               ip,
                               password,
                               target_name,
                               target_capacity,
                               target_segment_size):
        #sudo /opt/IBM_DS/client/SMcli 192.168.105.11 -p 'password' -c "create logicalDrive array[pool0] raidLevel=5 userLabel=\"test-300M\" owner=A segmentSize=128 capacity=300 MB;"  
        iscsi_cmd = 'create logicalDrive array[' + str(pool) + '] raidLevel=' + str(raid_level) + ' userLabel="' + str(target_name) + '" owner=' + str(owner) + ' segmentSize=' + str(target_segment_size) + ' capacity=' + str(target_capacity) + ';'
        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                                                 

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise IBM_DS_Exception, "Create Lun Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout) + ", stderr: " + str(data_stderr)

    #Creates the host group if it does not exist. 
    @classmethod
    def __create_host_group(self,
                            ip,
                            password,
                            target_group_label):
        if not IBM_DS.host_group_exists(ip, password, target_group_label):
            #sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "create hostGroup userLabel=\"testhostgroupcreation\" ; " 
            iscsi_cmd = 'create hostGroup userLabel="' + str(target_group_label)  + '" ; '
            cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

            LOG.debug('command: ' + str(cmd))

            rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout     

            LOG.debug("rtncode: " + str(rtncode))
            LOG.debug("data_stdout: " + str(data_stdout))
            LOG.debug("data_stderr: " + str(data_stderr))

            if rtncode != 0:
                raise IBM_DS_Exception, "Create Lun Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout)+ ", stderr: " + str(data_stderr)

        else:
            LOG.debug("Host Group " + target_group_label + " exists, skipping creation")

    #Creates the host group if it does not exist.                               
    @classmethod
    def __create_host(self,
                      ip,
                      password,
                      target_group_label,
                      target_host_label,
                      target_initiator_label,
                      initiator_iqn):
        #create the host label if necessary
        if not IBM_DS.host_exists(ip, password, target_host_label):
            #sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "create host userLabel=\"pruthHost42\" hostGroup=\"all-vms\" hostType=\"Linux\" ;" 
            iscsi_cmd = 'create host userLabel="' + str(target_host_label) + '" hostGroup="' + str(target_group_label) + '" ;'
            cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

            LOG.debug('command: ' + str(cmd))

            rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout   

            LOG.debug("rtncode: " + str(rtncode))
            LOG.debug("data_stdout: " + str(data_stdout))
            LOG.debug("data_stderr: " + str(data_stderr))

            if rtncode != 0:
                raise IBM_DS_Exception, "Create Lun Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout)+ ", stderr: " + str(data_stderr)
        else:
            LOG.debug("Host " + target_host_label + " exists, skipping creation")


        if not IBM_DS.initiator_exists(ip, password, initiator_iqn):
            #sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "create iscsiInitiator iscsiName=\"iqn.2012-02.net.exogeni:9876543210\" userLabel=\"pruthcountdown\" host=\"pruthHost42\" ;"   
            iscsi_cmd = 'create iscsiInitiator iscsiName="' + str(initiator_iqn) + '" userLabel="' + str(target_initiator_label) + '" host="' + str(target_host_label)  + '";'
            cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

            LOG.debug('command: ' + str(cmd))

            rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout 

            LOG.debug("rtncode: " + str(rtncode))
            LOG.debug("data_stdout: " + str(data_stdout))
            LOG.debug("data_stderr: " + str(data_stderr))

            if rtncode != 0:
                raise IBM_DS_Exception, "Create Lun Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout)+ ", stderr: " + str(data_stderr)
        else:
            LOG.debug("Initiator iqn " + initiator_iqn + " exists, skipping creation")

    @classmethod
    def __map_drive_to_hostgroup(self,
                                 ip,
                                 password,
                                 target_name,
                                 target_lun,
                                 target_group_label
                                 ):
        #sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "set logicaldrive [ \"pruth100\" ] logicalUnitNumber=10 host=\"pruthHost42\" ; "         
        iscsi_cmd = 'set logicaldrive ["' + str(target_name) + '"]  logicalUnitNumber=' + str(target_lun) + ' hostGroup="' + str(target_group_label)  + '";'
        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout  

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        if rtncode != 0:
            raise IBM_DS_Exception, "Create Lun Failed, bad error code (" + str(rtncode) + ") : " + str(cmd)+ ", stdout: " + str(data_stdout)+ ", stderr: " + str(data_stderr)

    @classmethod
    def __delete_host_group(self 
                           ):
        pass

    @classmethod
    def __delete_host(self
                      ):
        pass

    @classmethod
    def __delete_host_initiator(self
                                ):
        pass

    @classmethod
    def __delete_logical_drive(self
                               ):
        pass




    @classmethod
    def create_lun(self, 
                   pool, 
                   raid_level, 
                   owner, 
                   ip, 
                   password, 
                   target_name, 
                   target_lun, 
                   target_capacity, 
                   target_segment_size, 
                   target_group_label,    
                   hosts):  
        
        LOG.info('Creating LUN:')

        target_name = target_name[:30]
        target_group_label = target_group_label[:30]
        
        LOG.debug('pool: ' + str(pool))
        LOG.debug('raid_level: ' + str(raid_level))
        LOG.debug('owner: ' + str(owner))
        LOG.debug('ip: ' + str(ip))
        LOG.debug('password: ' + str(password))
        LOG.debug('target_lun: ' + str(target_lun))
        LOG.debug('target_segment_size: ' + str(target_segment_size))

        LOG.debug('target_name: ' + str(target_name))
        LOG.debug('target_group_label (truncated to 30 chars): ' + str(target_group_label))
        
        LOG.debug('HOSTS: ' + str(hosts))

        #create the logical drive
        IBM_DS.__create_logical_drive(pool,raid_level,owner,ip,password,target_name,target_capacity,target_segment_size)
        
        #create the host group if necessary
        IBM_DS.__create_host_group(ip,password,target_group_label)

        for host in hosts:
            #create host if necessary
            IBM_DS.__create_host(ip,password,target_group_label,host['label'],host['initiator_label'],host['initiator_iqn'])
            
        #map drive to the host group
        IBM_DS.__map_drive_to_hostgroup(ip, password, target_name, target_lun, target_group_label)

        return


    @classmethod
    def delete_lun(self, ip, password, target_name):
        target_name = str(target_name)[:30]

        LOG.debug('ip: ' + str(ip))
        LOG.debug('password: ' + str(password))
        LOG.debug('target_name: ' + str(target_name))

        host = IBM_DS.get_mapping_by_target_name(ip, password, target_name)
        LOG.debug('**************************************  target mapped to host: ' + str(host))
        
        count = None
        if host != None and len(host) >= 2:
            luns = IBM_DS.get_lun_mappings(ip, password, host)
            info = IBM_DS.get_host_info(ip, password, str(host[1]))
            LOG.debug('**************************************  host mapped to: ' + str(luns) )
            LOG.debug('**************************************  host info: ' + str(info) )

        if host == None or len(host) < 2:
            LOG.warning("Delete LUN found no mappings. host == " + str(host))
            
        elif luns == None or len(luns) == 0:
            LOG.warning("delete_lun:  not removing host or hostGroup because host/group mappings count is not sane (luns == " + str(luns) +  ")")
          
        elif len(luns) > 1:
            LOG.debug("delete_lun:  not removing host or hostGroup because more host/group mappings exist (luns == " + str(luns) + ")")
          
        else:
            for lun in luns:
                host_type = lun['Accessible by']['type']
                host_label = lun['Accessible by']['label']
                #is lun mapped to group? i.e. is it mapped to whole slice 
                if host_type == 'Host Group':
                    # sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "delete host [ \"host-pruth-1234567890\" ] ; "                                                                                                                                                              
                    iscsi_cmd = 'delete hostGroup ["' + str(host_label) + '"];'
                    cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

                    LOG.debug("command: " + str(cmd))

                    rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                                                                                                   
                    LOG.debug("rtncode: " + str(rtncode))
                    LOG.debug("data_stdout: " + str(data_stdout))
                    LOG.debug("data_stderr: " + str(data_stderr))

                    if rtncode != 0:
                        raise IBM_DS_Exception, "Delete hostGroup Failed, bad error code (" + str(rtncode) + ") : " + str(cmd) + ", stdout: " + str(data_stdout)+ ", stderr: " + str(data_stderr)

                elif host_type == 'Host':
                
                    #host_initiators = IBM_DS.get_initiators_for_host(ip, password, host_label)
                    host_initiators = info['Initiators']
                    LOG.debug("host initiators: " + str(host_initiators))
            
                
                    #needed to check to see if we need to delete group too. However, group cannot be deleted until the end
                    group_luns = IBM_DS.get_lun_mappings(ip, password, info['Host Group'])
                                                         

                    for initiator in host_initiators:
                        #sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p  'password' -c "delete iscsiInitiator [ \"initiator-pruth-1234567890\" ] ; "
                        iscsi_cmd = 'delete iscsiInitiator ["' + str(initiator['Label']) + '"];'
                        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]
                
                        LOG.debug("command: " + str(cmd))
                    
                        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                                                                                      
                
                
                        LOG.debug("rtncode: " + str(rtncode))
                        LOG.debug("data_stdout: " + str(data_stdout))
                        LOG.debug("data_stderr: " + str(data_stderr))
            
                        if rtncode != 0:
                            LOG.error("Delete iscsiInitiator Failed, bad error code (" + str(rtncode) + ") : " + str(cmd) + ", stdout: " + str(data_stdout)+ ", stderr: " + str(data_stderr))                                                                                       
           
                    # sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "delete host [ \"host-pruth-1234567890\" ] ; "
                    iscsi_cmd = 'delete host ["' + str(host_label) + '"];'
                    cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]
                    
                    LOG.debug("command: " + str(cmd))

                    rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                                                                                      


                    LOG.debug("rtncode: " + str(rtncode))
                    LOG.debug("data_stdout: " + str(data_stdout))
                    LOG.debug("data_stderr: " + str(data_stderr))
                
                    if rtncode != 0:
                        raise IBM_DS_Exception, "Delete LUN Failed, bad error code (" + str(rtncode) + ") : " + str(cmd) + ", stdout: " + str(data_stdout)+ ", stderr: " + str(data_stderr)

                    if group_luns == None:
                        # sudo /opt/IBM_DS/client/SMcli 192.168.102.11 -p 'password' -c "delete host [ \"host-pruth-1234567890\" ] ; " 
                        iscsi_cmd = 'delete hostGroup ["' + str(info['Host Group']) + '"];'
                        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

                        LOG.debug("command: " + str(cmd))

                        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout  
                        
                        LOG.debug("rtncode: " + str(rtncode))
                        LOG.debug("data_stdout: " + str(data_stdout))
                        LOG.debug("data_stderr: " + str(data_stderr))

                        if rtncode != 0:
                            raise IBM_DS_Exception, "Delete hostGroup Failed, bad error code (" + str(rtncode) + ") : " + str(cmd) + ", stdout: " + str(data_stdout)+ ", stderr: " + str(data_stderr)



        #delete the lun                                                                                                                                                                                                                                                             
        #sudo /opt/IBM_DS/client/SMcli 192.168.105.11 -p 'password' -c "delete logicalDrive [\"test-300M\"];"                                                                                                                                                                       
        iscsi_cmd = 'delete logicalDrive ["' + str(target_name) + '"];'
        cmd = [ "sudo" , "/opt/IBM_DS/client/SMcli", str(ip), '-p', str(password), '-c', str(iscsi_cmd)]

        LOG.debug("command: " + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                                                                                                                                   


        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        return

   



    




