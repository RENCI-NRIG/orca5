#!/bin/bash

# use this script to deploy external libraries needed by the driver to our
# maven repository.
# The authentication information must be stored in 
# ~/.m2/settings.xml
# Your settings.xml should look something like this:
#
#<settings xmlns="http://maven.apache.org/POM/4.0.0"
#  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
#  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
#                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
#  <servers>
#    <server>
#      <id>orca.deploy</id>
#      <username>YOUR_USER_NAME</username>
#      <password>YOUR_PASSWORD</password>
#    </server>
#  </servers>
#  <profiles>
#    <profile>
#      <id>webdav</id>
#      <activation>
#        <activeByDefault>true</activeByDefault>
#      </activation>
#      <properties>
#        <username>YOUR_USER_NAME</username>
#        <password>YOUR_PASSWORD</password>
#      </properties>
#    </profile>
#  </profiles>  
#</settings>



if [ $# -ne 5 ]; then
	echo "usage: deploy.sh groupid artifactid version packaging path_to_artifact"
	exit 1
fi	

mvn -X deploy:deploy-file \
	-DgroupId=$1 \
    -DartifactId=$2 \
    -Dversion=$3 \
    -Dpackaging=$4 \
    -Dfile=$5 \
    -DrepositoryId='orca.deploy' \
    -Durl='dav:https://geni-orca.renci.org/maven'
