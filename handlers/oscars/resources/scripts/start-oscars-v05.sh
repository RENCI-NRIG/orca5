#!/bin/bash 

# Create a reservation for a ION/OSCARS circuit
# parameters are: 
# OSCARS_SCRIPTS_DIR (from static build properties based on package)
# OSCARS_HOME and OSCARS_IDC, OSCARS_QUERY_INTERVAL(in sec) and OSCARS_DESC[optional] and OSCARS_PATHSETUP[optional] (from static properties in oscars.site.properties)
# OSCARS_L2_SRC, OSCARS_L2_DST and OSCARS_BW, OSCARS_L2_SRC_TAG, OSCARS_L2_DST_TAG [both optional] (from controller)
# and either OSCARS_START_TIME and OSCARS_END_TIME (in Unix time; seconds since the epoch)
# or
# OSCARS_DURATION (in seconds)

# prep
if [ -z ${OSCARS_SCRIPTS_DIR} ]; then
    echo "start-oscars.sh: OSCARS package scripts directory undefined, exiting"
    exit 1
fi

if [ ! -z ${OSCARS_DESC} ]; then
    DESC="-desc $OSCARS_DESC"
else
    DESC="-desc ORCA-initiated-circuit"
fi

if [ -z ${OSCARS_QUERY_INTERVAL} ]; then
    OSCARS_QUERY_INTERVAL=5
fi

if [ ! -z ${OSCARS_PATHSETUP} ]; then
    PATHSETUP="-pathsetup $OSCARS_PATHSETUP"
else
    PATHSETUP="-pathsetup timer-automatic"
fi

if [ ! -z ${OSCARS_L2_SRC_TAG} ]; then
    VLAN="-vlan ${OSCARS_L2_SRC_TAG}"
else
    VLAN="-vlan any"
fi

# parameter checks
if [ -z ${OSCARS_L2_SRC} ] || [ -z ${OSCARS_L2_DST} ]; then
	echo "start-oscars.sh: You must specify both OSCARS_L2_SRC and OSCARS_L2_DST"
	exit 1
fi

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
    START_TIME=${OSCARS_START_TIME}
    END_TIME=${OSCARS_END_TIME}
else
    if [ ! -z ${OSCARS_DURATION} ]; then
		# from now until duration
		START_TIME=`date +%s`
		END_TIME=$(($START_TIME + $OSCARS_DURATION))
		IMMEDIATE=true
    else
		echo "start-oscars.sh: Either OSCARS_START_TIME and OSCARS_END_TIME or OSCARS_DURATION must be specified, exiting"
		exit 1
    fi
fi

. ${OSCARS_SCRIPTS_DIR}/oscars-common.bash

# hack for explicit route for SC11
if [ ! -z ${OSCARS_PATH} ]; then
	OPATH="-path ${OSCARS_PATH}"
else
	OPATH=
fi

# for OSCARS java tools
build_oscars_classpath

# send stderr there
ERRFILE="/tmp/oscars.$$.tmp"
trap 'rm -f $ERRFILE' EXIT

pushd ${OSCARS_API_HOME}/examples >/dev/null 2>&1

# create path
if ${OSCARS_DEBUG=false} ; then
    echo "start-oscars.sh: DEBUG IS ON: running reservation request from src ${OSCARS_L2_SRC} to dst ${OSCARS_L2_DST} starting ${START_TIME} until ${END_TIME}"
fi

GRI=`run_oscars createReservation -repo ${OSCARS_CERT_REPO} -url ${OSCARS_IDC} ${OPATH} -l2source ${OSCARS_L2_SRC} -l2dest ${OSCARS_L2_DST} -start ${START_TIME} -end ${END_TIME} -bwidth ${BW} ${DESC} ${VLAN} ${PATHSETUP} 2> ${ERRFILE} | awk '/^GRI:/ { print $2}'`

if ${OSCARS_DEBUG=false} ; then
    echo "start-oscars.sh: DEBUG IS ON: New GRI=${GRI}"
fi

if [ -z ${GRI} ] ; then
    # grep for error
    ERRORMSG=`grep "Caused by" ${ERRFILE}`
    echo "start-oscars.sh: OSCARS did not return a GRI to createReservation request due to: \"${ERRORMSG}\", exiting"
    exit 1
fi

# if it is immediate reservation, (default not),
# then we wait until it goes into ACTIVE
# otherwise exit while PENDING
if ${IMMEDIATE=false}; then
# immediate reservation
    if ${OSCARS_DEBUG=false} ; then
	echo "start-oscars.sh: DEBUG IS ON: Waiting for immediate GRI to become ACTIVE"
    fi

    STATUS="SUBMITTED"
    while [ "${STATUS}" = "SUBMITTED" ] || [ "${STATUS}" = "ACCEPTED" ] || [ "${STATUS}" = "INSETUP" ] || [ "${STATUS}" = "PENDING" ]; do
	if ${OSCARS_DEBUG=false} ; then
	    echo "start-oscars.sh: DEBUG IS ON: running query for GRI ${GRI}"
	fi
	STATUS=`run_oscars query -repo ${OSCARS_CERT_REPO} -url ${OSCARS_IDC} -gri ${GRI} 2> /dev/null | awk '/^Status:/ { print $2}'`
	sleep ${OSCARS_QUERY_INTERVAL}
    done

    if [ "${STATUS}" != "ACTIVE" ]; then
	echo "start-oscars.sh: returning status for immediate reservation GRI ${GRI} expected ACTIVE, received ${STATUS}, exiting."
	exit 1
    fi
else
    # advance reservation
    if ${OSCARS_DEBUG=false} ; then
	echo "start-oscars.sh: DEBUG IS ON: Waiting for advance GRI to become PENDING"
    fi
    STATUS="SUBMITTED" 
    while [ "${STATUS}" = "SUBMITTED" ]; do
	if ${OSCARS_DEBUG=false} ; then
	    echo "start-oscars.sh: DEBUG IS ON: running query for GRI ${GRI}"
	fi
	STATUS=`run_oscars query -repo ${OSCARS_CERT_REPO} -url ${OSCARS_IDC} -gri ${GRI} 2> /dev/null | awk '/^Status:/ { print $2}'`
	sleep ${OSCARS_QUERY_INTERVAL}
    done

    if [ "${STATUS}" != "PENDING" ]; then
	echo "start-oscars.sh: returning status for advanced reservation GRI ${GRI} expected PENDING, received ${STATUS}, exiting."
	exit 1
    fi
fi

# get unit.vlan.tag encoded (GRI|SRC PORT|SRC TAG|DST PORT|DST TAG)
run_oscars query -repo ${OSCARS_CERT_REPO} -url ${OSCARS_IDC} -gri ${GRI} 2> /dev/null | awk -v resid="${GRI}" -v pat1="${OSCARS_L2_SRC}" -v pat2="${OSCARS_L2_DST}" '
BEGIN { mymatch=0 }
/Path:/   {mymatch=1 }
$0 ~ pat1 { if (mymatch == 1) src=$0 }
$0 ~ pat2 { if (mymatch == 1) dst=$0 }
END {
    gsub(/[ \t]/, "", src);
    gsub(/[ \t]/, "", dst);
    n=split(src, srcarray, ",");
    n=split(dst, dstarray, ",");
    printf("%s|%s|%s|%s|%s\n", resid, srcarray[1], srcarray[3], dstarray[1], dstarray[3]);
}' 

popd >/dev/null 2>&1
