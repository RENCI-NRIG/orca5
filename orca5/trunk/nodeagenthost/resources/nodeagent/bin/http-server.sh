#!/bin/bash

DEBUG_PORT=8000
# This script is responsible for starting the node agent service.
# Note: it must be run from ${NA_HOME}

export AXIS2_HOME=${NA_HOME}

# ADD the lib directory to the classpath
AXIS2_CLASSPATH=$AXIS2_CLASSPATH:${AXIS2_HOME}/lib

# Add all jars from lib to the classpath
for f in $AXIS2_HOME/lib/*.jar
do
  AXIS2_CLASSPATH=$AXIS2_CLASSPATH:$f
done

# Add all properties files to the classpath
#for f in $AXIS2_HOME/lib/*.properties
#do
#  AXIS2_CLASSPATH=$AXIS2_CLASSPATH:$f
#done

# Important: Add all service archives to the classpath.
# The Simple Web Server we use does not handle correcly
# classpaths when we use WS-Security. We must add the services
# to the classpath to ensure that resources can be resolved.
# Unfortunately, resources requiring jars inside the service archive
# cannot be resolved like this: the jar files must be placed in $AXIS2_HOME/lib
for f in $AXIS2_HOME/repository/services/*.aar
do
  AXIS2_CLASSPATH=$AXIS2_CLASSPATH:$f
done

export AXIS2_CLASSPATH

#echo "classpath $AXIS2_CLASSPATH"

# Uncomment this line to turn on debugging. Note that a port scan on the debug port will kill the
# JVM. This is a known bug in Java 1.5.
DEBUG_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${DEBUG_PORT}"

# start the server and deploy all services


launch_daemon()
{
  /bin/bash <<EOF
     java ${DEBUG_OPTIONS} -Ddaemon.pidfile=${ORCA_AXIS2_TOOLS_PID_FILE} -classpath ${AXIS2_CLASSPATH} orca.nodeagenthost.Server ${a} </dev/null 1>& log/start.log &
     pid=\$!
     echo \${pid} > ${NA_HOME}/bin/tmppid
EOF
}

a=$*

launch_daemon
daemon_pid=`cat ${NA_HOME}/bin/tmppid`

if ps -p "${daemon_pid}" >/dev/null 2>&1
then
  # daemon is running.
  echo ${daemon_pid} > ${ORCA_AXIS2_TOOLS_PID_FILE}
else
  #echo "Daemon did not start."
  exit -1
fi

exit 0
