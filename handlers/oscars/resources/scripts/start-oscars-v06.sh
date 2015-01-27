#!/bin/bash 

# Create a reservation for a ION/OSCARS circuit
# parameters are: 
# OSCARS_HOME and OSCARS_IDC, OSCARS_QUERY_INTERVAL(in sec) and OSCARS_DESC[optional] and OSCARS_PATHSETUP[optional] (from static properties in oscars.site.properties)
# OSCARS_L2_SRC, OSCARS_L2_DST and OSCARS_BW, OSCARS_L2_SRC_TAG and OSCARS_L2_DST_TAG (from controller)
# and either OSCARS_START_TIME and OSCARS_END_TIME (in Unix time; seconds since the epoch)
# or
# OSCARS_DURATION (in seconds) for immediate reservations

# prep
if [ -z ${OSCARS_SCRIPTS_DIR} ]; then
    echo "start-oscars.sh: OSCARS package scripts directory undefined, exiting"
    exit 1
fi

if [ ! -z ${OSCARS_DESC} ]; then
    DESC="--description $OSCARS_DESC"
else
    DESC="--description ORCA-initiated-circuit"
fi

if [ -z ${OSCARS_QUERY_INTERVAL} ]; then
    OSCARS_QUERY_INTERVAL=5
fi

# parameter checks
if [ -z ${OSCARS_L2_SRC} ] || [ -z ${OSCARS_L2_DST} ]; then
	echo "start-oscars-v06.sh: You must specify both OSCARS_L2_SRC and OSCARS_L2_DST"
	exit 1
fi

if [ -z ${OSCARS_L2_SRC_TAG} ] || [ -z ${OSCARS_L2_DST_TAG} ]; then
	echo "start-oscars-v06.sh: You must specify source and destination VLAN tags"
fi

# GEC17 tag-remapping hack
# remap certain tags 
function remap_tag() 
{
if [ "$1" == "3195" ]; then
	echo 4001
elif [ "$1" == "3196" ]; then
	echo 4002
elif [ "$1" == "3197" ]; then
	echo 4003
elif [ "$1" == "3198" ]; then
	echo 4004
elif [ "$1" == "3199" ]; then
	echo 4005
else
	echo $1	
fi
}

#VLANSPEC="--tagA $(remap_tag ${OSCARS_L2_SRC_TAG}) --tagZ $(remap_tag ${OSCARS_L2_DST_TAG})"
VLANSPEC="--tagA ${OSCARS_L2_SRC_TAG} --tagZ ${OSCARS_L2_DST_TAG}"

if [ -z ${OSCARS_BW} ]; then
	# set default bandwidth 100Mbps
	BW=100
else
	# if coming from orca, divide by 10^6 (ORCA uses bps, OSCARS Mbps)
	BW=$(($OSCARS_BW/1000000))
	# if less than 100Mbps, set to 100Mbps
	if [ $((${BW} < 100)) = "1" ]; then
		BW=100
	fi
fi

# immediate or advance
if [ ! -z ${OSCARS_START_TIME} ] && [ ! -z ${OSCARS_END_TIME} ] ; then
    TIMESPEC="--start ${OSCARS_START_TIME} --end ${OSCARS_END_TIME}"
else
    if [ ! -z ${OSCARS_DURATION} ]; then
        TIMESPEC="--duration ${OSCARS_DURATION}"
    else
		echo "start-oscars.sh: Either OSCARS_START_TIME and OSCARS_END_TIME or OSCARS_DURATION must be specified, exiting"
		exit 1
    fi
fi

. ${OSCARS_SCRIPTS_DIR}/oscars-common-v06.sh

# send stderr there
ERRFILE="/tmp/oscars.$$.tmp"
trap 'rm -f $ERRFILE' EXIT

# create path
if ${OSCARS_DEBUG=false} ; then
    echo "start-oscars-v06.sh: DEBUG IS ON: running reservation request from src ${OSCARS_L2_SRC} to dst ${OSCARS_L2_DST} with VLAN ${VLANSPEC} and timespec ${TIMESPEC}"
fi

GRI=`run_oscars create --intA ${OSCARS_L2_SRC} --intZ ${OSCARS_L2_DST} ${VLANSPEC} ${TIMESPEC} --bw ${BW} ${DESC} --poll ${OSCARS_QUERY_INTERVAL} -c 2> ${ERRFILE}`

if ${OSCARS_DEBUG=false} ; then
    echo "start-oscars-v06.sh: DEBUG IS ON: New GRI=${GRI}"
fi

if [ -z ${GRI} ] ; then
    # grep for error
    ERRORMSG=`cat ${ERRFILE}`
    echo "start-oscars-v06.sh: OSCARS did not return a GRI to createReservation request due to: \"${ERRORMSG}\", exiting"
    exit 1
else
    echo ${GRI}
    exit 0
fi
