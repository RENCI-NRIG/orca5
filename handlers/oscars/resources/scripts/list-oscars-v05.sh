#!/bin/bash 

# Query reservation for a ION/OSCARS circuit
# parameters are: 
# OSCARS_SCRIPTS_DIR (from static build properties based on package)
# OSCARS_HOME and OSCARS_IDC (from static properties in oscars.site.properties)

# prep
if [ -z ${OSCARS_SCRIPTS_DIR} ]; then
    echo "OSCARS package scripts directory undefined, cannot proceed"
    exit 1
fi

. ${OSCARS_SCRIPTS_DIR}/oscars-common.bash

# for OSCARS java tools
build_oscars_classpath

ERRFILE="/tmp/oscars.$$.tmp"
trap 'rm -f $ERRFILE' EXIT

pushd ${OSCARS_API_HOME}/examples >/dev/null 2>&1

if ${OSCARS_DEBUG=false} ; then
    echo "DEBUG IS ON: running reservation list"
fi

run_oscars list -repo ${OSCARS_CERT_REPO} -url ${OSCARS_IDC} 2> $ERRFILE

popd >/dev/null 2>&1

