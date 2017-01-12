#!/bin/bash

ORCA_CONFIG_DIR="/opt/orca-oracle"

DOCKER_NAME_MYSQL="orca-mysql"
DOCKER_NAME_AM_BROKER="orca-am-broker"
DOCKER_NAME_SM="orca-sm"
DOCKER_NAME_CONTROLLER="orca-controller"

DOCKER_NET_NAME="orca"

# remove stopped or running containers
f_rm_f_docker_container ()
{
  #container_name="$1"
  RUNNING=$(docker inspect --format="{{ .State.Running }}" $1 2> /dev/null)

  # if it's not there at all, we don't need to remove it
  if [ $? -ne 1 ]; then
    echo -n "Removing $1: "
    docker rm -f $1

    # check exit status, and kill script if not successful
    if [ $? -ne 0 ]
    then
      exit $?
    fi
  fi
}

# Remove any previous docker containers of name
echo "Info: removing any previous Orca containers..."
f_rm_f_docker_container ${DOCKER_NAME_CONTROLLER}
f_rm_f_docker_container ${DOCKER_NAME_SM}
f_rm_f_docker_container ${DOCKER_NAME_AM_BROKER}

# Create docker network
NET_INSPECT=$(docker network inspect ${DOCKER_NET_NAME} 2> /dev/null)
# only create it if it doesn't already exist
if [ $? -eq 1 ]; then
  echo -n "Creating docker network ${DOCKER_NET_NAME}: "
  docker network create ${DOCKER_NET_NAME}
else
  echo "Info: Docker network '${DOCKER_NET_NAME}' already exists."
fi

# Docker-on-Mac is a bit slower
var_sleep=10
if [[ $OSTYPE == darwin* ]]
then
  let "var_sleep *= 15"
fi

# The MySQL container probably doesn't need to be restarted
RUNNING=$(docker inspect --format="{{ .State.Running }}" $DOCKER_NAME_MYSQL 2> /dev/null)

if [ $? -eq 1 ] || [ "$RUNNING" == "false" ]; then
  if [ "$RUNNING" == "false" ]; then
    f_rm_f_docker_container ${DOCKER_NAME_MYSQL}
  fi

  # Start Orca MySQL server
  echo -n "docker run ${DOCKER_NAME_MYSQL}: "
  docker run -d \
             --net ${DOCKER_NET_NAME} \
             --name ${DOCKER_NAME_MYSQL} \
             --hostname orca-mysql \
             --publish 3306:3306\
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
else
  echo "Container '${DOCKER_NAME_MYSQL}' is running; not restarting."
fi

# Start Orca AM+Broker
echo -n "docker run ${DOCKER_NAME_AM_BROKER}: "
docker run -d \
           --net ${DOCKER_NET_NAME} \
           --name ${DOCKER_NAME_AM_BROKER} \
           --hostname orca-am-broker \
           --publish 9010:9010 \
           --volume ${ORCA_CONFIG_DIR}/am+broker/config:/etc/orca/am+broker-12080/config \
           --volume ${ORCA_CONFIG_DIR}/am+broker/ndl:/etc/orca/am+broker-12080/ndl \
           renci/orca-am-broker \
           debug # DEBUG mode, for JMX remote monitoring

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
echo -n "docker run ${DOCKER_NAME_SM}: "
docker run -d \
           --net ${DOCKER_NET_NAME} \
           --name ${DOCKER_NAME_SM} \
           --hostname orca-sm \
           --publish 9011:9010 \
           --volume ${ORCA_CONFIG_DIR}/sm/config:/etc/orca/sm-14080/config \
           renci/orca-sm \
           debug # DEBUG mode, for JMX remote monitoring

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
echo -n "docker run ${DOCKER_NAME_CONTROLLER}: "
docker run -d \
           --net ${DOCKER_NET_NAME} \
           --name ${DOCKER_NAME_CONTROLLER} \
           --hostname orca-controller \
           --publish 11443:11443 \
           --publish 9012:9010 \
           --volume ${ORCA_CONFIG_DIR}/controller/config:/etc/orca/controller-11080/config \
           renci/orca-controller \
           debug # DEBUG mode, for JMX remote monitoring

# check exit status from docker run, and kill script if not successful
if [ $? -ne 0 ]
then
  exit $?
fi

echo "Note: You will probably need to wait 60 seconds for Orca to finish starting up."

