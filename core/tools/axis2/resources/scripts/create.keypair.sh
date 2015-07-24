#!/bin/bash

# Creates an rsa keypair

path=$1				# path to the keystore 
password=$2			# keystore password 
keyalias=$3			# key alias to store in keystore
keypass=$4			# key password
dname=$5			# Distinguished Name for certificate creation

term=3650			# almost 10 years
keysize=1024		# 1K key size: somewhat small but fast to generate

if [ $# -lt 5 ]
    then echo "Usage $0 <keystore path> <keystore password> <key alias> <key password> <Certificate Distignuished Name>"
    exit 1
fi

if [ -e $path ]			# test if keystore exists
		then 
			keytool -list -keystore $path -storepass $password
			if [ $? -ne 0 ]		# keystore corrupted 
				then echo "Keystore corrupted"
				exit 1		 
			fi
		else
			echo "No such file $path"
			exit 1
fi	

# check if there is already a key with keyalias
keytool -list -alias $keyalias -keystore $path -storepass $password 
if [ $?  -eq  0 ]
	then echo "$keyalias is already registered with $path"
	echo "Use $keyalias as default"
	exit 0
fi
 

# create key
keytool -genkey -validity $term -alias $keyalias -keystore $path -storetype JKS -keyalg rsa -keysize $keysize -storepass $password -keypass $keypass -dname $dname
if [ $? -ne 0 ]
	then echo "keytool command failed to creat the key"
	exit 1
fi

