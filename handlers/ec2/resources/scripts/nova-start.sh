#!/bin/bash


source ${EUCA_KEY_DIR}/novarc
###source ${NOVA_SCRIPTS_DIR}/nova-common.sh
source $(dirname $0)/nova-common.sh


#echo Starting nova-start.sh

#temp for debugging
logfile=/tmp/start.sh.log
#exec > $logfile 2>&1
#exec > $logfile >&2


#export PATH=${EC2_HOME}/bin:${PATH}

if [ ! -z $EUCA_GROUP ]; then
	GROUP="-g $EUCA_GROUP"      
fi
    
if [ ! -z $NEUCA_INI ]; then
	USER_DATA="--user-data-file $NEUCA_INI"
fi

if [ ! -z $AKI_NAME ]; then
	KERNEL="--kernel $AKI_NAME"
fi

if [ ! -z $ARI_NAME ]; then
	INITRD="--ramdisk $ARI_NAME"
fi

if [ ! -z $EC2_INSTANCE_TYPE ]; then
	INSTANCE_TYPE="--instance-type $EC2_INSTANCE_TYPE"
fi

function exit_cleanly {
  EXIT=`echo $?`
  if [ $EXIT != "0" ]
  then
    echo `date`:"start.sh: Exit code of $EXIT for last command.  Exiting"  >> $logfile
    exit 1
  fi
}   

function testSSH() {
	local user=${1}
	local host=${2}
	local key=${3}
	local timeout=${4}
	
	SSH_OPTS="-q -o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no"
	
	ssh -q -q $SSH_OPTS -o "BatchMode=yes" -o "ConnectTimeout ${timeout}" -i $key ${user}@${host} "echo 2>&1" && return 0 || return 1
}

# check that instance size is appropriate for the image (if set)
if [ ! -z $EC2_INSTANCE_TYPE ]; then
	OUT=`check_image_size $AMI_NAME $EC2_INSTANCE_TYPE`
	if [ "$?" != "0" ]; then
		echo `date`: "start.sh: check_image_size returned: $OUT"
		exit 1
	fi
fi

# authorize ssh access from anywhere to the group containing the vm
if [ -z $EUCA_GROUP ]; then
	# the vm is in the default group
	euca-authorize -P tcp -p 22 -s 0.0.0.0/0 default > /dev/null 2>&1
else
	# the vm is in a custom group
	euca-authorize -P tcp -p 22 -s 0.0.0.0/0 $EUCA_GROUP > /dev/null 2>&1
fi

# try allocate an IP address if necessary (false is default if unset)
#if ${EC2_USE_PUBLIC_ADDRESSING=false} ; then
#	PUBLIC_IP=$(assign-public-ip 'i-00000000')
#	if [ "$?" != "0" ]; then
#           echo `date`: ERROR: cannot assign public IP, exiting  >> $logfile
#           exit 1
#	fi
#fi

#echo `date`: Allocated public ip: $PUBLIC_IP  >> $logfile
echo `date`: "euca-run-instances --addressing private -k $EC2_SSH_KEY $USER_DATA $AMI_NAME $KERNEL $INITRD $GROUP $INSTANCE_TYPE" >> $logfile

#sleep 100

INSTANCE_ID=`euca-run-instances --addressing private -k $EC2_SSH_KEY $USER_DATA $AMI_NAME $KERNEL $INITRD $GROUP $INSTANCE_TYPE 2>> /dev/null| grep INSTANCE | awk '{print $2}'`
exit_cleanly

if [ -z ${INSTANCE_ID} ]; then
	echo `date`: "start.sh: after executing \"euca-run-instances --addressing private -k $EC2_SSH_KEY $USER_DATA $AMI_NAME $KERNEL $INITRD $GROUP $INSTANCE_TYPE\" unable to create a VM, exiting."  >> $logfile
#	if ${EC2_USE_PUBLIC_ADDRESSING} ; then
#		euca-release-address $PUBLIC_IP > /dev/null 2>&1
#	fi
	exit 1
fi


validateInstanceID $INSTANCE_ID
if [ "$?" != "0" ]; then
     echo `date`: nova-start.sh recieved invalid instance id: "($INSTANCE_ID)" >> $logfile
     exit 1
fi



echo `date`: INSTANCE_ID $INSTANCE_ID, PID $$, PPID $PPID   >> $logfile

echo `date`: Waiting for $INSTANCE_ID to enter running state  >> $logfile
# wait for the machine to become ready. ec2-describe-instances gets stuck and times out sometimes
FAILCOUNT=0
STATUS=$(nova-get-instance-status $INSTANCE_ID) 
#if [ "$STATUS" == "0" ]; then
#	# could not get status
#	echo "start.sh: getting status. no such instance or persistent problem with euca-describe-instances, exiting"
#	exit 1
#fi

# 30 minutes
count=180
while [ "$STATUS" = "pending" -o "$STATUS" = "VM_DOES_NOT_EXIST" ] && [ "$count" != "0" ]
do      
	sleep 10
	STATUS=$(nova-get-instance-status $INSTANCE_ID)
	if [ "$STATUS" == "EUCA_ERROR" ]; then
		# could not get status
	        echo `date`: "start.sh: getting status. no such instance or persistent problem with euca-describe-instances, exiting"  >> $logfile
       		exit 1
	fi

	# ensuring progress
	count=$(($count - 1))
done

echo `date`: $INSTANCE_ID is in state $STATUS  >> $logfile

if [ "$STATUS" != "running" ]; then
	echo `date`: "start.sh: VM instance $INSTANCE_ID transitioned into $STATUS instead of 'running', terminating instance and exiting"  >> $logfile
	terminateFailedVM $INSTANCE_ID $PUBLIC_IP
	exit 1
fi

# associate IP with the VM
# try allocate an IP address if necessary (false is default if unset)
if ${EC2_USE_PUBLIC_ADDRESSING=false} ; then
        PUBLIC_IP=$(assign-public-ip $INSTANCE_ID)
        if [ "$?" != "0" ]; then
           echo `date`: ERROR: cannot assign public IP, terminating instance $INSTANCE_ID with public_ip $PUBLIC_IP exiting  >> $logfile
	   terminateFailedVM $INSTANCE_ID $PUBLIC_IP
           exit 1
        fi
fi


#test for valid public_ip
PUBLIC_IP=$(nova-get-instance-public-ip $INSTANCE_ID)
validateIP $PUBLIC_IP
if [ "$?" != "0" ]; then
   echo `date`: nova-start.sh invalid PUBLIC_IP terminating instance $INSTANCE_ID: "($PUBLIC_IP)" >> $logfile
   terminateFailedVM $INSTANCE_ID $PUBLIC_IP
   exit 1
fi


echo `date`: Ping test: $STATUS $INSTANCE_ID $PUBLIC_IP  >> $logfile
if [ -z ${EC2_PING_RETRIES} ] || [ "${EC2_PING_RETRIES}" = "0" ]
then
	UNPINGABLE=false
else
	# wait until the instance is pingable
	UNPINGABLE=true
	for ((i=0;i<${EC2_PING_RETRIES=60};i+=1));
	do
		PUBLIC_IP=$(nova-get-instance-public-ip $INSTANCE_ID)
		#echo ping test $INSTANCE_ID  $IP $i
		PING=`ping -c 1 $PUBLIC_IP > /dev/null 2>&1`
		if [ "$?" = "0" ]; then
			UNPINGABLE=false	
			break
		fi
		echo `date`: Ping test fail  $STATUS $INSTANCE_ID $PUBLIC_IP, retry $i  >> $logfile
		sleep 10
	done
fi

echo `date`: Ping test $i:  $STATUS $INSTANCE_ID $PUBLIC_IP, success  >> $logfile

if ${UNPINGABLE} ; then
	echo `date`: "Ping test:  $INSTANCE_ID $PUBLIC_IP, failed terminating instance and exiting." >> $logfile
	terminateFailedVM $INSTANCE_ID $PUBLIC_IP
	exit 1
fi
echo `date`: Ping test:  $INSTANCE_ID $PUBLIC_IP, success  >> $logfile


echo `date`: ssh test: $STATUS $INSTANCE_ID $PUBLIC_IP  >> $logfile
if [ -z ${EC2_SSH_RETRIES} ] || [ "${EC2_SSH_RETRIES}" = "0" ]
then
	UNSSHABLE=false
else
	UNSSHABLE=true
	KEY=${EUCA_KEY_DIR}/${EC2_SSH_KEY}
	for ((i=0;i<${EC2_SSH_RETRIES=10};i+=1));
	do
		testSSH root $PUBLIC_IP $KEY 5
		if [ "$?" = "0" ]; then
			UNSSHABLE=false
			break
		fi
		echo  `date`: ssh test fail instance: $INSTANCE_ID, public_ip:  $PUBLIC_IP, retry $i  >> $logfile
		sleep 10
	done
fi

if ${UNSSHABLE} ; then
	echo `date`: "ssh test:  $INSTANCE_ID $PUBLIC_IP, failed terminating instance and exiting." >> $logfile
	terminateFailedVM $INSTANCE_ID $PUBLIC_IP
	exit 1
fi

echo `date`: ssh test:  $INSTANCE_ID $PUBLIC_IP, success  >> $logfile



echo $INSTANCE_ID
