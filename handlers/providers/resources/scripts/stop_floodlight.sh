#!/bin/bash 

if [ ! -z $1  ]; then
	FVVLAN=$1
fi

if [ -z "$FVVLAN" ]; then
	echo "Must specify VLAN"
	exit 1
fi

if [[ -z "$USEPS" || ! -n "$USEPS" ]]; then
	NOXPID=`pgrep -f "net.orca.vlan=vlan-$FVVLAN-"`
else
	NOXPID=`ps axw | grep nox | grep net.orca.vlan=vlan-$FVVLAN- | awk '{ print $1}'`
fi
	
if [ ! -z "$NOXPID" ]; then
	kill -TERM $NOXPID
fi
