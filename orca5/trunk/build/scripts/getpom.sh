#!/bin/bash

# Use this file to download and install into your local maven repository
# a specific version of the orca top-level pom file. This step is
# necessary to bootstrap the build process of any dependent project.
# Example usage:
#  To retrieve version 1.0 of the POM file and install it in your local
#  maven repostiory type the following:
#     ./getpom.sh 1.0

function die() {
	echo $1
	exit 1
}

if [ $# -ne 1 ]; then
	echo "Usage: getpom.sh pom-version"
	exit 1
fi	

which wget > /dev/null
if  [ $? -ne 0 ]; then
	echo "This script requires wget. Please install wget and try again"
	exit 1
fi	

version=$1
dir=~/.m2/repository/orca/orca/$version

if [ -e $dir/orca-$version.pom ]; then
	echo "Pom file already exists: $dir/orca-${version}.pom"
	exit 0
fi

echo "Creating directory: $dir"
mkdir -p $dir || die "could not create local repository directory: $dir"
pushd . > /dev/null
cd $dir
echo "Fetching pom file from Orca's maven repository"
wget https://geni-orca.renci.org/maven/orca/orca/$version/orca-$version.pom 
if [ $? -ne 0 ]; then
	echo -e "An error occurred while fetching POM file.\n \
	Please make sure you are connected to the Internet and that you
	specified a valid POM version.\n \
	The URL of the POM file we tried to download is: https://geni-orca.renci.org/maven/orca/orca/$version/orca-$version.pom" 
	popd > /dev/null
	exit 1
else
	echo "Pom file downloaded successfully"
	popd > /dev/null
fi	
