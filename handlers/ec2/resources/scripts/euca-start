#!/bin/bash 

source ${EUCA_KEY_DIR}/eucarc

export PATH=${EC2_HOME}/bin:${PATH}

if [ ! -z $EC2_CONNECTION_TIMEOUT ]; then
	CONNECTION_TIMEOUT="--connection-timeout $EC2_CONNECTION_TIMEOUT"
fi

if [ ! -z $EC2_REQUEST_TIMEOUT ]; then
	REQUEST_TIMEOUT="--request-timeout $EC2_REQUEST_TIMEOUT"
fi

if [ ! -z $EUCA_GROUP ]; then
	GROUP="-g $EUCA_GROUP"      
fi
    
if [ ! -z $NEUCA_INI ]; then
	USER_DATA="--user-data-file $NEUCA_INI"
fi

if [ ! -z $AKI_NAME ]; then
	KERNEL="--kernel $AKI_NAME"
fi

if [ ! -z $ARI_NAME ]; then
	INITRD="--ramdisk $ARI_NAME"
fi

if [ ! -z $EC2_INSTANCE_TYPE ]; then
	INSTANCE_TYPE="--instance-type $EC2_INSTANCE_TYPE"
fi

function testSSH() {
	local user=${1}
	local host=${2}
	local key=${3}
	local timeout=${4}
	
	SSH_OPTS="-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no"
	
	ssh -q -q $SSH_OPTS -o "BatchMode=yes" -o "ConnectTimeout ${timeout}" -i $key ${user}@${host} "echo 2>&1" && return 0 || return 1
}

function validateIP() {
# Code cribbed from comp.unix.shell post
# http://groups.google.com/group/comp.unix.shell/msg/96bb870de95c9aed?dmode=source
	case "$*" in
		""|*[!0-9.]*|*[!0-9]) return 1 ;;
	esac

	local IFS=.
	set -- $*
	[ $# -eq 4 ] &&
	[ ${1:-666} -le 255 ] && [ ${2:-666} -le 255 ] &&
	[ ${3:-666} -le 255 ] && [ ${4:-666} -le 254 ]
}

# authorize ssh access from anywhere to the group containing the vm
if [ -z $EUCA_GROUP ]; then
	# the vm is in the default group
	ec2-authorize $CONNECTION_TIMEOUT $REQUEST_TIMEOUT -P tcp -p 22 -s 0.0.0.0/0 default > /dev/null 2>&1
	ec2-authorize $CONNECTION_TIMEOUT $REQUEST_TIMEOUT -P tcp -p 1024-65535 -s 0.0.0.0/0 default > /dev/null 2>&1
	ec2-authorize $CONNECTION_TIMEOUT $REQUEST_TIMEOUT -P udp -p 1024-65535 -s 0.0.0.0/0 default > /dev/null 2>&1
	ec2-authorize $CONNECTION_TIMEOUT $REQUEST_TIMEOUT -P icmp -t -1:-1 -s 0.0.0.0/0 default > /dev/null 2>&1
else
	# the vm is in a custom group
	ec2-authorize $CONNECTION_TIMEOUT $REQUEST_TIMEOUT -P tcp -p 22 -s 0.0.0.0/0 $EUCA_GROUP > /dev/null 2>&1
	ec2-authorize $CONNECTION_TIMEOUT $REQUEST_TIMEOUT -P tcp -p 1024-65535 -s 0.0.0.0/0 $EUCA_GROUP > /dev/null 2>&1
	ec2-authorize $CONNECTION_TIMEOUT $REQUEST_TIMEOUT -P udp -p 1024-65535 -s 0.0.0.0/0 $EUCA_GROUP > /dev/null 2>&1
	ec2-authorize $CONNECTION_TIMEOUT $REQUEST_TIMEOUT -P icmp -t -1:-1 -s 0.0.0.0/0 $EUCA_GROUP > /dev/null 2>&1
fi

REASON=""
INSTANCE_RETRIES=${EC2_STARTUP_RETRIES=5}

if ${STARTSH_DEBUG=false} ; then
	echo Retrying $INSTANCE_RETRIES times
fi
INSTANCE_ID=""
while [ -z ${INSTANCE_ID} ] && [ $INSTANCE_RETRIES -gt 0 ]
do
	if ${STARTSH_DEBUG=false} ; then
		echo "Retry $INSTANCE_RETRIES"
	fi

	# try to allocate an IP address if necessary (false is default if unset)
	if ${EC2_USE_PUBLIC_ADDRESSING=false} ; then
		if ${STARTSH_DEBUG=false} ; then
			echo Executing: ec2-allocate-address
		fi
		PUBLIC_IP=`ec2-allocate-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT 2> /dev/null | awk '{print $2}'`
		if [ "$PUBLIC_IP" = "" ]; then
			REASON="start.sh: Unable to allocate public IP address" 
			if ${STARTSH_DEBUG=false} ; then
				echo DEBUG IS ON: $REASON
			fi
			INSTANCE_RETRIES=$((INSTANCE_RETRIES-1))
			continue
		fi
	fi

	if ${STARTSH_DEBUG=false} ; then
		echo Executing: ec2-run-instances --addressing private -k $EC2_SSH_KEY $USER_DATA $AMI_NAME $KERNEL $INITRD $GROUP $INSTANCE_TYPE
	fi
	INSTANCE_ID=`ec2-run-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT --addressing private -k $EC2_SSH_KEY $USER_DATA $AMI_NAME $KERNEL $INITRD $GROUP $INSTANCE_TYPE 2>/dev/null | grep INSTANCE | awk '{print $2}'`

	# FIXME:
	# We have a suspicion that the Eucalyptus cloud controller may
	# not respond atomically to an invocation of ec2-run-instances.
	# If this can be verified, it should be filed as a bug against Eucalyptus;
	# it would cause us to leak instances.
	#
	# Reason:
	# If ec2-run-instances reports an exception from the cloud controller,
	# rather than output containing a valid instance ID, we cannot know if
	# the instance started in despite of the exception.
	# If the instance did start, there's no way (short of *very* broken
	# heuristics) to determine the correct instance ID to terminate.
	if [ -z ${INSTANCE_ID} ]; then
		REASON="start.sh: after executing \"ec2-run-instances --addressing private -k $EC2_SSH_KEY $USER_DATA $AMI_NAME $KERNEL $INITRD $GROUP $INSTANCE_TYPE\" unable to create a VM"
		if ${STARTSH_DEBUG=false} ; then
			echo DEBUG IS ON: $REASON
		fi
		if ${EC2_USE_PUBLIC_ADDRESSING} ; then
			ec2-release-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP > /dev/null 2>&1
		fi
		INSTANCE_RETRIES=$((INSTANCE_RETRIES-1))
		continue
	fi

	# wait for the machine to become ready. ec2-describe-instances gets stuck and times out sometimes
	FAILCOUNT=0
	STATUS="pending"
	while [ "$STATUS" = "pending" ]
	do
		if ${STARTSH_DEBUG=false} ; then
			echo Executing ec2-describe-instances $INSTANCE_ID
		fi
		STATUS=`ec2-describe-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $INSTANCE_ID 2>/dev/null`
		if [ "$?" != "0" ]; then
			# force status to pending for now, assume a transient problem, but keep count
			STATUS="pending"
			FAILCOUNT=$((FAILCOUNT+1))
			if [ "$FAILCOUNT" -eq 10 ]; then
				REASON="start.sh: persistent problem with ec2-describe-instances in STATUS check, exiting loop"
				if ${STARTSH_DEBUG=false} ; then
					echo DEBUG IS ON: $REASON
				fi
				STATUS="'ec2-describe-instances failed'"
				break
			fi
		else
			STATUS=`echo $STATUS | awk '{print $10}'`
		fi
		sleep 10
	done

	if [ "$STATUS" != "running" ]; then
		REASON="start.sh: VM instance $INSTANCE_ID transitioned into $STATUS instead of 'running'" 
		if ${STARTSH_DEBUG=false} ; then
			echo DEBUG IS ON: $REASON
		fi
		if ${EC2_USE_PUBLIC_ADDRESSING} ; then
			ec2-release-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP > /dev/null 2>&1
		fi
		ec2-terminate-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $INSTANCE_ID > /dev/null 2>&1
		INSTANCE_RETRIES=$((INSTANCE_RETRIES-1))
		INSTANCE_ID=""
		continue
	fi

	# associate IP with the VM
	if [ "$PUBLIC_IP" != "" ]; then
		if ${STARTSH_DEBUG=false} ; then
			echo Executing ec2-associate-address -i $INSTANCE_ID $PUBLIC_IP
		fi
		STATUS=`ec2-associate-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT -i $INSTANCE_ID $PUBLIC_IP 2> /dev/null`
	fi

	# get instance IP (public or private)
	# We need to loop; ec2-describe-instances occasionally fails in creative ways...
	FAILCOUNT=0
	IP="0.0.0.0"
	while [ "$IP" = "0.0.0.0" ]
	do
		IP=`ec2-describe-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $INSTANCE_ID 2> /dev/null | grep INSTANCE | awk '{print $4}'`
		validateIP $IP
		if [ "$?" != "0" ]; then
		# force IP to 0.0.0.0 for now, assume a transient problem, but keep count
			IP="0.0.0.0"
			FAILCOUNT=$((FAILCOUNT+1))
			if [ "$FAILCOUNT" -eq 10 ]; then
				REASON="start.sh: persistent problem with ec2-describe-instances in IP check, exiting loop"
				if ${STARTSH_DEBUG=false} ; then
					echo DEBUG IS ON: $REASON
				fi
				IP=""
				break
			fi
		elif [ $IP != "0.0.0.0" ]; then
			break
		fi
		sleep 10
	done

	if [ -z ${IP} ]; then
		REASON="start.sh: VM instance $INSTANCE_ID failed to report an IP address" 
		if ${STARTSH_DEBUG=false} ; then
			echo DEBUG IS ON: $REASON
		fi
		if ${EC2_USE_PUBLIC_ADDRESSING} ; then
			ec2-disassociate-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP > /dev/null 2>&1
			ec2-release-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP > /dev/null 2>&1
		fi
		ec2-terminate-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $INSTANCE_ID > /dev/null 2>&1
		INSTANCE_RETRIES=$((INSTANCE_RETRIES-1))
		INSTANCE_ID=""
		continue
	fi

	if [ -z ${EC2_PING_RETRIES} ] || [ "${EC2_PING_RETRIES}" = "0" ]
	then
		UNPINGABLE=false
	else
		# wait until the instance is pingable
		UNPINGABLE=true
		for ((i=0;i<${EC2_PING_RETRIES=60};i+=1));
		do
			PING=`ping -c 1 $IP > /dev/null 2>&1`
			if [ "$?" = "0" ]; then
				UNPINGABLE=false
				break
			fi
			sleep 10
		done
	fi

	if ${UNPINGABLE} ; then
		REASON="start.sh: Unable to ping VM instance $INSTANCE_ID on $IP" 
		if ${STARTSH_DEBUG=false} ; then
			echo DEBUG IS ON: $REASON
		fi
		if ${EC2_USE_PUBLIC_ADDRESSING} ; then
			ec2-disassociate-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP > /dev/null 2>&1
			ec2-release-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP > /dev/null 2>&1
		fi
		ec2-terminate-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $INSTANCE_ID > /dev/null 2>&1
		INSTANCE_RETRIES=$((INSTANCE_RETRIES-1))
		INSTANCE_ID=""
		continue
	fi

	if [ -z ${EC2_SSH_RETRIES} ] || [ "${EC2_SSH_RETRIES}" = "0" ]
	then
		UNSSHABLE=false
	else
		UNSSHABLE=true
		KEY=${EUCA_KEY_DIR}/${EC2_SSH_KEY}
		for ((i=0;i<${EC2_SSH_RETRIES=10};i+=1));
		do
			testSSH root $IP $KEY 5
			if [ "$?" = "0" ]; then
				UNSSHABLE=false
				break
			fi
			sleep 10
		done
	fi

	if ${UNSSHABLE} ; then
		REASON="start.sh: Unable to ssh into VM instance $INSTANCE_ID on $IP"
		if ${STARTSH_DEBUG=false} ; then
			echo DEBUG IS ON: $REASON
		fi
		if ${EC2_USE_PUBLIC_ADDRESSING} ; then
			ec2-disassociate-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP > /dev/null 2>&1
			ec2-release-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP > /dev/null 2>&1
		fi
		ec2-terminate-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $INSTANCE_ID > /dev/null 2>&1
		INSTANCE_RETRIES=$((INSTANCE_RETRIES-1))
		INSTANCE_ID=""
		continue
	fi
done

if [ -z ${INSTANCE_ID} ]; then
	echo $REASON
	exit 1
fi

echo $INSTANCE_ID

