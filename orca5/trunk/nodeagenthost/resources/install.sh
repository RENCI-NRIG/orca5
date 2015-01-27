#!/bin/bash

if  [ $# -lt 1 ]; then
    echo "Usage $0 <na home> [<na port>]"
    exit 1
fi

HERE=`pwd`
NA_HOME=$1

if [ -z $2 ]; then
    NA_PORT=8080
else
    NA_PORT=$2
fi

# get rid of state versions
rm /etc/init.d/na
# make the file
echo "#!/bin/bash" >> /etc/init.d/na
echo "export NA_HOME=${NA_HOME}" >> /etc/init.d/na
# temporarily until we make changes to PathGuesser
echo "export NM_HOME=${NA_HOME}" >> /etc/init.d/na
echo "PORT=${NA_PORT}" >> /etc/init.d/na
cat na.template >> /etc/init.d/na
# make it executable
chmod 0700 /etc/init.d/na

# make the nodeagent start at boot time
cd /etc/init.d
update-rc.d na defaults

cd $HERE

# copy the nodeagent installation
mkdir -p $NA_HOME
cp -av nodeagent/* $NA_HOME/

chmod 700 $NA_HOME/bin/*
