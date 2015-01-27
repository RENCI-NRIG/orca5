#!/bin/bash

# This script is responsible for preparing the source tree for usage.


function die() {
	echo $1
	exit 1
}


echo "Generating admin security configuration"
cd tools/config
ant security.create.admin.config || die "failed to create admin security configuration"
cd -
echo "Admin security configuration created successfully"

echo "Adding a link to runtime directory in the relevant projects"

cd tools/cmdline
ln -s -f ../config/runtime .
cd -

cd webapp
ln -s -f ../tools/config/runtime .
cd -

cd core/shirako
ln -s -f ../../tools/config/runtime .
cd -

cd core/policy
ln -s -f ../../tools/config/runtime .
cd -

echo "Finished linking runtime directory"

echo "Preparing cmdline project"
cd tools/cmdline
ant get.packages
cd -

echo "Finished preparing cmdline project"

#echo "Preparing core for unit tests"
#cd core/shirako
#mvn -Pant -Dtarget=copy.local
#cd -

#cd core/policy
#ln -s -f ../shirako/local .
#cd -

#cd core/cod
#ln -s -f ../shirako/local .
#cd -

echo "Finished preparing core for unit tests"

echo "Basic tree preparation complete"
exit 0


