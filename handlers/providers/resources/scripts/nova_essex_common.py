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

class Project_Exception(Exception):
    pass

class User_Exception(Exception):
    pass

class Network_Exception(Exception):
    pass

class VM_Exception(Exception):
    pass


class VM_Does_Not_Exist(VM_Exception):
    pass


class Openstack_Command_Fail(VM_Exception):
    pass


class Openstack_Fatal_Command_Fail(Openstack_Command_Fail):
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

class Project:
    @classmethod
    def _cleanup_user_by_name(self, name):
        try:
            cmd = ["openstack", "user", "delete", str(name)]
            rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

            if rtncode != 0:
                raise User_Exception, str(cmd)

        except User_Exception as e:
            LOG.warning(
                "_cleanup_user_by_name-User_Exception: Usually OK, probably deleting a User that was already deleted, " + str(
                    type(e)) + " : " + str(e))
            return
        except Exception as e:
            LOG.error(
                "_cleanup_user_by_name-Exception: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
            raise Openstack_Command_Fail, "openstack user delete commmand failed for " + str(name) + ": " + str(cmd)

    @classmethod
    def _cleanup_project_by_name(self, name):
        try:
            cmd = ["openstack", "project", "delete", str(name)]
            rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60)  # TODO: needs real timeout

            if rtncode != 0:
                raise Project_Exception, str(cmd)

        except Project_Exception as e:
            LOG.warning(
                "_cleanup_project_by_name-Project_Exception: Usually OK, probably deleting a Project that was already deleted, " + str(
                    type(e)) + " : " + str(e))
            return
        except Exception as e:
            LOG.error(
                "_cleanup_project_by_name-Exception: " + str(type(e)) + " : " + str(e) + "\n" + str(traceback.format_exc()))
            raise Openstack_Command_Fail, "openstack project delete commmand failed for " + str(name) + ": " + str(cmd)

    @classmethod
    def get_project(self, project_name):
        # get project if it already exists
        cmd = ["openstack", "project", "show", "-f", "value", "-c", "id", str(project_name)]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        if rtncode != 0:
           LOG.warning("openstack project show command with non-zero rtncode " + str(project_name) + ": " + str(cmd) +
                       ", rtncode: " + str(rtncode) +
                       ", data_stdout: " + str(data_stdout) +
                       ", data_stderr: " + str(data_stderr)) 
           return None
        else :
           LOG.debug("Project " + str(project_name) + " exists")
           return data_stdout.strip()

    @classmethod
    def create_project(self, project_name):
        new_project_id = None
        new_project_id = self.get_project(project_name)

        if new_project_id is not None:
            LOG.debug("Project " + str(project_name) + " exists")
            return new_project_id 

        retries = 3
        timeout = 10
        status = None
        for i in range(retries):
            try:
                new_project_id = None
                cmd = ["openstack", "project", "create",
                       "--description", str(project_name),
                       str(project_name), "-fjson"]
                rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
                if rtncode != 0:
                    LOG.warning("openstack project create command with non-zero rtncode " + str(project_name) + ": " + str(cmd) +
                                ", rtncode: " + str(rtncode) +
                                ", data_stdout: " + str(data_stdout) +
                                ", data_stderr: " + str(data_stderr) +
                                ", retrying (" + str(i) + ")")

                    # check if project create succeeded for another join
                    new_project_id = self.get_project(project_name)
                    if new_project_id is not None:
                        LOG.debug("Project " + str(project_name) + " exists")
                        return new_project_id
                    raise Openstack_Command_Fail, str(cmd)

                new_project_id = json.loads(data_stdout)['id']
                if new_project_id is not None:
                    LOG.info('openstack project create success: ' + str(new_project_id) + " : " + str(cmd))
                    break;

            except Exception as e:
                pass

            time.sleep(timeout)

        # if we retried too many time declare failure
        if new_project_id is None:  # and i >= retries:
            try:
                self._cleanup_project_by_name(project_name)
            except:
                pass
            raise Openstack_Command_Fail, 'openstack failed to create project ' + str(i) + ' retries, giving up'

        return new_project_id 


    @classmethod
    def generate_user_keystone_file(self, project_name, user_name, user_pwd, ec2_auth_url):
        keystone_cred_file = self.get_keystone_file_name(project_name, user_name)
        if (os.path.isfile(keystone_cred_file)):
            LOG.debug(keystone_cred_file + " already exists")
            time.sleep(4)
            return keystone_cred_file

        fd = None
        try:
            fd = open(keystone_cred_file, 'a+')
            fd.seek(0)
            fd.truncate()
            fd.write("unset OS_SERVICE_TOKEN\n")
            fd.write("    export OS_USERNAME=" + user_name + "\n")
            fd.write("    export OS_PASSWORD=" + user_pwd + "\n")
            fd.write("    export OS_AUTH_URL=" + ec2_auth_url + "\n")
            fd.write("    export PS1='[\u@\h \W(keystone_" + project_name + ")]\$ '\n")
            fd.write("export OS_PROJECT_NAME=" + project_name + "\n")
            fd.write("export OS_USER_DOMAIN_NAME=Default\n")
            fd.write("export OS_PROJECT_DOMAIN_NAME=Default\n")
            fd.write("export OS_IDENTITY_API_VERSION=3\n")
            fd.close()
        except Exception as e:
            LOG.error("Exception occured while writing to " + keystone_cred_file + " file")
            LOG.error("Exception e=" + str(e))
            raise Openstack_Command_Fail, 'failed to write to cred file ' + keystone_cred_file
        LOG.debug("Successfully generated " + keystone_cred_file + " file")
        return keystone_cred_file

    @classmethod
    def get_user(self, user_name):
        # get user if it already exists
        cmd = ["openstack", "user", "show", "-f", "value", "-c", "id", str(user_name)]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        new_user_id = None
        if rtncode == 0:
            new_user_id = data_stdout.strip()
            LOG.debug("User " + new_user_id + " exists")
        else:
            LOG.error("openstack user show command with non-zero rtncode " + str(user_name) + ": " + str(cmd) +
                      ", rtncode: " + str(rtncode) +
                      ", data_stdout: " + str(data_stdout) +
                      ", data_stderr: " + str(data_stderr))
        return new_user_id

    @classmethod
    def create_user(self, project_name, user_name, user_email, user_pwd, role, admin_user):
        new_user_id = None
        new_user_id = self.get_user(user_name)
        if new_user_id is not None:
           LOG.debug("User " + user_name + " exists")
           return new_user_id

        retries = 3
        timeout = 10
        for i in range(retries):
            try:
                new_user_id = None
                cmd = ["openstack", "user", "create",
                       "--project", str(project_name),
                       "--password", str(user_pwd),
                       "--email", str(user_email),
                       str(user_name), "-fjson"]
                rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
                if rtncode != 0:
                    LOG.warning("openstack user create command with non-zero rtncode " + str(user_name) + ": " + str(cmd) +
                                ", rtncode: " + str(rtncode) +
                                ", data_stdout: " + str(data_stdout) +
                                ", data_stderr: " + str(data_stderr) +
                                ", retrying (" + str(i) + ")")

                    # check if user has been created by another join
                    new_user_id = self.get_user(user_name)
                    if new_user_id is not None:
                       time.sleep(timeout)
                       LOG.debug("User " + user_name + " exists")
                       return new_user_id

                    raise Openstack_Command_Fail, str(cmd)

                new_user_id = json.loads(data_stdout)['id']
                if new_user_id is not None:
                    LOG.info('openstack user create success: ' + str(new_user_id) + " : " + str(cmd))
                    break;

            except Exception as e:
                pass

            time.sleep(timeout)

        # if we retried too many time declare failure
        if new_user_id is None:  # and i >= retries:
            try:
                self._cleanup_user_by_name(user_name)
            except:
                pass
            raise Openstack_Command_Fail, 'openstack failed to create project ' + str(i) + ' retries, giving up'

        cmd = ["openstack", "role", "add", "--user", str(user_name), "--project", str(project_name), str(role)]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
        if rtncode != 0:
            LOG.warning("openstack role add command with non-zero rtncode " + str(user_name) + ": " + str(cmd) +
                        ", rtncode: " + str(rtncode) +
                        ", data_stdout: " + str(data_stdout) +
                        ", data_stderr: " + str(data_stderr))
            raise Openstack_Command_Fail, str(cmd)

        cmd = ["openstack", "role", "add", "--user", str(admin_user), "--project", str(project_name), str(role)] 
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
        if rtncode != 0:
            LOG.warning("openstack role add command with non-zero rtncode " + str(user_name) + ": " + str(cmd) +
                        ", rtncode: " + str(rtncode) +
                        ", data_stdout: " + str(data_stdout) +
                        ", data_stderr: " + str(data_stderr))
            raise Openstack_Command_Fail, str(cmd)

        return new_user_id 

    @classmethod
    def get_management_network(self, mgmt_network):
        # get management network id
        cmd = ["openstack", "network", "show", "-c", "id", "-f", "value", str(mgmt_network)]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))
    
        if rtncode == 0:
            nw_id = data_stdout.strip()
            LOG.debug(mgmt_network + "=" + nw_id)
            return nw_id
        return None

    @classmethod
    def get_default_security_group(self, project_name = None):
        # get project if it already exists
        cmd = None
        if project_name is not None:
            cmd = ["openstack", "security", "group", "list", "--project", str(project_name), "-f", "json", "-c", "ID"]
        else:
            cmd = ["openstack", "security", "group", "list", "-f", "json", "-c", "ID"]

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        if rtncode != 0:
           LOG.warning("openstack security group rule list command with non-zero rtncode " + 
                       str(project_name) + ": " + str(cmd) +
                       ", rtncode: " + str(rtncode) +
                       ", data_stdout: " + str(data_stdout) +
                       ", data_stderr: " + str(data_stderr)) 
           return 

        group_list=json.loads(data_stdout.strip())
        return group_list

    @classmethod
    def delete_default_security_group(self, project_name):
        group_list=self.get_default_security_group(project_name)
        for g in group_list: 
            cmd = ["openstack", "security", "group", "delete", str(g['ID']) ]
            rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

            LOG.debug("rtncode: " + str(rtncode))
            LOG.debug("data_stdout: " + str(data_stdout))
            LOG.debug("data_stderr: " + str(data_stderr))

            if rtncode != 0:
                LOG.warning("openstack security group rule delete command with non-zero rtncode " + 
                            str(project_name) + ": " + str(cmd) +
                            ", rtncode: " + str(rtncode) +
                            ", data_stdout: " + str(data_stdout) +
                            ", data_stderr: " + str(data_stderr)) 

    @classmethod
    def get_default_security_group_rules(self, sg_id):
        # get project if it already exists
        cmd = ["openstack", "security", "group", "rule", "list", str(sg_id), "-f", "json", "-c", "ID"]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        if rtncode != 0:
           LOG.warning("openstack security group rule list command with non-zero rtncode " + 
                       ": " + str(cmd) +
                       ", rtncode: " + str(rtncode) +
                       ", data_stdout: " + str(data_stdout) +
                       ", data_stderr: " + str(data_stderr)) 
           return None

        rule_list=json.loads(data_stdout.strip())
        return rule_list


    @classmethod
    def delete_default_security_group_rules(self, sg_id):
        rule_list=self.get_default_security_group_rules(sg_id)
        for rule in rule_list: 
            cmd = ["openstack", "security", "group", "rule", "delete", str(rule['ID']) ]
            rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

            LOG.debug("rtncode: " + str(rtncode))
            LOG.debug("data_stdout: " + str(data_stdout))
            LOG.debug("data_stderr: " + str(data_stderr))

            if rtncode != 0:
                LOG.warning("openstack security group rule delete command with non-zero rtncode " + 
                            ": " + str(cmd) +
                            ", rtncode: " + str(rtncode) +
                            ", data_stdout: " + str(data_stdout) +
                            ", data_stderr: " + str(data_stderr)) 

    @classmethod
    def create_security_group_rules(self):
        sg_id=self.get_default_security_group()
        if sg_id is None or len(sg_id) < 1:
            return
        rule_list=self.get_default_security_group_rules(sg_id[0]['ID'])

        if len(rule_list) > 4:
            LOG.debug("Rules already exist")
            return   
        
        cmd = ["openstack", "security", "group", "rule", "create", 
               "--ingress", 
               "--ethertype", "IPv4", 
               "--protocol", "icmp", 
               "--remote-ip", "0.0.0.0/0",
               "default",
               "-fjson"]

        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
        if rtncode != 0:
            LOG.warning("openstack security group rule create command with non-zero rtncode " +
                        ": " + str(cmd) +
                        ", rtncode: " + str(rtncode) +
                        ", data_stdout: " + str(data_stdout) +
                        ", data_stderr: " + str(data_stderr))
            if data_stdout.find("SecurityGroupRuleExists") == -1:
                raise Openstack_Command_Fail, str(cmd)

        cmd = ["openstack", "security", "group", "rule", "create", 
               "--ingress", 
               "--ethertype", "IPv4", 
               "--protocol", "tcp", 
               "--dst-port", "22",
               "--remote-ip", "0.0.0.0/0",
               "default",
               "-fjson"]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
        if rtncode != 0:
            LOG.warning("openstack security group rule create command with non-zero rtncode " + 
                        ": " + str(cmd) +
                        ", rtncode: " + str(rtncode) +
                        ", data_stdout: " + str(data_stdout) +
                        ", data_stderr: " + str(data_stderr))
            if data_stdout.find("SecurityGroupRuleExists") == -1:
                raise Openstack_Command_Fail, str(cmd)

        cmd = ["openstack", "security", "group", "rule", "create", 
               "--ingress", 
               "--ethertype", "IPv6", 
               "--protocol", "tcp", 
               "--remote-ip", "::/0",
               "default",
               "-fjson"]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
        if rtncode != 0:
            LOG.warning("openstack security group rule create command with non-zero rtncode " + 
                        ": " + str(cmd) +
                        ", rtncode: " + str(rtncode) +
                        ", data_stdout: " + str(data_stdout) +
                        ", data_stderr: " + str(data_stderr))
            if data_stdout.find("SecurityGroupRuleExists") == -1:
                raise Openstack_Command_Fail, str(cmd)

        cmd = ["openstack", "security", "group", "rule", "create", 
               "--ingress", 
               "--ethertype", "IPv6", 
               "--protocol", "udp", 
               "--remote-ip", "::/0",
               "default",
               "-fjson"]
        rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
        if rtncode != 0:
            LOG.warning("openstack security group rule create command with non-zero rtncode " + 
                        ": " + str(cmd) +
                        ", rtncode: " + str(rtncode) +
                        ", data_stdout: " + str(data_stdout) +
                        ", data_stderr: " + str(data_stderr))
            if data_stdout.find("SecurityGroupRuleExists") == -1:
                raise Openstack_Command_Fail, str(cmd)

    @classmethod
    def get_key_pair(self, user_name):
        cmd = ["openstack", "keypair", "show", "-c", "id", "-f", "value", user_name]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))
    
        if rtncode == 0:
            LOG.debug(user_name + " already exists")
            return data_stdout

        return None

    @classmethod
    def generate_key_file(self, user_name, ssh_key):
        key_file=self.get_key_file(user_name)
        if (os.path.isfile(key_file)):
            LOG.debug(key_file + " already exists")
            time.sleep(4)
            return key_file 

        fd = None
        try:
            fd = open(key_file, 'a+')
            fd.seek(0)
            fd.truncate()
            fd.write(ssh_key)
            fd.close
        except Exception as e:
            LOG.error("Exception occured e=" + str(e))
            raise Openstack_Command_Fail, 'failed to write to key file ' + key_file 
        LOG.debug("Successfully generated " + key_file + " file")
        return key_file

    @classmethod
    def create_key_pair(self, user_name, kp_file):
        new_kp_id = None
        new_kp_id = self.get_key_pair(user_name)

        if new_kp_id is not None:
            LOG.debug(user_name + " already exists")
            return new_kp_id 

        retries = 3
        timeout = 10
        status = None
        for i in range(retries):
            try:
                new_kp_id = None
                cmd = ["openstack", "keypair", "create", 
                       "--public-key", str(kp_file),
                       str(user_name),
                       "-fjson"]
                rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
                if rtncode != 0:
                    LOG.warning("openstack keypair create command with non-zero rtncode " + str(user_name) + ": " + str(cmd) +
                                ", rtncode: " + str(rtncode) +
                                ", data_stdout: " + str(data_stdout) +
                                ", data_stderr: " + str(data_stderr) +
                                ", retrying (" + str(i) + ")")
                    new_kp_id = self.get_key_pair(user_name)
                    if new_kp_id is not None:
                        LOG.debug(user_name + " already exists")
                        return new_kp_id 

                    raise Openstack_Command_Fail, str(cmd)

                new_kp_id = json.loads(data_stdout)['id']

                if new_kp_id is not None:
                    LOG.info('openstack keypair create success: ' + str(new_kp_id) + " : " + str(cmd))
                    break;

                LOG.warning("openstack keypair create command failed" + str(user_name) + ": " + str(cmd) +
                            ", status: " + str(status) +
                            ", rtncode: " + str(rtncode) +
                            ", data_stdout: " + str(data_stdout) +
                            ", data_stderr: " + str(data_stderr) +
                            ", retrying (" + str(i) + ")")

            except Exception as e:
                pass

            time.sleep(timeout)

        # if we retried too many time declare failure
        if new_kp_id is None:  # and i >= retries:
            raise Openstack_Command_Fail, 'openstack keypair create failed after ' + str(retries) + ' retries, giving up'
        return new_kp_id

    @classmethod
    def get_key_file(self, user_name):
        key_file="/var/tmp/" + user_name + ".key"
        return key_file 

    @classmethod
    def get_keystone_file_name(self, project_name, user_name):
        keystone_cred_file = "/var/tmp/cred." + project_name + "." + user_name
        return keystone_cred_file

    @classmethod
    def setup_project(self, project_name, user_name, user_email, user_pwd, role, admin_user):

        try:
            project_id = self.create_project(project_name)
            user_id = self.create_user(project_name, user_name, user_email, user_pwd, role, admin_user)
        except Exception as e:
            self._cleanup(project_name, user_name)
            raise e

    @classmethod
    def start(self, project_name, user_name, user_email, user_pwd, role, admin_user, ssh_key, ec2_auth_url):

        try:
            keystone_cred_file = self.generate_user_keystone_file(project_name, user_name, user_pwd, ec2_auth_url) 

            project_id = self.create_project(project_name)

            user_id = self.create_user(project_name, user_name, user_email, user_pwd, role, admin_user)

            if (os.path.isfile(keystone_cred_file)):
                LOG.debug("Sourced " + keystone_cred_file)
                Commands.source(keystone_cred_file)
            else:
                raise Openstack_Command_Fail, 'failed to load cred file ' + keystone_cred_file

            self.create_security_group_rules()

            kp_file = self.generate_key_file(user_name, ssh_key)

            kp_id = self.create_key_pair(user_name, kp_file)

            return keystone_cred_file  

        except Exception as e:
            self._cleanup(project_name, user_name)
            raise e

    @classmethod
    def _cleanup(self, project_name, user_name):
        try:
            group_list=self.get_default_security_group(project_name)
            if group_list is not None and len(group_list) > 0:
                self.delete_default_security_group_rules(str(group_list[0]['ID']))
        except:
            pass
        try:
            self.delete_default_security_group(project_name)
        except:
            pass
        try:
            self._cleanup_user_by_name(user_name)
        except:
            pass
        try:
            self._cleanup_project_by_name(project_name)
        except:
            pass

    @classmethod
    def _cleanup_poll(self, project_name, user_name, timeout):

        try:
            begin = time.time()
            while True:
                time_passed = time.time() - begin
                try:
                    self._cleanup(project_name, user_name)
                except:
                    pass

                pj_id = self.get_project(project_name)
                if pj_id is None: 
                    LOG.debug("Openstack cleanup complete")
                    return

                if time_passed > timeout:
                    raise Openstack_Command_Fail, "openstack project delete commmand failed for " + str(id)

                time.sleep(10)
        except:
            LOG.error("Cleanup project failed: " + project_name) 

    @classmethod
    def _has_compute_resources(self, project_name):
        # check if project has active compute resources
        cmd = ["openstack", "server", "list", "--project", str(project_name), "-f", "json", "-c", "ID"]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        if rtncode != 0:
           LOG.warning("openstack project show command with non-zero rtncode " + str(project_name) + ": " + str(cmd) +
                       ", rtncode: " + str(rtncode) +
                       ", data_stdout: " + str(data_stdout) +
                       ", data_stderr: " + str(data_stderr)) 
           return 0 
        data_stdout.strip()
        if not data_stdout :
            return 0 
        val=len(json.loads(data_stdout))
        LOG.debug("Project " + project_name + " has compute resources " + str(val))
        return val

    @classmethod
    def _has_network_resources(self, project_name):
        # check if project has active network resources
        cmd = ["openstack", "network", "list", "--project", str(project_name), "-f", "json", "-c", "ID"]
        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout

        if rtncode != 0:
           LOG.warning("openstack project show command with non-zero rtncode " + str(project_name) + ": " + str(cmd) +
                       ", rtncode: " + str(rtncode) +
                       ", data_stdout: " + str(data_stdout) +
                       ", data_stderr: " + str(data_stderr)) 
           return 0 
        data_stdout.strip()
        if not data_stdout :
            return 0 
        val=len(json.loads(data_stdout))
        LOG.debug("Project " + project_name + " has network resources " + str(val))
        return val

    @classmethod
    def _has_resources(self, project_name):
        if self._has_compute_resources(project_name) != 0 :
            return True
        if self._has_network_resources(project_name) != 0 :
            return True
        return False
    
    @classmethod
    def _has_resources_poll(self, project_name, timeout):

        begin = time.time()
        while True:
            time_passed = time.time() - begin
            res = self._has_resources(project_name)

            if res == False:
                LOG.warning("openstack project " + str(project_name) + " has no resources")
                return False 

            if time_passed > timeout:
                break
            time.sleep(10)
        return True

    @classmethod
    def cleanup(self, project_name, user_name):
        if (os.path.isfile(os.environ['EUCA_KEY_DIR'] + "/novarc")):
            Commands.source(os.environ['EUCA_KEY_DIR'] + "/novarc")
        elif (os.path.isfile(os.environ['EUCA_KEY_DIR'] + "/openrc")):
            Commands.source(os.environ['EUCA_KEY_DIR'] + "/openrc")
        else:
            pass
        if self._has_resources_poll(project_name, 60) :
            LOG.debug("openstack project cleanup could not be started as project has resources")
            return
        LOG.debug("openstack project cleanup begin")
        self._cleanup_poll(project_name, user_name, 30*60) 
        try:
            os.remove(self.get_keystone_file_name(project_name, user_name))
            os.remove(self.get_key_file(user_name))
        except:
            pass

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
            raise Openstack_Command_Fail, "_get_console_log_by_ID Invalid id " + str(id)

        data = None
        vm_console_log = ''
        for i in range(retry):
            try:
                cmd = ["openstack", "console", "log", "show", str(id)]
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
            raise Openstack_Command_Fail, "Failed cmd  " + str(retry) + " times, giving up: " + str(cmd)

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
            raise Openstack_Command_Fail, "_get_info_by_ID Invalid id " + str(id)

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
            raise Openstack_Command_Fail, "Failed cmd  " + str(retry) + " times, giving up: " + str(cmd)

        return vm_info

    @classmethod
    def _get_info_by_ID(self, id, field):
        if id == None:
            raise Openstack_Command_Fail, "Invalid id " + str(id)

        try:
            vm_info = self._get_all_info_by_ID(id)
            return vm_info[field]
        except VM_Does_Not_Exist as e:
            raise e
        except Exception as e:
            raise Openstack_Command_Fail, "No value for " + str(field) + " in info for " + str(id)

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
        cmd = ["openstack", "server", "show", str(id), "-f", "value", "-c", "addresses"]
        rtncode, data_stdout, data_stderr = Commands.run(cmd,
                                                         timeout=60)  # TODO: needs real timeout
        LOG.debug("rtncode=" + str(rtncode))
        LOG.debug("data_stdout=" + str(data_stdout))
        LOG.debug("data_stderr=" + str(data_stderr))

        if rtncode != 0:
            raise Openstack_Command_Fail, str(cmd) + ": Failed to list floating ips"

        addresses = data_stdout.strip().split(";")
        floating_ip = None
        for add in addresses:
            add.strip()
            if add.find(",") != -1:
               ips=add.split(",")
               floating_ip = ips[1].strip()

        return floating_ip 

    @classmethod
    def _allocate_floating_ip(self, network, project_name, new_vm_id, retries=10, timeout=20):
        for i in range(retries):
            try:

                cmd = ["openstack", "floating", "ip", "create", "--description", str(new_vm_id), "-f", "value", "-c", "name", str(network)]
                rtncode, data_stdout, data_stderr = Commands.run(cmd,
                                                                 timeout=60)  # TODO: needs real timeout

                if rtncode == 0:
                    return data_stdout.strip() 

            except Exception as e:
                pass

            LOG.warning("Failed to allocate floating ip " + str(i) + " times, retrying")

            if i >= retries:
                raise Openstack_Command_Fail, "Failed to allocate floating ip  " + str(i) + " times, giving up: " + str(cmd)

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
                        raise Openstack_Command_Fail, "Failed check for associated floating ip (" + str(
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
                raise Openstack_Command_Fail, str(cmd) + ": Failed to associate floating ip (" + str(
                    ipaddr) + ") to vm (" + str(vm_id) + ") " + str(i) + " times, giving up: " + str(cmd)

            time.sleep(timeout)


    @classmethod
    def _cleanup_vm_by_name(self, name):
        try:
            cmd = ["openstack", "server", "delete", str(name)]
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
            raise Openstack_Command_Fail, "openstack server delete commmand failed for " + str(name) + ": " + str(cmd)

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
                    raise Openstack_Command_Fail, "openstack server delete commmand failed for " + str(id)

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

        raise Openstack_Command_Fail, "VM._cleanup_floating_addr " + floating_addr + "failed. giving up."

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
    def _start_vm(self, instance_type, img, ssh_key, user_data_file, name, mgmt_net_id, sec_group):
        # Boot the vm
        retries = 3
        timeout = 10
        status = None
        new_vm_id = None
        for i in range(retries):
            try:
                cmd = ["openstack", "server", "create",
                       "--flavor", str(instance_type),
                       "--security-group", str(sec_group),
                       "--image", str(img),
                       "--key-name", str(ssh_key),
                       "--nic", str(mgmt_net_id),
                       "--user-data", str(user_data_file),
                       "--wait",
                       str(name), "-fjson"]
                rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
                if rtncode != 0:
                    LOG.warning("openstack server create command with non-zero rtncode " + str(name) + ": " + str(cmd) +
                                ", rtncode: " + str(rtncode) +
                                ", data_stdout: " + str(data_stdout) +
                                ", data_stderr: " + str(data_stderr) +
                                ", retrying (" + str(i) + ")")
                    cmd = ["openstack", "server", "show", "-c", "id", "-f", "value", str(name)]
                    rtncode, data_stdout, data_stderr = Commands.run(cmd, timeout=60 * 30)
                    if rtncode == 0:
                        new_vm_id = data_stdout.strip()
                    raise Openstack_Command_Fail, str(cmd)

                new_vm_id = json.loads(data_stdout)['id']

                status = self.get_status_by_ID(new_vm_id).lower()
                if status == 'active':
                    LOG.info('openstack server create success: ' + str(new_vm_id) + " : " + str(cmd))
                    break;

                LOG.warning("openstack server create command failed with status != active " + str(name) + ": " + str(cmd) +
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
            raise Openstack_Command_Fail, 'openstack server create failed after ' + str(i) + ' retries, giving up'

        return new_vm_id

    @classmethod
    def start(self, instance_type, ami, qcow2, aki, ari, startup_retries, ping_retries, ssh_retries, user_data_file,
              name, public_network, project_name, user_name, user_email, user_pwd, role, admin_user, mgmt_network, ssh_key, ec2_auth_url):

        LOG.debug("start " + str(name))

        keystone_cred_file = Project.start(project_name, user_name, user_email, user_pwd, role, admin_user, ssh_key, ec2_auth_url)
        if (os.path.isfile(keystone_cred_file)):
            LOG.debug("Sourced " + keystone_cred_file)
            Commands.source(keystone_cred_file)
        else:
            raise Openstack_Command_Fail, 'failed to load cred file ' + keystone_cred_file

        mgmt_net_id = "net-id=" + Project.get_management_network(mgmt_network)

        key_file = Project.get_key_file(user_name)

        sec_group="default"
        img = ami 
        if qcow2 is not None:
           img = qcow2
        for i in range(startup_retries):
            try:
                new_vm_id = None
                status = None
                new_vm_id = self._start_vm(instance_type, img, user_name, user_data_file, name, mgmt_net_id, sec_group)
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
            floating_addr = self._allocate_floating_ip(public_network, project_name, new_vm_id)
        except Exception as e:
            LOG.info('Openstack failed to allocate floating ip to the VM ' + str(new_vm_id))

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
            raise VM_Broken('Openstack failed to allocate floating ip to the VM ' + str(new_vm_id), new_vm_id, console_log)

        # ping test
        if self._ping_test_vm(floating_addr, ping_retries, 10):
            LOG.info("VM " + new_vm_id + " is pingable on ip " + floating_addr)
        else:
            try:
                console_log = str(VM.get_console_log_by_ID(new_vm_id))
            except Exception:
                console_log = 'Cannot get console log'
            self._clean_all(new_vm_id, floating_addr)
            raise VM_Broken_Unpingable('Instance is not pingable: ' + str(new_vm_id), new_vm_id, console_log)

        # liveness check is bypassed as VMs are created with user provided public keys.
        # SSH can no longer be used as user private key is not available

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
            raise Openstack_Command_Fail, str(exception_msg)

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
