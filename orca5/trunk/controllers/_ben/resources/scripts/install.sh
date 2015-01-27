#!/bin/bash

if [ -z $1 ]; then
    echo "Missing driver root dir"
    exit -1
fi

DIR=$1

#chmod u+x $DIR/scripts/*.sh
chmod 0400 $DIR/keys/*
