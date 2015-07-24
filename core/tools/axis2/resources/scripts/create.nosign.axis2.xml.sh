#!/bin/bash

#######################################################################
# Creates an axis2.xml configuration personalized for a given actor


if [ $# -ne 4 ]; then
    echo "Usage $0 <template file> <output file> <key alias> <properties file>"
    exit 1
fi

TEMPFILE=$1
OUTFILE=$2
KEYALIAS=$3
PROPFILE=$4


if [ $# -lt 4 ]; then
    echo "Usage $0 <template file> <output file> <keyalias> <properties file>"
    exit 1
fi

cat > $OUTFILE <<EOF
<axisconfig name="AxisJava2.0">

EOF


cat ${TEMPFILE} >> $OUTFILE
