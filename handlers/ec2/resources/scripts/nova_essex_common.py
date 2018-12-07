#!/usr/bin/env python                                                                                                                                                   

import logging as LOG
import os
import json 
import signal
import time
import traceback
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
        Exception.__init__(self, message)
        self.vm_id = str(vm_id)
        self.console_log = str(console_log)

    def get_console_log(self):
        return str(self.console_log)

    def get_vm_id(self):
        return str(self.vm_id)


class VM_Broken_Unpingable(VM_Broken):
    def __init__(self, message, vm_id, console_log):
        VM_Broken.__init__(self, message, vm_id, console_log)


class VM_Broken_Unsshable(VM_Broken):
    def __init__(self, message, vm_id, console_log):
        VM_Broken.__init__(self, message, vm_id, console_log)


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
                cmd = ["nova", "console-log", str(id)]
                rtncode, data_stdout, data_stderr = Commands.run(cmd,
                                                                 timeout=60)  # TODO: needs real timeout

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
                cmd = ["openstack", "server", "show", str(id), "-fjson"]
                rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

                if rtncode != 0:
                    raise VM_Does_Not_Exist, str(cmd)

                vm_info = json.loads(data_stdout)
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
            raise Nova_Command_Fail, "No value for " + str(field) + " in info for " + str(id)

    @classmethod
    def _get_info_by_name(self, name, field):
        # name and id are interchangible for this purpose
        try:
            return self._get_info_by_ID(name, field)
        except Exception as e:
            LOG.error("_get_info_by_name: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
            raise e

    @classmethod
    def get_state_by_ID(self, id):
        return self._get_info_by_ID(id, 'OS-EXT-STS:vm_state')

    @classmethod
    def get_host_by_ID(self, id):
        return self._get_info_by_ID(id, 'OS-EXT-SRV-ATTR:host')

    @classmethod
    def get_instance_name_by_ID(self, id):
        return self._get_info_by_ID(id, 'OS-EXT-SRV-ATTR:instance_name')

    @classmethod
    def get_date_created_by_ID(self, id):
        return self._get_info_by_ID(id, 'created')

    @classmethod
    def get_instance_type_by_ID(self, id):
        return self._get_info_by_ID(id, 'flavor')

    @classmethod
    def get_image_by_ID(self, id):
        return self._get_info_by_ID(id, 'image')

    @classmethod
    def get_name_by_ID(self, id):
        return self._get_info_by_ID(id, 'name')

    @classmethod
    def get_status_by_ID(self, id):
        return self._get_info_by_ID(id, 'status')

    @classmethod
    def exists_by_ID(self, id):
        info = self._get_all_info_by_ID(id)
        if len(info) > 0:
            return True
        else:
            return False

    @classmethod
    def get_ID_by_name(self, name):
        return self._get_info_by_name(name, 'id')

    @classmethod
    def get_console_log_by_ID(self, id):
        return self._get_console_log_by_ID(id)

    @classmethod
    def get_floating_ip_by_id(self, id):
        cmd = ["openstack", "server", "show", id, "-f", "value", "-c", "addresses"]
        rtncode, data_stdout, data_stderr = Commands.run(cmd,
                                                         timeout=60)  # TODO: needs real timeout
        LOG.debug("rtncode=" + str(rtncode))
        LOG.debug("data_stdout=" + str(data_stdout))
        LOG.debug("data_stderr=" + str(data_stderr))

        if rtncode != 0:
            raise Nova_Command_Fail, str(cmd) + ": Failed to list floating ips"

        addresses = data_stdout.strip().split(";")
        floating_ip = None
        for add in addresses:
            add.strip()
            if add.find(",") != -1:
               ips=add.split(",")
               floating_ip = ips[1].strip()

        return floating_ip 

    @classmethod
    def _allocate_floating_ip(self, project, network, retries=10, timeout=20):
        for i in range(retries):
            try:

                cmd = ["openstack", "floating", "ip", "create", "-f", "value", "-c", "floating_ip_address", "--project", project, network]
                rtncode, data_stdout, data_stderr = Commands.run(cmd,
                                                                 timeout=60)  # TODO: needs real timeout

                if rtncode == 0:
                    return data_stdout.strip() 

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
                cmd = ["openstack", "server", "add", "floating", "ip", str(vm_id), str(ipaddr)]
                rtncode, data_stdout, data_stderr = Commands.run(cmd,
                                                                 timeout=60)  # TODO: needs real timeout

                if rtncode == 0:
                    # check to see if it was actually assigned
                    if self.get_floating_ip_by_id(vm_id) != ipaddr:
                        raise Nova_Command_Fail, "Failed check for associated floating ip (" + str(
                            ipaddr) + ") to vm (" + str(vm_id) + ")"

                    LOG.info("Assigned floating address " + str(ipaddr) + " to vm " + str(vm_id))
                    return ipaddr

            except Exception as e:
                LOG.debug( "Exception: " + str(type(e)) + " : " + str(e))
                pass

            LOG.warning("Failed to associate floating ip  (" + str(ipaddr) + ") to vm (" + str(vm_id) + ") " + str(
                i) + " times, retrying")
            LOG.warning("Info for failed associate for vm (" + str(vm_id) + "),  rtncode: " + str(
                rtncode) + ", data_stdout: " + str(data_stdout) + ", data_stderr: " + str(data_stderr))

            if i >= retries:
                raise Nova_Command_Fail, str(cmd) + ": Failed to associate floating ip (" + str(
                    ipaddr) + ") to vm (" + str(vm_id) + ") " + str(i) + " times, giving up: " + str(cmd)

            time.sleep(timeout)

    @classmethod
    def _cleanup_vm_by_name(self, name):
        try:
            cmd = ["nova", "delete", str(name)]
            rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

            if rtncode != 0:
                raise VM_Does_Not_Exist, str(cmd)

        except VM_Does_Not_Exist as e:
            LOG.warning(
                "_cleanup_vm_by_name-VM_Does_Not_Exist: Usually OK, probably deleting a VM that was already deleted, " + str(
                    type(e)) + " : " + str(e))
            return
        except Exception as e:
            LOG.error(
                "_cleanup_vm_by_name-Exception: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
            raise Nova_Command_Fail, "nova delete commmand failed for " + str(name) + ": " + str(cmd)

    @classmethod
    def _cleanup_vm_by_id(self, id):
        # cleanup_vm_by_id  and cleanup_vm_by_name have the same syntax and are interchangeable
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
            # This is the goal so its not an error
            return
        except:
            LOG.error("Cleanup instance failed: " + str(id))

    @classmethod
    def _cleanup_floating_addr(self, id, floating_addr, retries, timeout):

        for i in range(retries):

            try:
                cmd = ["openstack", "server", "remove", "floating", "ip", str(id), str(floating_addr)]
                rtncode, data_stdout, data_stderr = Commands.run(cmd,
                                                                 timeout=60)  # TODO: needs real timeout
                cmd = ["openstack", "floating", "ip", "delete", str(floating_addr)]
                rtncode, data_stdout, data_stderr = Commands.run(cmd,
                                                                 timeout=60)  # TODO: needs real timeout
                if rtncode == 0:
                    return

            except Exception as e:
                pass

            LOG.debug("_cleanup_floating_addr " + floating_addr + "failed. retrying (" + str(i) + ")")
            time.sleep(timeout)

        raise Nova_Command_Fail, "VM._cleanup_floating_addr " + floating_addr + "failed. giving up."

    @classmethod
    def _clean_all(self, vm_name, floating_addr=None):
        if floating_addr != None:
            try:
                self._cleanup_floating_addr(vm_name, floating_addr, 3, 60)
            except:
                pass

        try:
            self._cleanup_vm_by_name_poll(vm_name, 60 * 30)
        except:
            pass

    @classmethod
    def clean_all(self, vm_name, address=None):
        self._clean_all(vm_name, address)

    @classmethod
    def _ping_test_vm(self, floating_addr, retries, timeout):

        for i in range(retries):
            try:
                cmd = ["ping", "-c", "1", str(floating_addr)]
                rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)

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

        if (os.environ['EC2_SSH_TIMEOUT']).isdigit():
            ssh_timeout = int(os.environ['EC2_SSH_TIMEOUT'])
        else:
            ssh_timeout = 20

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
                rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=ssh_timeout)

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
    def _start_vm_nova(self, instance_type, img, ssh_key, user_data_file, name, mgmt_network, sec_group):
        # get management network id
        cmd = ["openstack", "network", "show", "-c", "id", "-f", "value", mgmt_network]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))
	
        if rtncode != 0:
            raise Nova_Command_Fail, "network show failed, bad error code (" + str(rtncode) + ") : " + str(cmd)
        mgmt_net_id="net-id=" + str(data_stdout).strip()

        # Boot the vm
        retries = 3
        timeout = 10
        status = None
        for i in range(retries):
            try:
                # KOMAL 
                instance_type="m1.small"
                new_vm_id = None
                cmd = ["openstack", "server", "create",
                       "--flavor", instance_type,
                       "--security-group", sec_group,
                       "--image", img,
                       "--key-name", ssh_key,
                       "--nic", mgmt_net_id,
                       "--user-data", user_data_file,
                       "--wait",
                       name, "-fjson"]
                rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
                if rtncode != 0:
                    LOG.warning("nova boot command with non-zero rtncode " + str(name) + ": " + str(cmd) +
                                ", rtncode: " + str(rtncode) +
                                ", data_stdout: " + str(data_stdout) +
                                ", data_stderr: " + str(data_stderr) +
                                ", retrying (" + str(i) + ")")
                    raise Nova_Command_Fail, str(cmd)

                new_vm_id = json.loads(data_stdout)['id']

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

            # clean up, maybe
            try:
                self._clean_all(new_vm_id)
            except:
                pass

            time.sleep(timeout)

        # if we retried too many time declare failure
        if status != 'active':  # and i >= retries:
            try:
                self._clean_all(new_vm_id)
            except:
                pass
            raise Nova_Command_Fail, 'nova boot failed after ' + str(i) + ' retries, giving up'

        return new_vm_id

    @classmethod
    def start(self, instance_type, ami, qcow2, aki, ari, ssh_key, startup_retries, ping_retries, ssh_retries, user_data_file,
              name, mgmt_network, sec_group, project, public_network):
        LOG.debug("start " + str(name))
        img = ami 
        if qcow2 is not None:
           img = qcow2
        for i in range(startup_retries):
            try:
                new_vm_id = None
                status = None
                new_vm_id = self._start_vm_nova(instance_type, img, ssh_key, user_data_file, name, mgmt_network, sec_group)
                status = self.get_status_by_ID(new_vm_id).lower()
                if status == 'active':
                    break
            except Exception as e:
                self._clean_all(new_vm_id)
                if i >= startup_retries:
                    raise e

        if status != 'active':
            # shouldn't get here. should be re-thrown above.
            try:
                console_log = str(VM.get_console_log_by_ID(new_vm_id))
            except Exception:
                console_log = 'Cannot get console log'
            self._clean_all(new_vm_id)
            raise VM_Broken(
                'THIS ERROR SHOULD NOT BE POSSIBLE (PLEASE CHECK WHY IT OCCURED):  Instance is not "ACTIVE": ' + str(
                    name) + ', instance id ' + str(new_vm_id) + ', status ' + str(status), new_vm_id, console_log)

        floating_addr = None

        # https://github.com/RENCI-NRIG/orca5/pull/166/files
        # https://github.com/RENCI-NRIG/exogeni/issues/117
        # allocate floating ip
        try:
            floating_addr = self._allocate_floating_ip(project, public_network)
        except Exception as e:
            LOG.info('Nova failed to allocate floating ip to the VM ' + str(new_vm_id))

        if floating_addr is not None:
            LOG.info("VM " + new_vm_id + " is allocated floating ip " + floating_addr)
            # assign floating ip
            self._assign_floating_ip(new_vm_id, floating_addr)
        else:
            try:
                console_log = str(VM.get_console_log_by_ID(new_vm_id))
            except Exception:
                console_log = 'Cannot get console log'
            self._clean_all(new_vm_id)
            raise VM_Broken('Nova failed to allocate floating ip to the VM ' + str(new_vm_id), new_vm_id, console_log)

        # ping test
        if self._ping_test_vm(floating_addr, ping_retries, 10):
            LOG.info("VM " + new_vm_id + " is pingable on ip " + floating_addr)
        else:
            try:
                console_log = str(VM.get_console_log_by_ID(new_vm_id))
            except Exception:
                console_log = 'Cannot get console log'
            self._clean_all(new_vm_id)
            raise VM_Broken_Unpingable('Instance is not pingable: ' + str(new_vm_id), new_vm_id, console_log)

        # FIXME: Liveness of the VMs is determined by SSHing presuming SSH server is running.
        # Common means to determine liveness of VMs is needed.
        # Currently, liveness check (by ssh) is executed only for VMs that are booted from images
        # with ami, qcow2, aki,ari components with the assumption that Unix-based VM images will have aki and ari.
        # Non-Unix-based (Windows) VMs are booted from images that have only ami and liveness check is bypassed.

        # if strings specifying ami, aki and ari are not empty (and not None) - not a windows image, can test SSH
        if ami and qcow2 and aki and ari:
            # ssh test
            LOG.info("Testing SSH reachability for VM " + new_vm_id)
            if self._ssh_test_vm(floating_addr, os.environ['EUCA_KEY_DIR'] + "/" + str(ssh_key), "root", ssh_retries,
                                 10):
                LOG.info("VM " + new_vm_id + " is sshable on ip " + floating_addr)
            else:
                try:
                    console_log = str(VM.get_console_log_by_ID(new_vm_id))
                except Exception:
                    console_log = 'Cannot get console log'
                self._clean_all(new_vm_id)
                raise VM_Broken_Unsshable('Instance is not sshable: ' + str(new_vm_id), new_vm_id, console_log)

        # return the new uuid
        return new_vm_id

    @classmethod
    def stop(self, id):
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
            self._cleanup_vm_by_id_poll(id, 60 * 30)
        except Exception as e:
            LOG.error("stop _cleanup_vm_by_id_poll exception: " + str(type(e)) + " : " + str(e) + "\n" + str(
                traceback.format_exc()))
            exception_msg += ", Failed cleaning up vm (" + str(id) + ")"
            raise Nova_Command_Fail, str(exception_msg)

        return

    @classmethod
    def get_host(self, id):
        return self.get_host_by_ID(id)

    @classmethod
    def get_ip(self, id):
        return self.get_floating_ip_by_id(id)

# Upon import, read in the needed OpenStack credentials from one of the right places.
if (os.path.isfile(os.environ['EUCA_KEY_DIR'] + "/novarc")):
    Commands.source(os.environ['EUCA_KEY_DIR'] + "/novarc")
elif (os.path.isfile(os.environ['EUCA_KEY_DIR'] + "/openrc")):
    Commands.source(os.environ['EUCA_KEY_DIR'] + "/openrc")
else:
    pass
