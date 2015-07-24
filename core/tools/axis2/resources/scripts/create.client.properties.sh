#!/bin/bash

if [ $# -ne 3 ]; then
    echo "Usage $0 <properties file location> <keystore> <keystore pass>"
    exit 1
fi

path=$1
keystore=$2
keystorepass=$3

echo "org.apache.ws.security.crypto.provider=org.apache.ws.security.components.crypto.Merlin" > $path
CODE=$?
if [ $CODE -ne 0 ]; then
    exit $CODE
fi

echo "org.apache.ws.security.crypto.merlin.keystore.type=jks" >> $path
CODE=$?
if [ $CODE -ne 0 ]; then
    exit $CODE
fi

echo "org.apache.ws.security.crypto.merlin.keystore.password=${keystorepass}" >> $path
CODE=$?
if [ $CODE -ne 0 ]; then
    exit $CODE
fi

echo "org.apache.ws.security.crypto.merlin.file=${keystore}" >> $path
CODE=$?

exit $CODE
