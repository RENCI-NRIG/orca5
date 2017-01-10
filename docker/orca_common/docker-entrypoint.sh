#!/bin/bash
set -e

prog="orca"
#exec="/opt/orca/bin/orcad"

export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:/bin/java::")
export PATH=$JAVA_HOME/bin:$PATH
    
# default: run Orca
if [ "$1" = "$prog" ] || [ "$1" = "debug" ]; then
    [ -e /etc/sysconfig/$prog ] && . /etc/sysconfig/$prog

    export ORCA_HOME
    export ORCA_SERVER_PORT

    chown -R geni-orca:nonrenci $ORCA_HOME

    DEBUG_OPTIONS=""
    # enable JMX profiling in DEBUG mode
    if [ "$1" = "debug" ] || [ "$2" = "debug" ]; then
        DEBUG_OPTIONS=" wrapper.debug=TRUE \"#include.debug=TRUE\""
        cp /tmp/wrapper-overrides.conf /opt/orca/conf/wrapper-overrides.conf
    fi

    #exec $exec start
    exec /opt/orca/bin/wrapper-linux-x86-64 /opt/orca/conf/wrapper.conf wrapper.syslog.ident=orcad wrapper.pidfile="$ORCA_HOME/run/orcad.pid" wrapper.logfile="$ORCA_HOME/logs/orca-stdout.log" wrapper.daemonize=FALSE $DEBUG_OPTIONS
fi

# allow user to run e.g. /bin/bash
exec "$@"

