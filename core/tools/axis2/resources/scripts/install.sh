#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage $0 path"
    exit 1
fi

dir=$1
chmod u+x ${dir}/*.sh
