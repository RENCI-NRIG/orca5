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


class SLURM_MANAGE_Exception(Exception):
    pass


class SLURM_MANAGE:

    @classmethod
    def create_allocation(self, 
                   owner, 
                   type, 
                   reservation):  
        
        LOG.info('Creating SLURM ALLOCATION:')
        LOG.debug('owner: ' + str(owner))
        LOG.debug('type: ' + str(type))
        LOG.debug('reservation: ' + str(reservation))

	overprovision = False

	# This is the dictionary of available SLURM allocation types; Can be extended
	slurm_types = {'slurm.tiny': 1, 'slurm.small': 4, 'slurm.medium': 16, 'slurm.large': 64};
	# This is the hashmap of number of extra nodes to be overprovisioned for each SLURM allocation type: overprovision sq_rt(N) for N nodes
	overprovision_factor = {'slurm.tiny': 1, 'slurm.small': 2, 'slurm.medium': 4, 'slurm.large': 8};

	# parse type to determine count
	# optionally use owner

	if slurm_types.has_key(str(type)):
		if overprovision :
                        numNodes = slurm_types.get(str(type)) + overprovision_factor.get(str(type))
                        LOG.debug("numNodes = " + str(numNodes))
                else :
                        numNodes = slurm_types.get(str(type))
                        LOG.debug("numNodes = " + str(numNodes))
	else :
    	    	data_stdout = 'FALSE'
    	    	LOG.debug("Unknown SLURM allocation type... exiting...")
    	    	return data_stdout

	# TODO: Add reservation in the command line argument
	# Run as sudo ?
	cmd = ["/usr/bin/salloc", '-N', str(numNodes), '--no-shell']
	#cmd = ["/usr/bin/salloc", '--reservation=scdemo', '-N', str(numNodes), '--no-shell']

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                            

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        return data_stdout

    @classmethod
    def delete_allocation(self, 
                   allocation_id):  
        
        LOG.info('Deleting SLURM ALLOCATION:')
        LOG.debug('allocation_id: ' + str(allocation_id))

	cmd = ["/usr/bin/scancel", str(allocation_id)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                            

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        return data_stdout

    @classmethod
    def modify_allocation(self, 
                   allocation_id,  
                   count):  
        
        LOG.info('Modifying SLURM ALLOCATION:')
        LOG.debug('allocation_id: ' + str(allocation_id))
        LOG.debug('count: ' + str(count))

	cmd = ["/usr/bin/scontrol", "update", "JobId=" + str(allocation_id), "NumNodes=" + str(count)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                            

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        return data_stdout

    @classmethod
    def modifynodelist_allocation(self, 
                   allocation_id,  
                   nodelist):  
        
        LOG.info('Modifying SLURM ALLOCATION:')
        LOG.debug('allocation_id: ' + str(allocation_id))
        LOG.debug('nodelist: ' + str(nodelist))

	cmd = ["/usr/bin/scontrol", "update", "JobId=" + str(allocation_id), "NodeList=" + str(nodelist)]

        LOG.debug('command: ' + str(cmd))

        rtncode,data_stdout,data_stderr = Commands.run(cmd, timeout=60) #TODO: needs real timeout                            

        LOG.debug("rtncode: " + str(rtncode))
        LOG.debug("data_stdout: " + str(data_stdout))
        LOG.debug("data_stderr: " + str(data_stderr))

        return data_stdout

    @classmethod
    def get_jobnodeinfo(self, 
                   allocation_id):  
        
        LOG.info('Getting node list for a  SLURM ALLOCATION:')
        LOG.debug('allocation_id: ' + str(allocation_id))

	#print Popen("echo Hello World", stdout=PIPE, shell=True).stdout.read()
	#p = Popen("echo Hello World", stdout=PIPE, shell=True)
	p = Popen("/usr/bin/scontrol show job " + str(allocation_id) + " | grep \" NodeList\" | awk -F \"=\" \'{print $2}\'", stdout=PIPE, shell=True)
	# There should be only one line in the output of above command
	for line in p.stdout.readlines():
		output = line.rstrip()

	p = Popen("/usr/bin/scontrol show hostname " + str(output) + " | paste -d, -s", stdout=PIPE, shell=True)
	# There should be only one line in the output of above command
	for line in p.stdout.readlines():
		return line.rstrip()


    @classmethod
    def get_nodestatus(self, 
                   nodename):  
        
        LOG.info('Getting status of a node')
        LOG.debug('nodename: ' + str(nodename))

	p = Popen("/usr/bin/scontrol show Node " + str(nodename) + " | grep State | awk \'{print $1}\' | awk -F \"=\" \'{print $2}\'", stdout=PIPE, shell=True)
	# There should be only one line in the output of above command
	for line in p.stdout.readlines():
		return line.rstrip()
