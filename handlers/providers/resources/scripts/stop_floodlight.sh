#!/bin/bash 

if [ ! -z $1  ]; then
	FVVLAN=$1
fi

if [ -z "$FVVLAN" ]; then
	echo "Must specify VLAN"
	exit 1
fi


STATUS_RUNNING="--filter status=running"
STATUS_CREATED="--filter status=created"
STATUS_RESTARTING="--filter status=restarting"
STATUS_REMOVING="--filter status=removing"
STATUS_PAUSED="--filter status=paused"
STATUS_EXITED="--filter status=exited"
STATUS_DEAD="--filter status=dead"

DOCKER_CONTAINER_NAME="floodlight-${FVVLAN}"

RUNNING_CONT=$(docker ps ${STATUS_RUNNING} --format "{{.Names}}" --filter "name=${DOCKER_CONTAINER_NAME}")

if [ "${RUNNING_CONT}" == ${DOCKER_CONTAINER_NAME} ]; then
    docker stop  ${RUNNING_CONT} 
    docker rm ${RUNNING_CONT}
else
    CONT=$(docker ps --format "{{.Names}}" --filter "name=${DOCKER_CONTAINER_NAME}" ${STATUS_EXITED} ${STATUS_CREATED} ${STATUS_RESTARTING} ${STATUS_REMOVING} ${STATUS_DEAD} ${STATUS_PAUSED})
    if [ "${CONT}" == ${DOCKER_CONTAINER_NAME} ]; then
        docker rm ${CONT}
    fi
fi
 
