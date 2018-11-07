#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage $0 file"
    exit -1
fi

OUT=`mktemp /tmp/XXXXXXXXXXX.XXX`

OUT2=`mktemp /tmp/XXXXXXXXXX.XXX`

#cat $1 | sed 's/\_messageContext\.setEnvelope\(env\)\;/\_messageContext\.setEnvelope\(env\)\;orca\.shirako\.proxies\.soapaxis2\.util\.ContextTools\.setMyAuthToken\(\_messageContext\,authToken\)\;/g' > ${OUT}

# delete empty lines
sed '/^$/d' $1 > $OUT

# add extra argument
sed '
/)$/ {
# Found one - now read in the next line
	N
# delete the "#" and the new line character, 
	s/)\n[\t ]*throws java.rmi.RemoteException/, orca.security.AuthToken authToken) throws java.rmi.RemoteException/
}' $OUT > $OUT2


sed '
/_messageContext.setEnvelope(env);/ a\
orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);' $OUT2 > $OUT

mv $OUT $1
