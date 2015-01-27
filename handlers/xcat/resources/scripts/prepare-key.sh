#!/bin/bash

KEY=${XCAT_CONF_DIR}/${XCAT_SSH_KEY}
SSH_OPTS="-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
machine=$1
user_login=$2
user_key=$3

ssh $SSH_OPTS -i $KEY root@${machine} "echo ${user_key} >> .ssh/authorized_keys"

if [ "${user_login}" != "root" ]; then
	# create account and add to sudoers
	ssh $SSH_OPTS -i $KEY root@${machine} "useradd -m $user_login"
	ssh $SSH_OPTS -i $KEY root@${machine} "echo '${user_login}  ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers"
	USERHOMEPREFIX=`ssh $SSH_OPTS -i $KEY root@${machine} "useradd -D | grep HOME | awk '{ split(\\$0, a, \"=\"); print a[2]}'"`
	# take a guess
	if [ -z "${USERHOMEPREFIX}" ]; then
		USERHOMEPREFIX="/home"
	fi
	ssh $SSH_OPTS -i $KEY root@${machine} "mkdir -p ${USERHOMEPREFIX}/${user_login}/.ssh/; echo ${user_key} >> ${USERHOMEPREFIX}/${user_login}/.ssh/authorized_keys; chown -R ${user_login} ${USERHOMEPREFIX}/${user_login}/.ssh/"
else
	# root is simple
	ssh $SSH_OPTS -i $KEY root@${machine} "echo ${user_key} >> .ssh/authorized_keys"
fi
