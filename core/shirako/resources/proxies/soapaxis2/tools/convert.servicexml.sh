#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage $0 file name"
    exit -1
fi

OUT=`mktemp /tmp/XXXXXXXXXXXXX.XXX`
cat $1 | sed 's/<[//]*serviceGroup>//g' > ${OUT}
mv $OUT $1
