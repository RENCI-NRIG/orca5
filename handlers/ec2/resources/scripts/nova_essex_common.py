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

    @classmethod
    def source(self, script, update=1):
        pipe = Popen(". %s; env" % script, stdout=PIPE, shell=True, env={'PATH': os.environ['PATH']})
        data = pipe.communicate()[0]
        env = dict((line.split("=", 1) for line in data.splitlines()))
        if update:
           os.environ.update(env)
        return env

class VM_Exception(Exception):
    pass

class VM_Does_Not_Exist(VM_Exception):
    pass

class Nova_Command_Fail(VM_Exception):
    pass

class Nova_Fatal_Command_Fail(Nova_Command_Fail):
    pass
    
class VM_Broken(VM_Exception):
    def __init__(self, message, vm_id, console_log):
        Exception.__init__(self,message)
        self.vm_id = str(vm_id)
        self.console_log = str(console_log)

    def get_console_log(self):
        return str(self.console_log)
   
    def get_vm_id(self):
        return str(self.vm_id)

class VM_Broken_Unpingable(VM_Broken):
    def __init__(self, message, vm_id, console_log):
        VM_Broken.__init__(self,message, vm_id, console_log)

class VM_Broken_Unsshable(VM_Broken):
    def __init__(self, message, vm_id, console_log):
        VM_Broken.__init__(self,message, vm_id, console_log)

class VM:
    @classmethod
    def _get_console_log_by_ID(self, id):
        try:
            retry = os.environ['EC2_RETRIES']
        except:
            retry = 3

        try:
            timeout = os.environ['EC2_TIMEOUT']
        except:
            timeout = 5


        if id == None:
            raise Nova_Command_Fail, "_get_console_log_by_ID Invalid id " + str(id)

        data = None
        vm_console_log = ''
        for i in range(retry):
            try:
                cmd = ["nova", "console-log", str(id) ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                    

                if rtncode != 0:
                    raise VM_Does_Not_Exist, str(cmd)

                vm_console_log = data_stdout
                break
            except VM_Does_Not_Exist as e:
                raise e
            except Exception as e:
                LOG.warning("Failed cmd: " + str(cmd) + ", retrying (" + str(i) + ")")
                time.sleep(timeout)

        if i == retry:
            raise Nova_Command_Fail, "Failed cmd  " + str(retry) + " times, giving up: " + str(cmd)

        return vm_console_log


    @classmethod
    def _parse_vm_info(self, info):
        vm_info={}
        lines = info.split('\n')

        

        if len(lines) >= 4:
            lines = info.split('\n')[3:-2]
        else:
            raise Nova_Command_Fail, "Parse vm info: len(lines) < 4: " + str(id)

        for line in lines:
            line = line.split('|')
            if len(line) >= 3:
                vm_info[line[1].strip()] = line[2].strip()
            elif len(line) >= 2:
                vm_info[line[1].strip()] = ''

        return vm_info

    @classmethod
    def _get_all_info_by_ID(self, id):
        try: 
            retry = os.environ['EC2_RETRIES']
        except:
            retry = 3

        try:
            timeout = os.environ['EC2_TIMEOUT']
        except:
            timeout = 5

            
        if id == None:
            raise Nova_Command_Fail, "_get_info_by_ID Invalid id " + str(id)
        
        data = None
        vm_info = {}
        for i in range(retry):
            try:
                cmd = ["nova", "show", str(id) ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout
                
                if rtncode != 0:
                    raise VM_Does_Not_Exist, str(cmd)
                                  
                vm_info = self._parse_vm_info(data_stdout)
                break
            except VM_Does_Not_Exist as e:
                raise e
            except Exception as e:
                LOG.warning("Failed cmd: " + str(cmd) + ", retrying (" + str(i) + ")")
                time.sleep(timeout)
        
        if i == retry:
            raise Nova_Command_Fail, "Failed cmd  " + str(retry) + " times, giving up: " + str(cmd)

        return vm_info

    @classmethod
    def _get_info_by_ID(self, id, field):
        if id == None:
            raise Nova_Command_Fail, "Invalid id " + str(id)

        try:
            vm_info = self._get_all_info_by_ID(id)
            return vm_info[field]
        except VM_Does_Not_Exist as e:
            raise e
        except Exception as e:
            raise Nova_Commnad_Fail, "No value for " + str(field) + " in info for " + str(id)

    @classmethod
    def _get_info_by_name(self, name, field):
        #name and id are interchangible for this purpose
        try:
            return self._get_info_by_ID(name, field)
        except Exception as e:
            LOG.error("_get_info_by_name: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
            raise e
        
        
    @classmethod
    def get_state_by_ID(self, id):
        return self._get_info_by_ID(id,'OS-EXT-STS:vm_state')
                  
    @classmethod
    def get_host_by_ID(self, id):
        return self._get_info_by_ID(id,'OS-EXT-SRV-ATTR:host')

    @classmethod
    def get_instance_name_by_ID(self, id):
        return self._get_info_by_ID(id,'OS-EXT-SRV-ATTR:instance_name')

    @classmethod
    def get_date_created_by_ID(self, id):
        return self._get_info_by_ID(id,'created')

    @classmethod
    def get_instance_type_by_ID(self, id):
        return self._get_info_by_ID(id,'flavor')

    @classmethod
    def get_image_by_ID(self, id):
        return self._get_info_by_ID(id,'image')

    @classmethod
    def get_name_by_ID(self, id):
        return self._get_info_by_ID(id,'name')

    @classmethod
    def get_status_by_ID(self, id):
        return self._get_info_by_ID(id,'status')

    @classmethod
    def get_ip_by_ID(self, id):
        return self._get_info_by_ID(id,'public network')

    @classmethod
    def exists_by_ID(self, id):
        info = self._get_all_info_by_ID(id)
        if len(info) > 0:
            return True
        else:
            return False

    @classmethod
    def get_ID_by_name(self, name):
        return self._get_info_by_name(name,'id')
    
    @classmethod
    def get_console_log_by_ID(self, id):
        return self._get_console_log_by_ID(id)

    @classmethod
    def _get_all_floating_ip_2_vm(self):
        cmd = ["nova", "floating-ip-list" ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                            
        if rtncode != 0:
            raise Nova_Command_Fail, str(cmd) + ": Failed to list floating ips"

        #Parse the result 
        floating_ip_info={}
        lines = data_stdout.split('\n')

        if len(lines) >= 4:
            lines = info.split('\n')[3:-2]
        else:
            raise Nova_Command_Fail, "Parsing floating ips, len(lines) < 4:\n " + str(lines)

        for line in lines:
            line = line.split('|')
            if len(line) >= 3:
                floating_ip_info[line[1].strip()] = line[2].strip()
       
        return floating_ip_info

    @classmethod
    def _get_all_vms_2_floating_ip(self):
        cmd = ["nova", "floating-ip-list" ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                       
        if rtncode != 0:
            raise Nova_Command_Fail, str(cmd) + ": Failed to list floating ips"

        #Parse the result
        floating_ip_info={}
        lines = data_stdout.split('\n')

        if len(lines) >= 4:
            lines = lines[3:-2]
        else:
            raise Nova_Command_Fail, "Parsing floating ips, len(lines) < 4:\n" + str(lines)

        for line in lines:
            line = line.split('|')
            if len(line) >= 3 and line[2].strip() != 'None':  
                floating_ip_info[line[2].strip()] = line[1].strip()
                
        return floating_ip_info


    @classmethod
    def get_floating_ip_by_id(self, id):
        cmd = ["nova", "floating-ip-list" ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                        
        if rtncode != 0:
            raise Nova_Command_Fail, str(cmd) + ": Failed to list floating ips"

        floating_ips = self._get_all_vms_2_floating_ip()

        try:
            ipaddr = floating_ips[id]
        except:
            ipaddr = None

        return ipaddr

    @classmethod
    def _get_ip_from_allocate_floating_ip_info(self, info):
        lines = info.split('\n')

        if len(lines) >= 4:
            lines = info.split('\n')[3:-2]
        else:
            raise Nova_Command_Fail, "Parse floating ip info: len(lines) < 4 \n" + str(info)

        if len(lines) != 1:
            raise Nova_Command_Fail, "Parse floating ip info: len(lines) != 1 \n" + str(info)

        try:
            #Could validate that ip str is in a valid form... but we dont
            return lines[0].split('|')[1].strip()
        except:
            raise Nova_Command_Fail, "_get_ip_from_allocate_floating_ip_info could not find ip in line: " + str(info)


    @classmethod
    def _allocate_floating_ip(self, retries=10, timeout=20):
        for i in range(retries):
            try:

                cmd = ["nova", "floating-ip-create" ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout              
        
                if rtncode == 0:
                    ipaddr = self._get_ip_from_allocate_floating_ip_info(str(data_stdout))
                    LOG.info("Allocated floating address: " + str(ipaddr))
                    return ipaddr

            except Exception as e:
                pass
                
            LOG.warning("Failed to allocate floating ip " + str(i) + " times, retrying")
        
            if i >= retries:
                raise Nova_Command_Fail, "Failed to allocate floating ip  " + str(i) + " times, giving up: " + str(cmd)
                      
            time.sleep(timeout)

    @classmethod
    def _assign_floating_ip(self, vm_id, ipaddr, retries=10, timeout=20):
        for i in range(retries):
            try:
                cmd = ["nova", "add-floating-ip", str(vm_id), str(ipaddr) ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                         
                
                if rtncode == 0:
                    #check to see if it was actually assigned
                    if self.get_floating_ip_by_id(vm_id) != ipaddr:
                        raise Nova_Command_Fail, "Failed check for associated floating ip (" + str(ipaddr) + ") to vm (" + str(vm_id) + ")"
                    
                    LOG.info("Assigned floating address " + str(ipaddr) + " to vm " + str(vm_id))
                    return ipaddr

            except Exception as e:
                pass
    
            LOG.warning("Failed to associate floating ip  (" + str(ipaddr) + ") to vm (" + str(vm_id) + ") " + str(i) + " times, retrying")
            LOG.warning("Info for failed associate for vm (" + str(vm_id) + "),  rtncode: " + str(rtncode) + ", data_stdout: " + str(data_stdout) + ", data_stderr: " + str(data_stderr))

            if i >= retries:
                raise Nova_Command_Fail, str(cmd) + ": Failed to associate floating ip (" + str(ipaddr) + ") to vm (" + str(vm_id) + ") " + str(i) + " times, giving up: " + str(cmd)
    
            time.sleep(timeout)


    @classmethod
    def _cleanup_vm_by_name(self, name):
        try:
            cmd = ["nova", "delete", str(name) ]
            rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout
            
            if rtncode != 0:
                raise VM_Does_Not_Exist, str(cmd)

        except VM_Does_Not_Exist as e:
            LOG.warning("_cleanup_vm_by_name-VM_Does_Not_Exist: Usually OK, probably deleting a VM that was already deleted, " + str(type(e)) + " : " + str(e) )
            return
        except Exception as e:
            LOG.error("_cleanup_vm_by_name-Exception: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
            raise Nova_Command_Fail, "nova delete commmand failed for " + str(name) + ": " + str(cmd)

    @classmethod
    def _cleanup_vm_by_id(self, id):
        #cleanup_vm_by_id  and cleanup_vm_by_name have the same syntax and are interchangeable
        self._cleanup_vm_by_name(id)

    @classmethod
    def _cleanup_vm_by_name_poll(self, name, timeout):
        self._cleanup_vm_by_id_poll(name, timeout)
      
    @classmethod
    def _cleanup_vm_by_id_poll(self, id, timeout):
        try:
            begin = time.time()
            while True:
                time_passed = time.time() - begin
                try:
                    self._cleanup_vm_by_id(id)
                except:
                    pass
            
                if not self.exists_by_ID(id):
                    return
            
                if time_passed > timeout:
                    raise Nova_Command_Fail, "nova delete commmand failed for " + str(id)

                time.sleep(10)

        except VM_Does_Not_Exist:
            #This is the goal so its not an error
            return
        except:
            LOG.error("Cleanup instance failed: " + str(id))

    @classmethod
    def _cleanup_floating_addr(self, id, floating_addr, retries, timeout):
        
        for i in range(retries):
                
            try:
                cmd = ["nova", "remove-floating-ip", str(id), str(floating_addr) ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                       
                cmd = ["nova", "floating-ip-delete", str(floating_addr) ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                                                                       
                if rtncode == 0:
                    return 
                                                  
            except Exception as e:
                pass

            LOG.debug("_cleanup_floating_addr " + floating_addr  + "failed. retrying (" + str(i) + ")")
            time.sleep(timeout)
            
        raise Nova_Command_Fail, "VM._cleanup_floating_addr " + floating_addr  + "failed. giving up."
    
    @classmethod
    def _clean_all(self, vm_name, floating_addr=None):
        if floating_addr != None:
            try:
                self._cleanup_floating_addr(vm_name, floating_addr, 3, 60)
            except:
                pass

        try:
            self._cleanup_vm_by_name_poll(vm_name, 60*30)
        except:
            pass
        
    @classmethod
    def clean_all(self, vm_name,address=None):
        self._clean_all(vm_name,address)


    @classmethod
    def _ping_test_vm(self, floating_addr, retries, timeout):
        
        for i in range(retries):
            try:
                cmd = ["ping", "-c", "1",  str(floating_addr) ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60)                

                if rtncode == 0:
                    return True

            except Exception as e:
                pass

            LOG.warning("Failed cmd: " + str(cmd) + ", retrying (" + str(i) + ")")
            time.sleep(timeout)

        if i == retries:
            LOG.warning("Failed cmd  " + str(retries) + " times, giving up: " + str(cmd))
            return False


    @classmethod
    def _ssh_test_vm(self, floating_addr, key, user, retries, timeout):
        
        for i in range(retries):
            try:
                cmd = ["ssh", "-q", 
                       "-o", "PreferredAuthentications=publickey", 
                       "-o", "HostbasedAuthentication=no", 
                       "-o", "PasswordAuthentication=no",
                       "-o", "StrictHostKeyChecking=no",
                       "-o", "BatchMode=yes",
                       "-o", "ConnectTimeout=50",
                       "-i", str(key), 
                       str(user) + "@" + str(floating_addr),
                       "echo >/dev/null 2>&1"]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60)

                if rtncode == 0:
                    return True

            except Exception as e:
                pass

            LOG.warning("Failed cmd: " + str(cmd) + ", retrying (" + str(i) + ")")
            time.sleep(timeout)

        if i >= retries:
            LOG.warning("Failed cmd  " + str(retries) + " times, giving up: " + str(cmd))
            return False


    @classmethod
    def _start_vm_nova(self, instance_type, ami, ssh_key, user_data_file, name):
        #Boot the vm
        retries = 3
        timeout = 10
        status = None
        for i in range(retries):
            try:
                new_vm_id = None
                cmd = ["nova", "boot",
                       "--flavor", instance_type,
                       "--image", ami,
                       "--key_name", ssh_key,
                       "--user_data", user_data_file,
                       "--poll",
                       name ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60*30)
                if rtncode != 0:
                    LOG.warning("nova boot command with non-zero rtncode " + str(name) + ": " + str(cmd) +
                                ", rtncode: " + str(rtncode) +
                                ", data_stdout: " + str(data_stdout) +
                                ", data_stderr: " + str(data_stderr) +
                                ", retrying (" + str(i) + ")")
                    raise Nova_Command_Fail, str(cmd)
                
                new_vm_id = self._parse_vm_info(data_stdout)['id']
                
                status = self.get_status_by_ID(new_vm_id).lower()
                if status == 'active':
                    LOG.info('nova boot success: ' + str(new_vm_id) + " : " + str(cmd))
                    break;
                
                LOG.warning("nova boot command failed with status != active " + str(name) + ": " + str(cmd) +
                            ", status: " + str(status) +
                            ", rtncode: " + str(rtncode) +
                            ", data_stdout: " + str(data_stdout) +
                            ", data_stderr: " + str(data_stderr) +
                            ", retrying (" + str(i) + ")")
                
            except Exception as e:
                pass
            
            #clean up, maybe
            try:
                self._clean_all(new_vm_id)
            except:
                pass
            
            time.sleep(timeout)

        #if we retried too many time declare failure
        if status != 'active': # and i >= retries:
            try:
                self._clean_all(new_vm_id)
            except:
                pass
            raise Nova_Command_Fail, 'nova boot failed after ' + str(i) + ' retries, giving up'
     
        return new_vm_id                                                      

    @classmethod
    def _get_ec2_image_name(self, image_nova):
        image_ec2 = None
        
        LOG.debug("_get_ec2_image_name " + str(image_nova) )

        #get image name 
        cmd = ["glance", "show", str(image_nova) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60*30)
        
        if rtncode != 0:
            LOG.warning("glance show image_nova non-zero rtncode:  " + str(cmd) +
                        ", rtncode: " + str(rtncode) +
                        ", data_stdout: " + str(data_stdout) +
                        ", data_stderr: " + str(data_stderr))
            raise Nova_Command_Fail, str(cmd)
        
        #parse name out of euca-run-instances stdout
        lines = data_stdout.split("\n")

        name = None
        for line in lines:
            if line.startswith("Name"):
                tokens = line.split()
                if len(tokens) >= 2:
                    name = tokens[1]
                    break;

        if name == None:
            raise Nova_Command_Fail, "Failed to get name for image " + str(image_nova) + " : " + str(cmd) + ", rtncode: " + str(rtncode) +  ", data_stdout: " + str(data_stdout) + ", data_stderr: " + str(data_stderr)


        #get ec2 from name
        cmd = ["euca-describe-images" ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60*30)
        
        if rtncode != 0:
            LOG.warning("euca-describe-images non-zero rtncode:  " + str(cmd) +
                        ", rtncode: " + str(rtncode) +
                        ", data_stdout: " + str(data_stdout) +
                        ", data_stderr: " + str(data_stderr))
            raise Nova_Command_Fail, str(cmd)
        
        #parse name out of euca-run-instances stdout
        lines = data_stdout.split("\n")
        
        for line in lines:
            tokens = line.split()
            if len(tokens) >= 4:
                if tokens[3] == "(" + name + ")":
                    image_ec2 = tokens[1]
                    break;
                
        if image_ec2== None:
            raise Nova_Command_Fail, "Failed to get ec2_name for image " + str(image_nova) + " : " + str(cmd) + ", rtncode: " + str(rtncode) +  ", data_stdout: " + str(data_stdout) + ", data_stderr: " + str(data_stderr)
                
        return image_ec2
        

        
        

    @classmethod
    def _start_vm_ec2(self, instance_type, ami, aki, ari, ssh_key, user_data_file, name):
        LOG.debug("_start_vm_ec2:  " + str(name) )
        #Boot the vm
        timeout = 20
        status = None
        new_vm_id = None

        try:
            ami_ec2 = self._get_ec2_image_name(ami)
            aki_ec2 = self._get_ec2_image_name(aki)
            ari_ec2 = self._get_ec2_image_name(ari)
        except Exception as e:
            LOG.debug("something in _get_ec2_image_name failed " + str(e) )
            raise Nova_Fatal_Command_Fail, "start_vm_ec2 fatal error: can not get ec2_image name" 
    
        LOG.debug("ami_ec2 = " + ami_ec2 + ", aki_ec2 = " + aki_ec2 + ", ari_ec2 = " + ari_ec2)
                                   
        cmd = ["euca-run-instances",
               "-n", "1",
               "--addressing", "private",
               "--kernel", str(aki_ec2),
               "--ramdisk", str(ari_ec2),
               "--instance-type", str(instance_type),
               "-k", str(ssh_key),
               #"-f", str(user_data_file),
               str(ami_ec2) ]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60*30)
        if rtncode != 0:
            LOG.warning("nova boot command with non-zero rtncode " + str(name) + ": " + str(cmd) +
                        ", rtncode: " + str(rtncode) +
                        ", data_stdout: " + str(data_stdout) +
                        ", data_stderr: " + str(data_stderr))
            
            
            #check for known fatal errros (i.e. errors that a retry will not help)
            #InstanceTypeNotFoundByName
            if data_stdout.find("InstanceTypeNotFoundByName") < 0:
                raise Nova_Fatal_Command_Fail, "Fatal command fail: euca-run-instances: InstanceTypeNotFoundByName " + str(name) + " : " + str(data_stdout)
            #QuotaError
            if data_stdout.find("QuotaError") < 0:
                raise Nova_Fatal_Command_Fail, "Fatal command fail: euca-run-instances: QuotaError " + str(name) + " : " + str(data_stdout)
                    
            raise Nova_Command_Fail, str(cmd)

        LOG.debug("rtncode: " + str(rtncode) +
                  ", data_stdout: " + str(data_stdout) +
                  ", data_stderr: " + str(data_stderr) )

        #parse name out of euca-run-instances stdout
        tokens = data_stdout.split()
        LOG.debug("tokens: " + str(tokens))
        if len(tokens) < 8:
            raise Nova_Command_Fail, "Parse euca-run-instances: len(tokens) <  8: " + str(name) + " : " + str(data_stdout)

        #At this point euca-run-instance command is successful.  now test for vm success/failure 

        assigned_name = tokens[7]
        new_vm_id = self.get_ID_by_name(assigned_name)
        LOG.debug("assigned_name: " + str(assigned_name) + ", new_vm_id" + str(new_vm_id))

        #loop waiting for vm to become active.  This needs to be a long time to wait for glance to tranfer the image to the worker
        for i in range(70):
            try:
                LOG.debug('ec2 boot getting status, try(' + str(i) + "): " + str(new_vm_id))
                status = self.get_status_by_ID(new_vm_id).lower()
                if status == 'active':
                    LOG.info('ec2 boot success: ' + str(new_vm_id))
                    break;
                if status == 'error':
                    break;
            except Exception as e:
                LOG.debug('ec2 boot getting status error, try(' + str(i) + "): " + str(new_vm_id) + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
                pass

            if i < 12:
                time.sleep(10)
            else:
                time.sleep(60)

        #if we retried too many time declare failure
        if status != 'active':
            try:
                console_log = str(VM.get_console_log_by_ID(new_vm_id))
            except Exception:
                console_log = 'Cannot get console log'
            self._clean_all(new_vm_id)
            raise VM_Broken('Nova boot failed',new_vm_id,console_log)
        
        #return the uuid that nova uses (not the one ec2 uses)
        return new_vm_id
            

    @classmethod
    def start(self, instance_type, ami, aki, ari, ssh_key, startup_retries, ping_retries, ssh_retries, user_data_file, name):
        LOG.debug("start " + str(name)) 
        for i in range(startup_retries):
            try:
                new_vm_id = None
                status = None
                #new_vm_id = self._start_vm_nova(instance_type, ami, ssh_key, user_data_file, name)
                new_vm_id = self._start_vm_ec2(instance_type, ami, aki, ari, ssh_key, user_data_file, name)
                status = self.get_status_by_ID(new_vm_id).lower()
                if status == 'active':
                    break
            except Exception as e:
                self._clean_all(new_vm_id)
                if i >= startup_retries:
                    raise e
                    
        if status != 'active':
            #shouldn't get here. should be re-thrown above.  
            try:
                console_log = str(VM.get_console_log_by_ID(new_vm_id))
            except Exception:
                console_log = 'Cannot get console log'
            self._clean_all(new_vm_id)
            raise VM_Broken('THIS ERROR SHOULD NOT BE POSSIBLE (PLEASE CHECK WHY IT OCCURED):  Instance is not "ACTIVE": ' + str(name) + ', instance id ' + str(new_vm_id) + ', status ' + str(status), new_vm_id,console_log)
        
    
        floating_addr = None

        #allocate floating ip
        floating_addr = self._allocate_floating_ip()
        if floating_addr == None:
            raise Nova_Command_Fail, 'Nova Failed to allocate floating ip ' + str(new_vm_id)
               
        #assign floating ip
        self._assign_floating_ip(new_vm_id, floating_addr)

        #ping test
        if self._ping_test_vm(floating_addr, ping_retries, 10):
            LOG.info("VM " + new_vm_id + " is pingable on ip " + floating_addr)
        else:
            try:
                console_log = str(VM.get_console_log_by_ID(new_vm_id))
            except Exception:
                console_log = 'Cannot get console log'
            self._clean_all(new_vm_id)    
            raise VM_Broken_Unpingable('Instance is not pingable: ' + str(new_vm_id), new_vm_id, console_log)
            
        #ssh test
        if self._ssh_test_vm(floating_addr, os.environ['EUCA_KEY_DIR']+"/"+str(ssh_key), "root", ssh_retries, 10):
            LOG.info("VM " + new_vm_id + " is sshable on ip " + floating_addr)
        else:
            try:
                console_log = str(VM.get_console_log_by_ID(new_vm_id))
            except Exception:
                console_log = 'Cannot get console log'
            self._clean_all(new_vm_id)
            raise VM_Broken_Unsshable('Instance is not sshable: ' + str(new_vm_id), new_vm_id,console_log)

        #return the new uuid
        return new_vm_id

    @classmethod
    def stop(self,id):
        LOG.debug("in stop: " + id)
        
        exception_msg = 'Stopping ' + str(id) + ": " 
        floating_addr = None
        try:
            floating_addr = self.get_floating_ip_by_id(id)
        except:
            exception_msg += "VM has no floating address" 
            
            
        if floating_addr != None:
            try:
                self._cleanup_floating_addr(id, floating_addr, 3, 60)
            except:
                exception_msg += ", Failed cleaning up floating addr (" + str(floating_addr) + ")"
            

        try:
            self._cleanup_vm_by_id_poll(id, 60*30)
        except Exception as e:
            LOG.error("stop _cleanup_vm_by_id_poll exception: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
            exception_msg += ", Failed cleaning up vm (" + str(id) + ")"
            raise Nova_Command_Fail, str(exception_msg)
            
        return

    @classmethod
    def get_host(self, id):
        return self.get_host_by_ID(id)
    
    @classmethod
    def get_ip(self, id):
        return self.get_floating_ip_by_id(id)

    

    @classmethod
    def update_userdata(self, id, userdata_file):

        retries = 3
        timeout = 10
        
        for i in range(retries):
            cmd = None
            try:
                cmd = ["nova", "update-userdata", str(id), str(userdata_file) ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60)
                
                if rtncode == 0:
                    return True
                
            except Exception as e:
                LOG.error("update_userdata: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
                pass
                
            LOG.warning("Failed cmd: " + str(cmd) + ", retrying (" + str(i) + ")")
            time.sleep(timeout)
                
        if i == retries:
            LOG.warning("Failed cmd  " + str(retries) + " times, giving up: " + str(cmd))
            return False
                
    @classmethod
    def get_userdata(self, id):

        retries = 3
        timeout = 10

        userdata=""
        for i in range(retries):
            cmd = None
            try:
                cmd = ["nova", "get-userdata", str(id) ]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60)
                
                if rtncode == 0:
                    userdata=data_stdout
                    break

            except Exception as e:
                LOG.error("get_userdata: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
                pass

            LOG.warning("Failed cmd: " + str(cmd) + ", retrying (" + str(i) + ")")
            time.sleep(timeout)

        if i == retries:
            LOG.warning("Failed cmd  " + str(retries) + " times, giving up: " + str(cmd))
            return "failed to get userdata: exceeded retries"

        return userdata
        

    @classmethod
    def prepare_key(self, instance_ip, user_ssh_key, root_ssh_key, retries=10, timeout=3):
        import traceback

        #ip = self._get_floating_ip_by_id(id)
        ip=instance_ip
      
        for i in range(retries):
            cmd = None
            try:
                cmd = ["ssh", "-q",
                       "-o", "PreferredAuthentications=publickey",
                       "-o", "HostbasedAuthentication=no",
                       "-o", "PasswordAuthentication=no",
                       "-o", "StrictHostKeyChecking=no",
                       "-o", "BatchMode=yes",
                       "-o", "ConnectTimeout=50",
                       "-i", str(root_ssh_key),
                       str('root') + "@" + str(ip),
                       "echo " + str(user_ssh_key) + " >> .ssh/authorized_keys"]
                rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=3)
                
                if rtncode == 0:
                    return True
                
            except Exception as e:
                LOG.error("prepare-key: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
                pass

            LOG.warning("Failed cmd: " + str(cmd) + ", retrying (" + str(i) + ")")
            time.sleep(timeout)

        if i == retries:
            LOG.warning("Failed cmd  " + str(retries) + " times, giving up: " + str(cmd))
            return False

    @classmethod
    def prepare_keys(self, instance_ip, user_id, user_ssh_keys, shouldSudo, root_ssh_key, retries=1, timeout=3):
        import traceback

        LOG.debug('prepare_keys')
        LOG.debug('instance_ip: ' + str(instance_ip))
        LOG.debug('user_id:' + str(user_id))
        LOG.debug('user_ssh_keys:' + str(user_ssh_keys))
        LOG.debug('shouldSudo:' + str(shouldSudo))
        LOG.debug('root_ssh_key:' + str(root_ssh_key))
        

        if user_id == 'root':
            for key in user_ssh_keys:
                VM.prepare_key(instance_ip, key, root_ssh_key)
            
            return
        
        else:
            #if its a new user
            ip=instance_ip
            
            # create account and add to sudoers                                                                                                                                                                                                                                           
            #ssh $SSH_OPTS -i $KEY root@${machine} "useradd -m $user_login"
            for i in range(retries):
                cmd = None
                try:
                    cmd = ["ssh", "-q",
                           "-o", "PreferredAuthentications=publickey",
                           "-o", "HostbasedAuthentication=no",
                           "-o", "PasswordAuthentication=no",
                           "-o", "StrictHostKeyChecking=no",
                           "-o", "BatchMode=yes",
                           "-o", "ConnectTimeout=50",
                           "-i", str(root_ssh_key),
                           str('root') + "@" + str(ip),
                           "useradd -m " + str(user_id) ]
                    rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=3)
                
                    if rtncode == 0:
                        break
                        
                except Exception as e:
                    LOG.error("prepare-keys: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
                    return

                LOG.warning("prepare-keys: ssh failed, retrying (" + str(i) +  "). " + str(cmd))
                time.sleep(timeout)

            if i == retries:
                LOG.warning("Failed cmd  " + str(retries) + " times, giving up: " + str(cmd))
                return False

            #ssh $SSH_OPTS -i $KEY root@${machine} "echo '${user_login} ALL=(ALL)  ALL' >> /etc/sudoers"
            if shouldSudo.lower() == 'yes':
                for i in range(retries):
                    cmd = None
                    try:
                        cmd = ["ssh", "-q",
                               "-o", "PreferredAuthentications=publickey",
                               "-o", "HostbasedAuthentication=no",
                               "-o", "PasswordAuthentication=no",
                               "-o", "StrictHostKeyChecking=no",
                               "-o", "BatchMode=yes",
                               "-o", "ConnectTimeout=50",
                               "-i", str(root_ssh_key),
                               str('root') + "@" + str(ip),
                               "echo '" + str(user_id)  + " ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers" ]
                        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=3)

                        if rtncode == 0:
                            break
                    
                    except Exception as e:
                        LOG.error("prepare-keys: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
                        return

                    LOG.warning("prepare-keys: ssh failed, retrying (" + str(i) +  "). " + str(cmd))
                    time.sleep(timeout)

                if i == retries:
                    LOG.warning("Failed cmd  " + str(retries) + " times, giving up: " + str(cmd))
                    return False
            else:
                LOG.debug("Skipping adding user to sudoers")


            # USERHOMEPREFIX=`ssh $SSH_OPTS -i $KEY root@${machine} "useradd -D | grep HOME | awk '{ split(\\$0, a, \"=\"); print a[2]}'"`
            # guess home_prefix == /home then try to find reall prefix
            user_home_prefix='/home'
            for i in range(retries):
                cmd = None
                try:
                    cmd = ["ssh", "-q",
                           "-o", "PreferredAuthentications=publickey",
                           "-o", "HostbasedAuthentication=no",
                           "-o", "PasswordAuthentication=no",
                           "-o", "StrictHostKeyChecking=no",
                           "-o", "BatchMode=yes",
                           "-o", "ConnectTimeout=50",
                           "-i", str(root_ssh_key),
                           str('root') + "@" + str(ip),
                           "useradd -D | grep HOME | awk '{ split(\\$0, a, \"=\"); print a[2]}'" ]
                    rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=3)

                    if rtncode == 0:
                        user_home_prefix = data_stdout
                        break

                except Exception as e:
                    LOG.error("prepare-keys: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
            
                LOG.warning("prepare-keys: ssh failed, retrying (" + str(i) +  "). " + str(cmd))
                time.sleep(timeout)

            if i == retries:
                LOG.warning("Failed cmd  " + str(retries) + " times, giving up: " + str(cmd))
                
            
            #ssh $SSH_OPTS -i $KEY root@${machine} "echo ${user_key} >> ${USERHOMEPREFIX}/${user_login}/.ssh/authorized_keys"
            for user_key in user_ssh_keys:
                for i in range(retries):
                    cmd = None
                    try:
                        cmd = ["ssh", "-q",
                               "-o", "PreferredAuthentications=publickey",
                               "-o", "HostbasedAuthentication=no",
                               "-o", "PasswordAuthentication=no",
                               "-o", "StrictHostKeyChecking=no",
                               "-o", "BatchMode=yes",
                               "-o", "ConnectTimeout=50",
                               "-i", str(root_ssh_key),
                               str('root') + "@" + str(ip),
                               "mkdir -p " + str(user_home_prefix) + "/" + str(user_id) + "/.ssh; echo " + str(user_key)  +  " >> " + str(user_home_prefix) + "/" + str(user_id) + "/.ssh/authorized_keys; chown -R " + str(user_id) + ":" + str(user_id)
 + " " + str(user_home_prefix) + "/" + str(user_id) + "/.ssh"  ]
                        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=3)

                        if rtncode == 0:
                            break

                    except Exception as e:
                        LOG.error("prepare-keys: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
                        return

                    LOG.warning("prepare-keys: ssh failed, retrying (" + str(i) +  "). " + str(cmd))
                    time.sleep(timeout)

                    if i == retries:
                        LOG.warning("Failed cmd  " + str(retries) + " times, giving up: " + str(cmd))
                        return False

            
# Upon import, read in the needed OpenStack credentials from one of the right places.
if (os.path.isfile(os.environ['EUCA_KEY_DIR'] + "/novarc")):
    Commands.source(os.environ['EUCA_KEY_DIR'] + "/novarc")
elif (os.path.isfile(os.environ['EUCA_KEY_DIR'] + "/openrc")):
    Commands.source(os.environ['EUCA_KEY_DIR'] + "/openrc")
else:
    pass
