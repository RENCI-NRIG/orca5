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

# We need to loop; ec2-describe-instances occasionally fails in creative ways...
REASON=""
FAILCOUNT=0
IP="0.0.0.0"
while [ "$IP" = "0.0.0.0" ]
do
	IP=`ec2-describe-instances $CONNECTION_TIMEOUT $REQUEST_TIMEOUT $1 2> /dev/null | grep INSTANCE | awk '{print $4}'`
	validateIP $IP
	if [ "$?" != "0" ]; then
	# force IP to 0.0.0.0 for now, assume a transient problem, but keep count
		IP="0.0.0.0"
		FAILCOUNT=$((FAILCOUNT+1))
		if [ "$FAILCOUNT" -eq 10 ]; then
			REASON="get-ip.sh: persistent problem with ec2-describe-instances while trying to fetch IP, exiting loop"
			IP=""
			break
		fi
	elif [ $IP != "0.0.0.0" ]; then
		break
	fi
	sleep 10
done

if [ -z ${IP} ]; then
	echo $REASON
	exit 1
fi

echo $IP


