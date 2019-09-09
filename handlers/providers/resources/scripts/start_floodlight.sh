#!/bin/bash

#set -x
# This script expects the following environment variables
# FVVLAN - mandatory
# FLHOST - mandatory (FQDN or IP of the host on which this runs)
# FL_FIRST_PORT - optional (default 50000)
# FL_LAST_PORT - optional (default 54999)
# FLEXEC - optional (default ./floodlight.sh)

# for testing you can use ./start_fl <vlan tag> <hostname> <port>

# optionally take from command line
if [ ! -z $1 ]; then
	FVVLAN=$1
fi

if [ ! -z $2 ]; then
	FLHOST=$2
fi

if [ ! -z $3 ]; then
	FLPORT=$3
fi

if [ -z "$FL_FIRST_PORT" ]; then
	FL_FIRST_PORT=50006
fi

if [ -z "$FL_LAST_PORT" ]; then
	FL_LAST_PORT=54999
fi

# Need to know the vlan
if [ -z "$FVVLAN" ]; then
	echo "Must specify vlan"
	exit 1
fi

# Need to know the host
if [ -z "$FLHOST" ]; then
	echo "Must specify the hostname"
	exit 1
fi

# need to know where FL is
if [ -z "$FLEXEC" ]; then
	FLEXEC="/opt/floodlight/floodlight.jar"
fi

#if [ ! -e "$FLEXEC" ]; then
#	echo "$FLEXEC is not a valid executable or cannot be found"
#	exit 1
#fi

function my_lockfile ()
{
        BASEFILE=`basename $1`
        TEMPFILE="/tmp/$BASEFILE.$$"
        LOCKFILE="/tmp/$BASEFILE.lock"
        echo $$ > $TEMPFILE 2>/dev/null || {
                #echo "You don't have permission to access `dirname $TEMPFILE`"
                return 1
        }
        ln $TEMPFILE $LOCKFILE 2>/dev/null && {
                rm -f $TEMPFILE
                return 0
        }
        # using 'cat' so we can catch error if file disappears
        STALE_PID=`cat $LOCKFILE 2>/dev/null` || {
                return 1
        }
        test "$STALE_PID" -gt "0" >/dev/null || {
                return 1
        }
        # if kill -0 succeeds, other process is still active
        kill -0 $STALE_PID 2>/dev/null && {
                rm -f $TEMPFILE
                return 1
        }
        # remove stale lock file
        rm $LOCKFILE 2>/dev/null 
        ln $TEMPFILE $LOCKFILE 2>/dev/null && {
                rm -f $TEMPFILE
                return 0
        }
        rm -f $TEMPFILE
        return 1
}

function lock_file ()
{
until my_lockfile "$1" ; do
        sleep 1
done
}

function unlock_file() 
{
BASEFILE=`basename $1`
rm -f "/tmp/$BASEFILE.lock"
}

# get an unsorted list of ports currently in use by floodlight-es
function getPorts () 
{
	PORTS=`ps axw | grep net.orca.vlan=vlan | gawk '{ print $20}' | gawk -F '=' '{ print $2}'`
        echo $PORTS
        return 0
}

# get a list of ports currently in use by OS
function getListeningPorts ()
{
	# BSD-style
	#PORTS=`netstat -p tcp -ntl | grep tcp | awk '{print $4}' | awk -F "[\.]" '{ print $5}'`
	# Linux-style
	#PORTS=`netstat -4 --tcp -ntl | grep tcp | awk '{print $4}' | awk -F ":" '{ print $2}'`
	#PORTS=`netstat --tcp -nt | grep tcp | awk '{print $4}' | awk -F ":" '{ print $2}'`
	LPORTS=`netstat --tcp -ntl | grep tcp | awk '{print $4}' | awk -F ":" '{ print $2}'`
	# ipV6 (for Java)
	OTHERPORTS=`netstat --tcp -ntl | grep tcp | awk '{print $4}' | awk -F ":" '{ print $4}'`
	echo "$LPORTS $OTHERPORTS"
	return 0
}

# check if a port is among listening ports
function checkListeningPorts ()
{
	CHECKPORT=$1
	LPORTS=$(getListeningPorts) || {
		return 1
	}
	echo Checking port $CHECKPORT in:
	for port in $LPORTS; do
		if [ "$port" = "$CHECKPORT" ]; then
			return 1
		fi
	done
	return 0
}
		
#identify an available port from the range
function findFreeNoxPort ()
{
        USEDPORTS=$(getPorts) || {
                return 1
        }
        FREEPORT=`echo "" | gawk -v used="$USEDPORTS" -v first="$FL_FIRST_PORT" -v last="$FL_LAST_PORT" 'BEGIN { RS=" "; }
{
        n=split(used, arr, " ")
        asort(arr)
        i=1
        port=first
        while(arr[i]==port && port<=last+1) {
                i++
                port++  
        }
        if (port==last+1)
                port=-1
        print port
}'`
        test $FREEPORT -eq -1 && {
                echo ""
                return 1
        }
        echo $FREEPORT
        return 0
}

function findRandomPort() 
{
LOOP="1"
while [ "$LOOP" = "1" ]; do
	# allocate a new port as needed
	FLPORT=$((RANDOM%($FL_LAST_PORT-$FL_FIRST_PORT) + $FL_FIRST_PORT))
	# check the port is not taken already
	LOOP="0"
	LPORTS=$(checkListeningPorts $FLPORT) || {
		FLPORT=$(( $FLPORT + 1 ))
		LOOP="1"
	}
done
echo $FLPORT
return 0
}

lock_file /tmp/fl_lock

# start FL
FPORT=$(findRandomPort)
DOCKER_IMAGE="floodlight_image"
DOCKER_CONTAINER_NAME="floodlight-${FVVLAN}"
DOCKER_DIR="/opt/floodlight"
FLJAR="/opt/floodlight/floodlight.jar"
DOCKER_OPT="dit"

IMAGE=$(docker image ls --filter "reference=${DOCKER_IMAGE}"  --format "{{.Repository}}")
if [ "${IMAGE}" != "${DOCKER_IMAGE}" ]; then
    docker image build -t ${DOCKER_IMAGE} ${DOCKER_DIR}        
fi

docker run --user $(id -u):$(id -g) -${DOCKER_OPT} -p ${FPORT}:${FPORT} -e FLPORT=${FPORT} -e FLEXEC=${FLJAR} -e FVVLAN=${FVVLAN} --name=${DOCKER_CONTAINER_NAME} ${DOCKER_IMAGE} > /dev/null 2>&1
 
# let floodlight get started
count=5
while [ "$count" != "0" ]; do
    RUNNING_CONT=$(docker ps --filter status=running --format "{{.Names}}" --filter "name=${DOCKER_CONTAINER_NAME}")
    if [ "${RUNNING_CONT}" != "${DOCKER_CONTAINER_NAME}" ]; then
        echo "ERROR: Unable to start Floodlight" 
        exit 1
    else 
        break
    fi
    ###checkListeningPorts $FLPORT > /dev/null || break
    sleep 1

    # no more than count times to ensure progress
    count=$(($count - 1))
done 


echo "tcp:$FLHOST:$FPORT"

unlock_file /tmp/fl_lock
