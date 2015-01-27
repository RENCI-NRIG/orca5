#!/bin/bash

function checkListeningPorts ()
{
        CHECKPORT=$1
	if [ "$CHECKPORT" = "10" ]; then
		echo "YES"
		return 1
	else
		echo "NO"
		return 0
	fi
}

p=0

count=10
STATUS="pending"

while [ "$STATUS" = "pending" -o $STATUS = "VM_DOES_NOT_EXIST" ] && [ "$count" != "0" ] ; do
	checkListeningPorts $p > /dev/null || break
	p=$(($p + 1))
	echo "P is " $p
	sleep 1
	count=$(($count - 1))
#	[ "$count" != "5" ] || STATUS="runnig"
done
