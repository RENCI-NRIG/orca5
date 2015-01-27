#!/bin/bash

# This script installs Orca web server's public key certificate into the
# global java keystore. The script requires that the environment variable
# JAVA_HOME is set. The script takes one optional argument: the password
# of the keystore. If no password is specified, the default password will
# be used.
# Usage: 
#         [sudo] ./getcertificate.sh [keystore password]
# If sudo is required, you must make sure that JAVA_HOME is defined for the root user.
# One simple way to do this is to invoke the script like this:
# sudo bash -c "export JAVA_HOME=$JAVA_HOME ; ./getcertificate.sh"
# #####################################################################

if [ -z $JAVA_HOME ]; then
	echo "Please set JAVA_HOME before invoking this script" 
	exit 1
fi


keystore=$JAVA_HOME/jre/lib/security/cacerts
if [ ! -e $keystore -a -e $JAVA_HOME/lib/security/cacerts ];
then
	keystore=$JAVA_HOME/lib/security/cacerts
fi
	
pass=$1

if [ -z $1 ]; then
	pass=changeit
fi	

if [ ! -r $keystore ]; then
	echo $keystore is not accessible
	exit 1
fi	

echo "Removing previous certificate from keystore $keystore"
$JAVA_HOME/bin/keytool -delete  -alias geni-orca -keystore $keystore -storepass $pass -noprompt

echo "Installing certificate from https://geni-orca.renci.org into $keystore"

$JAVA_HOME/bin/keytool -delete -alias geni-orca -keystore $keystore -storepass $pass -noprompt
echo |openssl s_client -connect geni-orca.renci.org:443 2>&1 | \
      sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' | \
      $JAVA_HOME/bin/keytool -import -trustcacerts -alias geni-orca -keystore $keystore -storepass $pass -noprompt

if [ $? -eq 0 ]; then 
	echo "Certificate installed successfully"
else
	echo "Certificate installation failed"
	exit 1
fi	

