#!/bin/bash

source ${EUCA_KEY_DIR}/novarc
KEY=${EUCA_KEY_DIR}/${EC2_SSH_KEY}
SSH_OPTS="-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no"

machine=$1
user_key=$2

ssh $SSH_OPTS -i $KEY root@${machine} "echo ${user_key} >> .ssh/authorized_keys"

