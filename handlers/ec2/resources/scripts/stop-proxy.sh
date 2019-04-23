#!/bin/bash

# Start port forwarding proxy for the instance (return Proxy IP address:PORT)
# for now uses Shorewall DNAT proxy code. Will also use SSH reverse
# tunnels later
# Author: Ilia Baldine (RENCI)

SSH_OPTS="-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no"

#  For Shorewall DNAT need to know (supplied as env variables)
# - IP address of proxy host  (PROXY_PROXY_IP)
# - Username and key of the user to execute the command (PROXY_USER, PROXY_SSH_KEY)
# - IP address of instance as visible from proxy (assume port 22 on instance) (PROXY_INSTANCE_IP)
# - PATH to installation of shorewall-dnat scripts (PROXY_SCRIPT_PATH)

if [ "${PROXY_TYPE}" = "SHOREWALL-DNAT" ]; then
	SSHCMD="export PATH=${PROXY_SCRIPT_PATH}:"'$PATH; execCmd.sh DEL '"${PROXY_INSTANCE_IP} 22"
	RES=`ssh ${SSH_OPTS} -i ${PROXY_SSH_KEY} ${PROXY_USER}@${PROXY_PROXY_IP} "${SSHCMD}"`
	eval $RES
	if [ "${STATUS}" = "OK" ]; then
		exit 0
	else
		echo $MSG
		exit 1
	fi
fi

echo "Unknown PROXY_TYPE ${PROXY_TYPE}"
exit 1
