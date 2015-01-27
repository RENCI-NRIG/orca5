#!/bin/bash

source ${EUCA_KEY_DIR}/novarc
source $(dirname $0)/nova-common.sh

logfile=/tmp/start.sh.log

function exit_cleanly {
  EXIT=`echo $?`
  if [ $EXIT != "0" ]
  then
    echo "stop.sh: Exit code of $EXIT for last command.  Exiting"  >> $logfile
    exit 1
  fi
}   
        
for instance_id in ${1}; do
	echo `date`: Shutting down instance $instance_id -- EC2_USE_PUBLIC_ADDRESSING: **$EC2_USE_PUBLIC_ADDRESSING**>> $logfile


        validateInstanceID $instance_id
        if [ "$?" != "0" ]; then
            echo `date`: nova-stop.sh called with invalid instance id: "($instance_id)" >> $logfile
            continue
        fi



        public_ip=$(nova-get-instance-public-ip $instance_id)
        if [ "$public_ip" == "VM_DOES_NOT_EXIST" ]; then
                #instance does not exist exit
                echo `date`: instance $instance_id does not exist >> $logfile
                continue
        fi
	
	# determine and release the public IP address
	echo `date`: Shutting down instance $instance_id with public ip $public_ip >> $logfile 	


        validateIP $public_ip
        if [ "$?" == "0" ]; then
	   if ${EC2_USE_PUBLIC_ADDRESSING=false}; then	
             echo `date`: Disassociating/Releasing ip $public_ip -- from instance $instance_id >> $logfile
             disassociateAddress $public_ip
             releaseAddress $public_ip
           fi
        fi
        #terminate the vm 
	echo `date`: cleanupVM instance: $instance_id >> $logfile
        cleanupVM $instance_id
done


