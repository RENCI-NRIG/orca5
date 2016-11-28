#!/bin/bash
set -e

prog="orca_controller-11080"
exec="/opt/orca-controller/bin/xmlrpcd"
    
# default: run Orca AM+Broker
if [ "$1" = "$prog" ]; then
    [ -e /etc/sysconfig/$prog ] && . /etc/sysconfig/$prog

    chown -R geni-orca:nonrenci /etc/orca

    export ORCA_CONTROLLER_HOME

    #exec $exec start
    exec /opt/orca-controller/bin/./wrapper-linux-x86-64 /opt/orca-controller/conf/wrapper.conf wrapper.syslog.ident=xmlrpcd wrapper.pidfile="$ORCA_CONTROLLER_HOME/run/xmlrpcd.pid" wrapper.logfile="$ORCA_CONTROLLER_HOME/logs/controller-stdout.log" wrapper.daemonize=FALSE
fi

# allow user to run e.g. /bin/bash
exec "$@"

