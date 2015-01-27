#!/bin/bash 

# Query reservation for a ION/OSCARS circuit
# parameters are: 
# OSCARS_SCRIPTS_DIR (from static build properties based on package)
# OSCARS_HOME and OSCARS_IDC (from static properties in oscars.site.properties)
# OSCARS_GRI

# prep
if [ -z ${OSCARS_SCRIPTS_DIR} ]; then
    echo "OSCARS package scripts directory undefined, cannot proceed"
    exit 1
fi

# extract GRI
if [ ! -z ${OSCARS_UNIT_VLAN_TAG} ]; then
    if ${OSCARS_DEBUG=false} ; then
	echo "stop-oscars.sh: DEBUG IS ON: extracting GRI from unit.vlan.tag"
    fi
    GRI=`awk -v tag="${OSCARS_UNIT_VLAN_TAG}" '
BEGIN {
    n=split(tag, tagarray, "|");
    print tagarray[1]
}'`
else
    if ${OSCARS_DEBUG=false} ; then
	echo "stop-oscars.sh: DEBUG IS ON: getting GRI from OSCARS_GRI"
    fi
    if [ ! -z ${OSCARS_GRI} ]; then
	GRI=${OSCARS_GRI}
    else
	echo "stop-oscars.sh: one of OSCARS_GRI or OSCARS_UNIT_VLAN_TAG must be specified, exiting"
	exit 1
    fi
fi

. ${OSCARS_SCRIPTS_DIR}/oscars-common.bash

# for OSCARS java tools
build_oscars_classpath

ERRFILE="/tmp/oscars.$$.tmp"
trap 'rm -f $ERRFILE' EXIT

pushd ${OSCARS_API_HOME}/examples >/dev/null 2>&1

if ${OSCARS_DEBUG=false} ; then
    echo "DEBUG IS ON: running reservation query on ${GRI}"
fi

run_oscars query -repo ${OSCARS_CERT_REPO} -url ${OSCARS_IDC} -gri ${GRI} 2> $ERRFILE

popd >/dev/null 2>&1
