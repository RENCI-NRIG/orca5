#!/bin/bash

source ${EUCA_KEY_DIR}/eucarc
KEY=${EUCA_KEY_DIR}/${EC2_SSH_KEY}
SSH_OPTS="-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no"

machine=$1
datamask=$2
manageip=$3
managemask=$4
managegw=$5

# need to make sure host is reachable or wait
check_ping() {
	#echo "Pinging host $1"
	ping -c 1 ${1} > /dev/null 2>&1
	if [ "$?" -ne "0" ]; then
		#echo "Host is not reachable"
		return 1
	else
		#echo "Host is reachable"
		return 0
	fi
}

# allow ctrl-c and such
trap `exit ` 2 1 15 

echo "Testing reachability of host $machine"
check_ping $machine

while [ "$?" -ne 0 ]; do
	sleep 1
	echo "Testing reachability of host $machine"
	check_ping $machine
done

ssh $SSH_OPTS -i $KEY root@${machine} "killall dhclient3; echo nameserver 192.168.201.254 > /etc/resolv.conf; ifconfig eth0 netmask ${datamask}; ifconfig eth1 up ${manageip} netmask ${managemask};route del default; route add default gateway ${managegw}"


