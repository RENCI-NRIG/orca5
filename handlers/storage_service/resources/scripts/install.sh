#!/bin/bash                                                                                                                                                                                    

if [ -z $1 ]; then
    echo "Missing package root dir"
    exit 1
fi

DIR=$1

chmod -R u+x $DIR/scripts

