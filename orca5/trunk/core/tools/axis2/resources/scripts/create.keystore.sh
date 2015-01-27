#!/bin/bash

# Creates an empty keystore
# usage: create.keystore.sh <keystore path> <keystore password>

path=$1				# path to the keytore 
password=$2			# keystore password 

if [ $# -lt 2 ]; then
    echo "Usage $0 <keystore path> <keystore password>"
    exit 1
fi

if [ -e $1 ]			# test if keystore exists
	then echo "Keystore already exists"
	keytool -list -keystore $1 -storepass $2 
	if [ $? -ne 0 ]		# keystore corrupted remove it
		then echo "Keystore is corrupted"
			 echo "Remove $1"
			 rm -f $1
	else 
		echo "Keystore valid"
		echo "Using $1"
		exit 0		 
	fi
fi	


# create a dummy key and then remove it -- is there a way of generating an empty keystore ?!?
keytool -genkey -alias dummy -keystore $1 -storetype JKS -keyalg rsa -storepass $2 -keypass trudykeypass -dname "CN=dummy,OU=Here,O=HereAlso,L=Durham,S=NC,C=US"
if [ $? -ne 0 ]
	then echo "keytool command failed to create the keystore"
	exit 1
fi
	
#remove the dummy key - now we have an empty keystore :)
keytool -delete  -alias dummy -keystore $1 -storepass $2
if [ $? -ne 0 ]
	then echo "keytool command failed to creat the keystore"
	exit 1
fi

