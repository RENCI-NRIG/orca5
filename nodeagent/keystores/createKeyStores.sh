#!/bin/sh

# the client keystore holds client public-private key pair
# the pair is identified as clientkey (alias)
# the client store pass is clientstorepass

# the client key pass is clientkeypass
keytool -genkey -alias clientkey -keystore client.jks -storetype JKS -keyalg rsa -storepass clientstorepass -keypass clientkeypass 

# trudy key pass is trudykeypass
keytool -genkey -alias trudykey -keystore trudy.jks -storetype JKS -keyalg rsa -storepass trudystorepass -keypass trudykeypass 


# DEPRECATED -> in the new protocol the server generates its own keys and keystore and imports the first certificate.
# the client requests the service certificate and registers it programatically in his keystore.
# The following commands are only for illustrative purposes:

# import client public key into server store (client will become trusted)
# keytool -import -alias clientkey -keystore server.jks -file client.key -storepass serverstorepass

#create server private key
# keytool -genkey -alias serverkey -keystore server.jks -keyalg rsa -storepass serverstorepass -keypass serverkeypass

#export server public key into server.key
# keytool -export -alias serverkey -keystore server.jks -file server.key -storepass serverstorepass

#import server public key into client.jks
# keytool -import -alias serverkey -keystore client.jks -file server.key -storepass clientstorepass

# the public key of the client is identified as clientkey
# the server store pass is serverstorepass