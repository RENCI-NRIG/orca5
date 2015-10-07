#!/bin/bash

source $(dirname $0)/common.sh

function testSSH() {
	local user=${1}
	local host=${2}
	local key=${3}
	local timeout=${4}
	
	SSH_OPTS="-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
	
	ssh -q -q $SSH_OPTS -o "BatchMode=yes" -o "ConnectTimeout ${timeout}" -i $key ${user}@${host} "echo >/dev/null 2>&1" && return 0 || return 1
}

# Let's begin with some sanity checks.
if [ -z $XCAT_IMAGE_NAME ]; then
    echo "start.sh: Image name not provided. Ensure that you have correctly set the xcat.image.name property in xcat.site.properties!"
    exit 1
fi

if [ -z $XCAT_SSH_KEY ]; then
    echo "start.sh: xCAT SSH key not provided. Ensure that you have correctly set the xcat.ssh.key property in xcat.site.properties!"
    exit 1
fi

if [ -z $XCAT_PROVISION_MAXWAIT ]; then
    echo "start.sh: xCAT maximum provisioning wait time not specified. Ensure that you have correctly set the xcat.provision.maxwait property in xcat.site.properties!"
    exit 1
fi

NODENAME=${1}

# try rebooting a few times
for ((rebootcount=0;rebootcount<${XCAT_REBOOT_RETRIES=3};rebootcount+=1));
do

	if [ -z ${NODENAME} ]; then
	    echo "start.sh: No available bare metal nodes found. Exiting."
	    exit 1
	else
	    rsetboot $NODENAME net >/dev/null 2>&1
	    if [ "$?" != "0" ]; then
	        echo "start.sh: Attempt to request network boot of bare metal node $NODENAME failed. Please ensure that your xCAT installation has been properly configured."
	        exit 1
	    fi
	
	    nodeset $NODENAME osimage="$XCAT_IMAGE_NAME" >/dev/null 2>&1
	    if [ "$?" != "0" ]; then
	        echo "start.sh: Attempt to nodeset bare metal node $NODENAME with osimage $XCAT_IMAGE_NAME failed. Please ensure that the image name is correct, and has been made available in xCAT."
	        exit 1
	    fi
	
	    rpower $NODENAME boot >/dev/null 2>&1
	    if [ "$?" != "0" ]; then
	        echo "start.sh: Attempt to boot bare metal node $NODENAME failed. Please ensure IPMI connectivity to the node's BMC device."
	        exit 1
	    fi
	fi
	
	# Poll, waiting for transition to "booted" status, up to XCAT_PROVISION_MAXWAIT seconds.
	STATUS=$(nodels $NODENAME nodelist.status | awk -F': ' '{print $2}') 
	
	SLEEP_INTERVAL=10
	count=$(($XCAT_PROVISION_MAXWAIT / $SLEEP_INTERVAL))
	while [ "$STATUS" != "booted" ] && [ "$count" != "0" ]
	do      
		sleep 10
	        STATUS=$(nodels $NODENAME nodelist.status | awk -F': ' '{print $2}') 
		count=$(($count - 1))
	done
	
	# FIXME: We should probably debug log that we exited the above loop,
	# without the node transitioning into 'booted' state.
	# For now, we proceed to the ping and ssh checks silently, just in case
	# xcat succeeded in imaging the node, but did not update its state table (for whatever reason).
	
	# ping node - check to see if alive
	if [ -z ${XCAT_PING_RETRIES} ] || [ "${XCAT_PING_RETRIES}" = "0" ]
	then
		UNPINGABLE=false
	else
		# wait until the instance is pingable
		UNPINGABLE=true
		for ((i=0;i<${XCAT_PING_RETRIES=6};i+=1));
		do
		    PING=`ping -c 1 $NODENAME > /dev/null 2>&1`
		    if [ "$?" = "0" ]; then
			UNPINGABLE=false
			break
		    fi
		    sleep 10
		done
	fi
	
	# From here on, if ping or ssh fails, we clean up the node and mark it unused.
	# The node may be fine, but not with this image; no point in disabling it globally.
	
	if ${UNPINGABLE} ; then
		#echo "start.sh: Unable to ping bare metal node $NODENAME. Cleaning up and exiting." 
		#cleanupNode $NODENAME
		#exit 1
		continue
	fi
	
	# check ssh to node
	if [ -z ${XCAT_SSH_RETRIES} ] || [ "${XCAT_SSH_RETRIES}" = "0" ]
	then
		UNSSHABLE=false
	else
		UNSSHABLE=true
	        KEY=${XCAT_CONF_DIR}/${XCAT_SSH_KEY}
		for ((i=0;i<${XCAT_SSH_RETRIES=6};i+=1));
		do
			testSSH root $NODENAME $KEY 5
			if [ "$?" = "0" ]; then
				UNSSHABLE=false
				break
			fi
			sleep 10
		done
	fi
	
	if ${UNSSHABLE} ; then
		#echo "start.sh: Unable to ssh into bare metal node $NODENAME. Cleaning up and exiting."
		#cleanupNode $NODENAME
		#exit 1
		continue
	fi
	
	# Node pingable and SSH-able - exit successfully.
	exit 0
done

echo "start.sh: Unable to boot bare-metal node $NODENAME. Cleaning up and exiting."
cleanupNode $NODENAME
exit 1
