#!/bin/bash

ORCA_CONFIG_DIR="/opt/orca-oracle"

DOCKER_NAME_MYSQL="orca-mysql"
DOCKER_NAME_AM_BROKER="orca-am-broker"
DOCKER_NAME_SM="orca-sm"
DOCKER_NAME_CONTROLLER="orca-controller"

DOCKER_NET_NAME="orca"

# Remove any previous docker containers of name
echo "Info: removing any previous Orca containers..."
docker rm -f ${DOCKER_NAME_CONTROLLER}
docker rm -f ${DOCKER_NAME_SM}
docker rm -f ${DOCKER_NAME_AM_BROKER}
docker rm -f ${DOCKER_NAME_MYSQL}
echo "Note: If no such containers existed, you can safely ignore this error."

# Create docker network
docker network create ${DOCKER_NET_NAME}
echo "Note: If your docker network '${DOCKER_NET_NAME}' already exists, you can safely ignore this error."

# Docker-on-Mac is a bit slower
var_sleep=10
if [[ $OSTYPE == darwin* ]]
then
  let "var_sleep *= 15"
fi

# Start Orca MySQL server
docker run -d \
           --net ${DOCKER_NET_NAME} \
           --name ${DOCKER_NAME_MYSQL} \
           --hostname orca-mysql \
           renci/orca-mysql

# check exit status from docker run, and kill script if not successful
if [ $? -ne 0 ]
then
  exit $?
fi

# Sleep
echo -n "Sleeping for ${var_sleep} to allow ${DOCKER_NAME_MYSQL} container to start ..."
sleep ${var_sleep};
echo " done."

# Start Orca AM+Broker
docker run -d \
           --net ${DOCKER_NET_NAME} \
           --name ${DOCKER_NAME_AM_BROKER} \
           --hostname orca-am-broker \
           --volume ${ORCA_CONFIG_DIR}/am+broker/config:/etc/orca/am+broker-12080/config \
           --volume ${ORCA_CONFIG_DIR}/am+broker/ndl:/etc/orca/am+broker-12080/ndl \
           renci/orca-am-broker

# check exit status from docker run, and kill script if not successful
if [ $? -ne 0 ]
then
  exit $?
fi

# Sleep
echo -n "Sleeping for ${var_sleep} to allow ${DOCKER_NAME_AM_BROKER} container to start ..."
sleep ${var_sleep};
echo " done."

# Start Orca SM
docker run -d \
           --net ${DOCKER_NET_NAME} \
           --name ${DOCKER_NAME_SM} \
           --hostname orca-sm \
           --volume ${ORCA_CONFIG_DIR}/sm/config:/etc/orca/sm-14080/config \
           renci/orca-sm

# check exit status from docker run, and kill script if not successful
if [ $? -ne 0 ]
then
  exit $?
fi

# Sleep
let "var_sleep /= 2";
echo -n "Sleeping for ${var_sleep} to allow ${DOCKER_NAME_SM} container to start ..."
sleep ${var_sleep};
echo " done."

# Start Orca Controller
docker run -d \
           --net ${DOCKER_NET_NAME} \
           --name ${DOCKER_NAME_CONTROLLER} \
           --hostname orca-controller \
           --publish 11443:11443 \
           --volume ${ORCA_CONFIG_DIR}/controller/config:/etc/orca/controller-11080/config \
           renci/orca-controller

# check exit status from docker run, and kill script if not successful
if [ $? -ne 0 ]
then
  exit $?
fi

echo "Note: You will probably need to wait 60 seconds for Orca to finish starting up."

