#!/bin/bash
set -e

# Setup Java correctly
export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:/bin/java::")
export PATH=$JAVA_HOME/bin:$PATH

# Setup Java to conform to buildrpm.sh script
$(mkdir -p /usr/java)
$(ln -sf $JAVA_HOME /usr/java/latest)

# if command starts with an option, prepend our default command
if [ "${1:0:1}" = '-' ]; then
	set -- "/root/git/orca5/redhat/buildrpm.sh" "$@"
fi

exec "$@"

