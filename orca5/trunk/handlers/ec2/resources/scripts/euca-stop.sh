#!/bin/bash

source ${EUCA_KEY_DIR}/eucarc

export PATH=${EC2_HOME}/bin:${PATH}

if [ ! -z $EC2_CONNECTION_TIMEOUT ]; then
        CONNECTION_TIMEOUT="--connection-timeout $EC2_CONNECTION_TIMEOUT"
fi

if [ ! -z $EC2_REQUEST_TIMEOUT ]; then
        REQUEST_TIMEOUT="--request-timeout $EC2_REQUEST_TIMEOUT"
fi

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

INSTANCE_ID=${1}

if [ "$INSTANCE_ID" = "" ]; then
	echo "stop.sh: no instance id was passed, exiting"
	exit 1
fi

# Fetch public and private IP address fields from ec2-describe-instances;
# disassociate and release public IP address, if one is found to be in use.
# We'll need to loop; ec2-describe-instances occasionally fails in creative ways...
REASON=""
FAILCOUNT=0
PUBLIC_IP="0.0.0.0"
PRIVATE_IP="0.0.0.0"
while [ "$PUBLIC_IP" = "0.0.0.0" ]
do
	INSTANCE_VALUES=`ec2-describe-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT 2>/dev/null | grep INSTANCE | grep $INSTANCE_ID`
	PUBLIC_IP=`echo $INSTANCE_VALUES | awk '{print $4}'`
	PRIVATE_IP=`echo $INSTANCE_VALUES | awk '{print $5}'`
	validateIP $PUBLIC_IP
	if [ "$?" != "0" ]; then
	# force PUBLIC_IP to 0.0.0.0 for now, assume a transient problem, but keep count
		PUBLIC_IP="0.0.0.0"
		PRIVATE_IP="0.0.0.0"
		FAILCOUNT=$((FAILCOUNT+1))
		if [ "$FAILCOUNT" -eq 10 ]; then
			REASON="Persistent problem with ec2-describe-instances while trying to fetch public and private IPs - manual cleanup of public IP may be required."
			break
		fi
	elif [ $PUBLIC_IP != "0.0.0.0" ]; then
		break
	fi
	sleep 10
done

if [ "$PUBLIC_IP" != "$PRIVATE_IP" ]; then
	ec2-disassociate-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP >/dev/null 2>&1
	ec2-release-address $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $PUBLIC_IP >/dev/null 2>&1
fi

ec2-terminate-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $INSTANCE_ID >/dev/null 2>&1
if [ "$?" != "0" ]; then
	REASON="Error encountered running ec2-terminate-instances $INSTANCE_ID. Manual cleanup may be required. "$REASON
fi

if [ ! -z "$REASON" ]; then
	REASON="stop.sh: "$REASON
	echo $REASON
	exit 1
fi
exit 0
