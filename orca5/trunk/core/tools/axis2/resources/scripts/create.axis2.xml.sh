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

    <!-- Engage the addressing module -->
    <!-- <module ref="addressing"/> -->

    <!-- Engage the security module -->
    <module ref="rampart" />

     <parameter name="OutflowSecurity">
        <action>
            <items>Signature</items>
            <user>$KEYALIAS</user>
            <!--alias of the key used to sign-->
            <passwordCallbackClass>orca.nodeagent.client.PWCallbackHandlerClient</passwordCallbackClass>
            <signaturePropFile>$PROPFILE</signaturePropFile>
            <signatureKeyIdentifier>DirectReference</signatureKeyIdentifier>
        </action>
    </parameter>


    <parameter name="InflowSecurity">
        <action>
            <items>Signature</items>
            <passwordCallbackClass>orca.nodeagent.client.PWCallbackHandlerClient</passwordCallbackClass>
            <signaturePropFile>$PROPFILE</signaturePropFile>
        </action>
    </parameter>
EOF


cat ${TEMPFILE} >> $OUTFILE
