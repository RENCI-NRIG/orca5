#!/bin/bash

# This script expects the following environment variables
# FVVLAN - mandatory
# NOXHOST - mandatory (FQDN or IP of the host on which this runs)
# NOX_FIRST_PORT - optional (default 50000)
# NOX_LAST_PORT - optional (default 55000)
# NOXEXEC - optional (default ./nox_core)

# for testing you can use ./start_nox <vlan tag> <hostname> <port>

# optionally take from command line
if [ ! -z $1 ]; then
	FVVLAN=$1
fi

if [ ! -z $2 ]; then
	NOXHOST=$2
fi

if [ ! -z $3 ]; then
	NOXPORT=$3
fi

if [ -z "$NOX_FIRST_PORT" ]; then
	NOX_FIRST_PORT=50000
fi

if [ -z "$NOX_LAST_PORT" ]; then
	NOX_LAST_PORT=55000
fi

# Need to know the vlan
if [ -z "$FVVLAN" ]; then
	echo "Must specify vlan"
	exit 1
fi

# Need to know the host
if [ -z "$NOXHOST" ]; then
	echo "Must specify the hostname"
	exit 1
fi

# need to know where nox is
if [ -z "$NOXEXEC" ]; then
	NOXEXEC="./nox_core"
fi

if [ ! -e "$NOXEXEC" ]; then
	echo "$NOXEXEC is not a valid executable or cannot be found"
	exit 1
fi

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

# get an unsorted list of ports currently in use by nox-es
function getPorts () 
{
	PORTS=`ps axw | grep "nox.*nox-vlan" | gawk '{ print $9}' | gawk -F ':' '{print $2}'`
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
	PORTS=`netstat --tcp -ntl | grep tcp | awk '{print $4}' | awk -F ":" '{ print $2}'`
	echo $PORTS
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
        FREEPORT=`echo "" | gawk -v used="$USEDPORTS" -v first="$NOX_FIRST_PORT" -v last="$NOX_LAST_PORT" 'BEGIN { RS=" "; }
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

LOOP="1"
lock_file /tmp/nox_lock

while [ "$LOOP" = "1" ]; do
	# allocate a new port as needed
	if [ -z $NOXPORT ]; then
		NOXPORT=$(findFreeNoxPort) || {
			echo "No ports available"
			exit 1
		}
	fi
	# check the port is not taken already
	LOOP="0"
	LPORTS=$(checkListeningPorts $NOXPORT) || {
		NOXPORT=$(( $NOXPORT + 1 ))
		LOOP="1"
	}
done


# start NOX
NOXDIR=`dirname $NOXEXEC`
NOXBASE=`basename $NOXEXEC`
cd $NOXDIR
nohup ./$NOXBASE -n /tmp/nox-vlan-$FVVLAN- -i ptcp:$NOXPORT pyswitch > /dev/null 2>&1 &

if [ "$?" != "0" ]; then
	echo "Unable to start NOX"
	exit 1
fi

# let nox get started
count=60
while [ "$count" != "0" ]; do
        checkListeningPorts $NOXPORT > /dev/null || break
        sleep 1

        # no more than count times to ensure progress
        count=$(($count - 1))
done


echo "tcp:$NOXHOST:$NOXPORT"

unlock_file /tmp/nox_lock
