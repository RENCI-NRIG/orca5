#!/bin/bash

KEY=${XCAT_CONF_DIR}/${XCAT_SSH_KEY}
SSH_OPTS="-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"

machine=$1

if [ -z $XCAT_NODE_SCRIPT ]; then
	echo "No script provided, exiting";
	exit 1;
fi

scp $SSH_OPTS -i $KEY $XCAT_NODE_SCRIPT root@${machine}:/tmp/

ONNODESCRIPT=`basename $XCAT_NODE_SCRIPT`
ssh $SSH_OPTS -i $KEY root@${machine} "/bin/bash /tmp/$ONNODESCRIPT"

