#!/bin/bash


logfile=/tmp/start.sh.log

#vars for locking around allocating/releasing public ips
lockfile=/tmp/orca.public_ip.lock
lockfd=200
locktimeout=300

function euca-allocate-address-locking ()
 { 
  PUBLIC_IP=""
  STATUS="1"

  (  if flock -w 300 200
     then
	 echo  `date`: euca-allocate-address-locking: got lock >> $logfile
         PUBLIC_IP=`euca-allocate-address 2> /dev/null`
         STATUS=$?
         echo  `date`: euca-allocate-address-locking: inside flock $PUBLIC_IP, STATUS: $STATUS  >> $logfile
         echo $PUBLIC_IP
         exit $STATUS 
     else
	echo  `date`: euca-allocate-address-locking: lock timed out >> $logfile
	exit 1
     fi
  ) 200>/tmp/orca.public_ip.lock
  
  STATUS=$?

  echo  `date`: euca-allocate-address-locking: returning $PUBLIC_IP, STATUS: $STATUS  >> $logfile
  exit $STATUS
}




function assign-public-ip(){
 local instance=${1}
 

#allocat an address from nova        
 FAILCOUNT=0
 #PUBLIC_IP=`euca-allocate-address 2> /dev/null`
 PUBLIC_IP=$(euca-allocate-address-locking)
 if [ "$?" != "0" ]; then
   #echo "start.sh: Unable to allocate public IP addr" 
   PUBLIC_IP="NO_IP"
   FAILCOUNT=$((FAILCOUNT+1))
 else
   PUBLIC_IP=`echo $PUBLIC_IP | awk '{print $2}'`
 fi

 while [ "$PUBLIC_IP" == "NO_IP" ]
   do
   sleep 5
   #PUBLIC_IP=`euca-allocate-address 2> /dev/null`
   PUBLIC_IP=$(euca-allocate-address-locking)
   if [ "$?" != "0" ]; then
      echo  `date`: force PUBLIC_IP to NO_IP for now, assume a transient problem, but keep count >> $logfile
      echo  `date`: $PUBLIC_IP >> $logfile
        PUBLIC_IP="NO_IP"
        FAILCOUNT=$((FAILCOUNT+1))
        if [ "$FAILCOUNT" -eq 10 ]; then
           echo `date`: "start.sh: persistent problem with euca-allocate-address, exiting ($FAILCOUNT)"  >> $logfile
           exit 1
        fi
   else
        PUBLIC_IP=`echo $PUBLIC_IP | awk '{print $2}'`
   fi
 done

 echo `date`: Allocated public ip: $PUBLIC_IP  >> $logfile

 # associate IP with the VM
 FAILCOUNT=0
 while  [ "$PUBLIC_IP" != "$ASSIGNED_IP" ]
   do
   
   if [ "$FAILCOUNT" -eq 8 ]; then
      echo `date`: "persistent problem with euca-associate-address, exiting ($FAILCOUNT)"  >> $logfile
      exit 2
   fi
      
   sleep $(( 2**$FAILCOUNT - 1 ))  #sleep for increasing amounts starting with 0
   echo  `date`: Associate address $PUBLIC_IP with instance $INSTANCE_ID, attempt $FAILCOUNT >> $logfile
   euca-associate-address -i $INSTANCE_ID $PUBLIC_IP 2> /dev/null
   ASSIGNED_IP=$(nova-get-instance-public-ip $INSTANCE_ID)
 
   FAILCOUNT=$((FAILCOUNT+1))
 done


 ASSIGNED_IP=`echo $ASSIGNED_IP | awk '{print $2}'`
 echo  `date`: Associate address $ASSIGNED_IP with instance $INSTANCE_ID, success >> $logfile
 echo $ASSIGNED_IP	
 exit 0
}

function free-public-ip(){
	local instance=${1}

}

#helper function to get euca-describe instance data
function  nova-get-euca-describe-instance() {
        local instance=${1}
        local timeout=${2}
        local retries=${3}

        EUCA_FAIL_COUNT=0
        DOES_NOT_EXIST_COUNT=0
        DONE=false
        while [ -n $DONE ]
        do
                DATA_LINE=`euca-describe-instances $instance`
                #echo loop: $DATA_LINE
                if [ "$?" != "0" ]; then
                        FAIL_COUNT=$((EUCA_FAIL_COUNT+1))
                        if [ $EUCA_FAIL_COUNT -gt $retries ]; then
                                echo "EUCA_FAIL"
                                exit
                        fi
                        sleep $timeout
                else
                        #euca success                                                                                                                                                   
                        if [ "`echo $DATA_LINE |  awk '{print $5}'`" != "INSTANCE" ] ||  [ "`echo $DATA_LINE |  awk '{print $6}'`" != "$instance" ]; then
                                DATA_LINE="VM_DOES_NOT_EXIST"
                        fi
                        DONE=true
                        echo $DATA_LINE
                        exit

                fi

        done

        echo $DATA_LINE
        exit

}





#Get any field from euca-describe-instances

function nova-get-instance-info() {
        local instance=${1}
        local column=${2}
        local timeout=${3}
        local retries=${4}
	
	#echo nova-get-instance-info $1 $2 $3 $4

	#Get a line of data from euca-describe-instances
	VM_BROKEN_COUNT=0
	#DATA_LINE=`euca-describe-instances $instance 2>/dev/null`
	DATA_LINE=$(nova-get-euca-describe-instance $instance $timeout $retries)
	if [ "$DATA_LINE" == "VM_DOES_NOT_EXIST" ] || [ "$DATA_LINE" == "EUCA_FAIL" ] ; then
   		echo $DATA_LINE
		exit
	fi

  	WC=`echo $DATA_LINE | wc -w 2>/dev/null`
	VM_BROKEN_COUNT=$((VM_BROKEN_COUNT+1))
	while [ "$WC" != "19" ]  
	do
        	sleep $timeout
        	DATA_LINE=$(nova-get-euca-describe-instance $instance $timeout $retries)
		WC=`echo $DATA_LINE | wc -w 2>/dev/null`
		#echo DATA_LINE for $instance: $DATA_LINE >> $ERROR_LOG
	        if [ "$DATA_LINE" == "VM_DOES_NOT_EXIST" ] || [ "$DATA_LINE" == "EUCA_FAIL" ] ; then
        	        echo $DATA_LINE
                	exit
        	fi

		VM_BROKEN_COUNT=$((VM_BROKEN_COUNT+1))		
                if [ "$VM_BROKEN_COUNT" -ge $retries ]; then
                        #echo "get-instance-info: persistent problem with euca-describe-instances, exiting" >&2
                        echo "VM_ERROR"
			exit
                fi
	done

	echo $DATA_LINE | awk '{print $'$column' }'
}


function nova-get-instance-public-ip(){
	local instance=${1}
	
	#nova-get-instance-info instance column timeout retries
	echo $(nova-get-instance-info $1 8 5 6)
	exit
}

function nova-get-instance-private-ip(){
        local instance=${1}

        #nova-get-instance-info instance column timeout retries
        echo $(nova-get-instance-info $1 9 5 6)
 	exit
}

function nova-get-instance-status(){
        local instance=${1}

        #nova-get-instance-info instance column timeout retries
        echo $(nova-get-instance-info $1 10 5 6)
}

function nova-get-instance-host(){
        local instance=${1}

        #nova-get-instance-info instance column timeout retries
        RAW=$(nova-get-instance-info $1 13 5 6)
	LEN=`expr length $RAW`
	LEN=$((LEN-1))
	echo `expr substr $RAW 1 $LEN`		

}

#returns the 1 if instance exists, 0 othewise
#does not wait for full row of data (IPs may not exist) 
#tries 6 times waiting 5 secs between tries
function nova-instance-exists(){
	local instance=${1}
        local column=2
        local timeout=5
        local retries=6

	VALUE=$(nova-get-describe-instance $instance $timeout $retries)

	if [ "$VALUE" == "DOES_NOT_EXIST" ]; then
		echo 0
	else
		echo 1
	fi
}


function nova-instance-exists(){
        local instance=${1}
        local column=2
        local timeout=5
        local retries=6

        VALUE=$(nova-get-euca-describe-instance $instance $timeout $retries)

        if [ "$VALUE" == "VM_DOES_NOT_EXIST" ]; then
                echo 0
        else
                echo 1
        fi
}


function isAddressAllocated(){
        local public_ip=${1}

        validateIP $public_ip
        if [ "$?" != "0" ]; then
             echo `date`: isAddressAllocated called with invalid public_ip: "($public_ip)" >> $logfile
	     #return not allocated
	     echo 0
             exit 
        fi



        #echo isAddressAllocated $public_ip
        FAILCOUNT=0
        DONE=false

        DATA=`euca-describe-addresses $public_ip`
        if [ "$?" == "0" ]; then
            # data is good
            if [ "$DATA" == "" ]; then
                #not allocated
                DONE=true
                echo 0
                exit
            else
                #allocated
                DONE=true
                echo 1
                exit
            fi
        fi

        # first try at data is bad, retry
        FAILCOUNT=$((FAILCOUNT+1))

        while [ -n $DONE ]
        do
            echo `date`: "Unable to test for public IP addr $public_ip (count $FAILCOUNT), retrying" >> $logfile
            sleep 5

            DATA=`euca-describe-addresses $public_ip`
            if [ "$?" == "0" ]; then
                # data is good
                if [ "$DATA" == "" ]; then
                   #not allocated
                   DONE=true
                   echo 0
                   exit
                else
                   #allocated
                   DONE=true
                   echo 1
                   exit
               fi
            else
                FAILCOUNT=$((FAILCOUNT+1))
                echo `date`: "Unable to test for public IP addr $public_ip (count $FAILCOUNT), retrying" >> $logfile
            fi
        done

        echo `date`: "persistent problem trying to test for public IP addr $public_ip, ($FAILCOUNT), skipping" >> $logfile
        echo 0
        exit
}

function isAddressAssociated(){
        local public_ip=${1}

        validateIP $public_ip
        if [ "$?" != "0" ]; then
             echo `date`: isAddressAssociated called with invalid public_ip: "($public_ip)" >> $logfile
             #return not associated
	     echo 0
             exit 0
        fi

        #echo isAddressAssociated $public_ip
        FAILCOUNT=0
        DONE=false

        RAWDATA=`euca-describe-addresses $public_ip`
        #DATA=`awk '{print $3}' <<< $RAWDATA`        
        if [ "$?" == "0" ]; then
            DATA=`awk '{print $3}' <<< $RAWDATA`
            #echo $DATA
            # data is good
	    if [ "$DATA" == "" ]; then
		DONE=true
		echo `date`:  public ip $public_ip already free >> $logfile
		#exit 0
 	    fi

            if [ "$DATA" == "None" -o "$DATA" == "" ]; then
                #not associated
                DONE=true
                echo 0
                #exit 0
            else
                #associated
                DONE=true
                echo 1
                #exit 0
            fi
        fi

        # first try at data is bad, retry
        FAILCOUNT=$((FAILCOUNT+1))

        while [ -n $DONE ]
        do
            echo `date`: " Unable to test for public IP addr $public_ip (count $FAILCOUNT), retrying" >> $logfile
            sleep 5

            RAWDATA=`euca-describe-addresses $public_ip`
            
            if [ "$?" == "0" ]; then
              DATA=`awk '{print $3}' <<< $RAWDATA`
              #echo $DATA
              # data is good
              if [ "$DATA" == "" ]; then
                  DONE=true
                  echo `date`:  public ip $public_ip already free >> $logfile
                  exit 0
              fi

              if [ "$DATA" == "None" -o "$DATA" == "" ]; then
                  #not associated
                  DONE=true
                  echo 0
                  exit 0
               else
                  #associated
                  DONE=true
                  echo 1
                  exit 0
               fi
           fi
           
	   if [ "$FAILCOUNT" -eq 10 ]; then
                    echo `date`: "persistent problem with euca-discribe-addresses $public_ip, skipping ($FAILCOUNT), skipping" >> $logfile
                    echo 0
		    exit 1
           fi


           FAILCOUNT=$((FAILCOUNT+1))
           echo `date`: " Unable to test for public IP addr $public_ip (count $FAILCOUNT), retrying" >> $logfile
        done

	exit 0
}


function validateIP() {
# Code cribbed from comp.unix.shell post
# http://groups.google.com/group/comp.unix.shell/msg/96bb870de95c9aed?dmode=source
        case "$*" in
                ""|*[!0-9.]*|*[!0-9]) return 1 ;;
        esac

        local IFS=.
        set -- $*
        [ $# -eq 4 ] &&
        [ ${1:-666} -le 255 ] && [ ${2:-666} -le 255 ] &&
        [ ${3:-666} -le 255 ] && [ ${4:-666} -le 254 ]
}

function validateInstanceID() {

        case "$*" in
                ""i-[[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]]) return 0 ;;
        esac

        return 1
}



function disassociateAddress(){
        local public_ip=${1}

        validateIP $public_ip
        if [ "$?" != "0" ]; then
             echo `date`: euca-disassociate-address called with invalid public_ip: "($public_ip)" >> $logfile
	     exit 1	 
        fi

        echo `date`: euca-disassociate-address $public_ip >> $logfile
	

        FAILCOUNT=0
        euca-disassociate-address $public_ip
        STATUS=$(isAddressAssociated $public_ip)
        while [ "$STATUS" == "1" ]
        do
            echo  "`date`: start.sh: Unable to disassociate public IP addr $public_ip (count $FAILCOUNT), retrying" >> $logfile
            FAILCOUNT=$((FAILCOUNT+1))
            sleep 5
            euca-disassociate-address $public_ip

            STATUS=$(isAddressAssociated $public_ip)
            if [ "$FAILCOUNT" -eq 10 ]; then
                    echo `date`: "start.sh: persistent problem with euca-disassociate-address $public_ip, skipping ($FAILCOUNT), skipping" >> $logfile
                    exit
            fi

         done

	echo `date`: euca-disassociate-address $public_ip, success  >> $logfile
}


function releaseAddress(){
  local public_ip=${1}

  validateIP $public_ip
  if [ "$?" != "0" ]; then
     echo `date`: releaseAddress called with invalid public_ip: "($public_ip)" >> $logfile
     exit 1
  fi

   echo `date`: release addess $public_ip >> $logfile	

  ( if flock -w 300 200
      then        
        echo `date`: euca-release-address $public_ip >> $logfile
        FAILCOUNT=0
        euca-release-address $public_ip
        STATUS=$(isAddressAllocated $public_ip)
        while [ "$STATUS" == "1" ]
        do
            echo  `date`: " Unable to release public IP addr $public_ip (count $FAILCOUNT), retrying" >> $logfile
            FAILCOUNT=$((FAILCOUNT+1))
            sleep 5
            euca-release-address $public_ip

            STATUS=$(isAddressAllocated $public_ip)
            if [ "$FAILCOUNT" -eq 10 ]; then
                    echo `date`: "persistent problem with euca-release-address $public_ip, skipping ($FAILCOUNT), skipping" >> $logfile
                    exit 1
            fi

        done
	echo `date`: euca-release-address: $public_ip, success >> $logfile
	exit 0
     else
       echo  `date`: euca-release-address: $public_ip, lock timed out >> $logfile
       exit 1
     fi
 ) 200>/tmp/orca.public_ip.lock

 
}


function cleanupVM(){
    local instance_id=${1}

    validateInstanceID $instance_id
    if [ "$?" != "0" ]; then
        echo `date`: cleanupVM called with invalid instance id: "($instance_id)" >> $logfile
        exit 1
    fi



    echo `date`: terminateVM $instance_id  >> $logfile
    FAILCOUNT=1
    euca-terminate-instances $instance_id
    sleep 10
    STATUS=$(nova-instance-exists $instance_id)
    while [ "$STATUS" == "1" ]
    do
        echo  `date`: "start.sh: Unable to terminate instance $instance_id (count $FAILCOUNT), retrying" >> $logfile
        euca-terminate-instances $instance_id
        sleep 10
        FAILCOUNT=$((FAILCOUNT+1))

        STATUS=$(nova-instance-exists $instance_id)
        if [ "$FAILCOUNT" -eq 10 ]; then
                echo `date`: "start.sh: persistent problem with euca-terminate-instances $instance_id, skipping ($FAILCOUNT), skipping" >> $logfile
                exit
        fi
    done

    echo `date`: terminateVM $instance_id, success  >> $logfile
}


function terminateFailedVM() {
    local instance_id=${1}
    local public_ip=${2}

    


    echo `date`:"nova-start.sh: Terminating failed vm $instance_id with public ip $public_ip "  >> $logfile

    validateIP $public_ip
    if [ "$?" == "0" ]; then
      if ${EC2_USE_PUBLIC_ADDRESSING=false}; then
         echo `date`: Disassociating/Releasing ip $public_ip -- from instance $instance_id >> $logfile
         disassociateAddress $public_ip
         releaseAddress $public_ip
      fi
    else 
      echo `date`: terminateFailedVM called with invalid public ip: "($public_ip)" >> $logfile
    fi

    validateInstanceID $instance_id
    if [ "$?" == "0" ]; then
        #terminate the vm                                                                                                                                                     
        cleanupVM $instance_id
    else
        echo `date`: terminateFailedVM called with invalid instance id: "($instance_id)" >> $logfile

    fi

}


function getPublicIPbyInstanceID(){
    local instance_id=${1}

    validateInstanceID $instance_id
    if [ "$?" != "0" ]; then
        echo `date`: getPublicIPbyInstanceID called with invalid instance id: "($instance_id)" >> $logfile
        exit 1
    fi

    

     DATA_LINE=$(nova-get-euca-describe-instance $instance_id 5 5)
        if [ "$DATA_LINE" == "VM_DOES_NOT_EXIST" ]; then
                #instance does not exist exit
                echo instance $instance does not exist  >> $logfile
                continue
        fi

        if [ "$DATA_LINE" != "VM_ERROR" ]; then
                # determine and release the public IP address
                #PUBLIC_IP=`euca-describe-instances | grep ${instance} | awk '{print $4}'`
                PUBLIC_IP=$(nova-get-instance-public-ip $instance_id)
                #if [ "$PUBLIC_IP" == "0" ]; then
                #       # could not get ip
                #       #echo could not get public ip for $instance             
                #fi

                #PRIVATE_IP=`euca-describe-instances | grep ${instance} | awk '{print $5}'`
                PRIVATE_IP=$(nova-get-instance-private-ip $instance_id)
                #if [ "$PRIVATE_IP" == "0" ]; then 
                #       # could not get ip
                #       echo could not get private ip for $instance
                #fi     

                #if [ "$PRIVATE_IP" != "0" ] && [ "$PUBLIC_IP" != "0" ] && [ "$PUBLIC_IP" != "$PRIVATE_IP" ]; then
                #fi
        fi
}


# get the host on which the VM is served
function get_nova_host {
	local INSTANCE=${1}
	local DBUSER=nova
	local DBPASS=nova
	local DB=nova
	local DBHOST=""

	if [ -z $INSTANCE ]; then
		echo "get_nova_host called with no instance id"
		exit 1
	fi

	local INSTANCENUM=`echo ${INSTANCE:2:10}`
	local QUERY="SELECT host FROM instances WHERE id=conv('$INSTANCENUM', 16, 10);"
	local WORKERNODE=`echo $QUERY | mysql -N $DBHOST -u $DBUSER -p$DBPASS $DB`
	echo $WORKERNODE
}

# ensure that the instance size is large enough for the image
# requires AMI and instance type name, optional fudge parameter
# to multiply image size before comparing to instance
# You can define the following env variables to affect its behavior:
# NOVAIMAGES - path to the image storage in NOVA (/var/lib/nova/buckets)
# NOVADB - name of nova database (nova)
# NOVADBUSER - name of nova database users (nova)
# NOVADBPASS - password of nova database user (nova)
# NOVADBHOST - host on which nova db mysql runs (localhost)
function check_image_size {
	local IMAGEID=${1}
	local INSTANCE_TYPE=${2}
	# optional
	local FUDGE=${3}

	local DESCRIBE=euca-describe-images
	local MYSQL=mysql
	local XMLGREP=xml_grep

	# can be overwritten through env settings
	local IMAGES=/var/lib/nova/buckets
	local DBUSER=nova
	local DBPASS=nova
	local DB=nova
	local DBHOST=""

	# check for env settings to override defaults
	if [ ! -z $NOVAIMAGES ]; then
		IMAGES=$NOVAIMAGES
	fi

	if [ ! -z $NOVADBUSER ]; then
		DBUSER=$NOVADBUSER
	fi

	if [ ! -z $NOVADBPASS ]; then
		DBPASS=$NOVADBPASS
	fi

	if [ ! -z $NOVADB ]; then
		DB=$NOVADB
	fi

	if [ ! -z $NOVADBHOST ]; then
		DBHOST="-h $NOVADBHOST"
	fi

	# sanity checks
	if [ -z $IMAGEID ]; then
		echo "check_image_size called without image id"
		exit 1
	fi

	if [ -z $INSTANCE_TYPE ]; then
		echo "check_image_size called without instance type"
		exit 1
	fi

	if [ -z $FUDGE ]; then
		FUDGE=1.1
	fi

	local QUERY="SELECT local_gb from instance_types where name=\"$INSTANCE_TYPE\";"
	local INSTANCESIZE=`echo $QUERY | mysql -N $DBHOST -u $DBUSER -p$DBPASS $DB`
	if [ -z $INSTANCESIZE ]; then
		echo "check_image_size requires a  valid instance type, not $INSTANCE_TYPE"
		exit 1
	fi

	# get information on the image
	local TMP=`$DESCRIBE $IMAGEID`

	if [ "$?" != "0" ]; then
		echo "check_image_size unable to locate image $IMAGEID"
		exit 1
	fi

	local IMAGEPATH=`echo $TMP | awk '{print $3}'`
	if [ ! -r $IMAGES/$IMAGEPATH ]; then
		echo "check_image_size unable to read manifest for $IMAGEID"
		exit 1
	fi
 
	# get image size from the NOVA metafile using xpath
	local IMAGESIZE=`$XMLGREP --text_only //manifest/image/size $IMAGES/$IMAGEPATH`

	# scale the image size (/1 is needed to get rid of trailing decimals in bc)
	IMAGESIZE=`echo "scale=0;$IMAGESIZE*$FUDGE/1" | bc`
	INSTANCESIZE=$(($INSTANCESIZE*1024*1024*1024))

	if [ "$IMAGESIZE" -gt "$INSTANCESIZE" ]; then
		echo "check_image_size $IMAGEID of size $IMAGESIZE will NOT fit into instance $INSTANCE_TYPE of size $INSTANCESIZE"
		exit 1
	else
		echo "OK: Image $IMAGEID will fit into instance $INSTANCE_TYPE"
		exit 0
	fi
}
