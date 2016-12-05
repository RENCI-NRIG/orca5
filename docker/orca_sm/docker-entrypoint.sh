#!/bin/bash
set -e

prog="orca_sm-14080"
exec="/opt/orca/bin/orcad"
    
export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:/bin/java::")
export PATH=$JAVA_HOME/bin:$PATH

# default: run Orca AM+Broker
if [ "$1" = "$prog" ]; then
    [ -e /etc/sysconfig/$prog ] && . /etc/sysconfig/$prog

    chown -R geni-orca:nonrenci /etc/orca

    export ORCA_HOME
    export ORCA_SERVER_PORT

    #exec $exec start
    exec /opt/orca/bin/wrapper-linux-x86-64 /opt/orca/conf/wrapper.conf wrapper.syslog.ident=orcad wrapper.pidfile="$ORCA_HOME/run/orcad.pid" wrapper.logfile="$ORCA_HOME/logs/orca-stdout.log" wrapper.daemonize=FALSE
fi

# allow user to run e.g. /bin/bash
exec "$@"

