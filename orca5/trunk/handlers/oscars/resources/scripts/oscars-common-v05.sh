#!/bin/bash 

if [ -z $OSCARS_HOME ]; then
    OSCARS_HOME=$ORCA_HOME/oscars
    echo Setting OSCARS_HOME to $OSCARS_HOME
fi

if [ -z $OSCARS_AXIS2_HOME ]; then
    OSCARS_AXIS2_HOME=$OSCARS_HOME/axis2-1.4.1
fi

if [ -z $OSCARS_IDC ]; then
    OSCARS_IDC="https://idcdev0.internet2.edu:8443/axis2/services/OSCARS/"
fi

OSCARS_API_HOME=$OSCARS_HOME/OSCARS-client-api
OSCARS_CERT_REPO=$OSCARS_API_HOME/examples/conf/axis-tomcat

# a hack path for SC11 to avoid L2 infrastructure 11/9/11 /ib
OSCARS_PATH="urn:ogf:network:domain=es.net:node=nersc-mr2:port=xe-7/1/0:link=*","urn:ogf:network:domain=es.net:node=nersc-mr2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=jgi-mr2:port=xe-0/1/0:link=xe-0/1/0.0","urn:ogf:network:domain=es.net:node=jgi-mr2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=lbl-mr2:port=xe-8/0/0:link=xe-8/0/0.0","urn:ogf:network:domain=es.net:node=lbl-mr2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=slac-mr2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=slac-mr2:port=xe-2/0/0:link=xe-2/0/0.0","urn:ogf:network:domain=es.net:node=sunn-sdn2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=sunn-sdn2:port=xe-2/2/0:link=xe-2/2/0.0","urn:ogf:network:domain=es.net:node=sunn-cr1:port=ge-1/0/0:link=ge-1/0/0.0","urn:ogf:network:domain=es.net:node=sunn-cr1:port=xe-0/1/0:link=xe-0/1/0.0","urn:ogf:network:domain=es.net:node=denv-cr2:port=xe-1/1/0:link=xe-1/1/0.0","urn:ogf:network:domain=es.net:node=denv-cr2:port=xe-0/1/0:link=xe-0/1/0.0","urn:ogf:network:domain=es.net:node=kans-cr1:port=xe-1/1/0:link=xe-1/1/0.0","urn:ogf:network:domain=es.net:node=kans-cr1:port=xe-0/1/0:link=xe-0/1/0.0","urn:ogf:network:domain=es.net:node=chic-cr1:port=xe-3/0/0:link=xe-3/0/0.0","urn:ogf:network:domain=es.net:node=chic-cr1:port=xe-3/1/0:link=xe-3/1/0.0","urn:ogf:network:domain=es.net:node=star-cr1:port=xe-1/3/0:link=xe-1/3/0.0","urn:ogf:network:domain=es.net:node=star-cr1:port=xe-0/0/0:link=xe-0/0/0.0","urn:ogf:network:domain=es.net:node=star-sdn1:port=xe-0/0/0:link=xe-0/0/0.0","urn:ogf:network:domain=es.net:node=star-sdn1:port=xe-2/0/0:link=*"

function build_oscars_classpath()
{
    OSCARS_CLASSPATH="."
    for f in "$OSCARS_AXIS2_HOME"/lib/*.jar
    do
	OSCARS_CLASSPATH="$OSCARS_CLASSPATH":$f
    done
    for f in $OSCARS_API_HOME/lib/*.jar
    do
	OSCARS_CLASSPATH="$OSCARS_CLASSPATH":$f
    done
    export OSCARS_CLASSPATH="$OSCARS_CLASSPATH":$OSCARS_API_HOME/OSCARS-client-api.jar:$OSCARS_API_HOME/examples/OSCARS-client-examples.jar
}

function run_oscars()
{
if [  $# -lt 1  ]
 then
    echo "run.sh createReservation|signal|list|query|cancel|subscribe|renew|pause|ressume|unsubscribe|regpublisher|destroyreg|notifylistener [request-specific-params]"
elif [ $1 == "createReservation" ] && [ "$2" == "-pf" ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true CreateReservationClient $*
elif [  $1 == "createReservation"  ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true CreateReservationCLI $*
elif [ $1 == "signal"  ]
 then    
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true SignalClient repo $url $3 $4 $5 $6
elif [ $1 == "query"  ]
 then    
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true QueryReservationCLI $*
elif [ $1 == "list"  ]
 then    
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true ListReservationCLI $*
elif [ $1 == "cancel"  ]
 then    
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true CancelReservationCLI $*
elif [ $1 == "subscribe"  ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true SubscribeClient $*
elif [ $1 == "renew"  ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true RenewClient $*
elif [ $1 == "pause"  ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true PauseSubscriptionClient $*
elif [ $1 == "resume"  ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true ResumeSubscriptionClient $*
elif [ $1 == "unsubscribe"  ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true UnsubscribeClient $*
elif [ $1 == "regpublisher"  ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true RegisterPublisherClient $*
elif [ $1 == "destroyreg"  ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true DestroyRegistrationClient $*
elif [ $1 == "notifylistener"  ]
 then
    java -cp $OSCARS_CLASSPATH -Djava.net.preferIPv4Stack=true -Djavax.net.ssl.keyStore=$OSCARS_CERT_REPO/OSCARS.jks -Djavax.net.ssl.keyStorePassword=password NotifyEchoHandler $*
else
    echo "Invalid operation specified. Usage: "
    echo "run.sh createReservation|signal|list|query|cancel|subscribe|renew|pause|ressume|unsubscribe|regpublisher|dstroyreg|notifylistener [request-specific-params]"
fi
}
