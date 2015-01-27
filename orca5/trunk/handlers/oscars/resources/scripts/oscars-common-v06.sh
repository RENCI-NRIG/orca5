#!/bin/bash 

OSCARS_BIN=${OSCARS_HOME}/bin/oscars

if [ -z $OSCARS_HOME ]; then
    OSCARS_HOME=${ORCA_HOME}/oscars
    echo Setting OSCARS_HOME to $OSCARS_HOME
fi

if [ -z $OSCARS_IDC ]; then
    OSCARS_IDC="https://idcdev0.internet2.edu:8443/axis2/services/OSCARS/"
fi

function run_oscars()
{
if [  $# -lt 1  ]
 then
    echo "run.sh create|cancel|list [request-specific-params]"
elif [  $1 == "list"  ]
 then
	$OSCARS_BIN --command list --url ${OSCARS_IDC} --truststore ${OSCARS_TRUSTSTORE} --keystore ${OSCARS_KEYSTORE} --alias ${OSCARS_ALIAS} --keystorepass ${OSCARS_KEYSTOREPASS} 
elif [ $1 == "create"  ]
 then    
	$OSCARS_BIN --command create --url ${OSCARS_IDC} --truststore ${OSCARS_TRUSTSTORE} --keystore ${OSCARS_KEYSTORE} --alias ${OSCARS_ALIAS} --keystorepass ${OSCARS_KEYSTOREPASS} $*
elif [ $1 == "cancel"  ]
 then    
	$OSCARS_BIN --command cancel --url ${OSCARS_IDC} --truststore ${OSCARS_TRUSTSTORE} --keystore ${OSCARS_KEYSTORE} --alias ${OSCARS_ALIAS} --keystorepass ${OSCARS_KEYSTOREPASS} $*
else
    echo "Invalid operation specified. Usage: "
    echo "run.sh create|cancel|list [request-specific-params]"
fi
}
