#!/bin/bash

function die() {
	echo $1
	exit 1
}


if [ $# -ne 2 ]; then
	die "Usage: workspace tools_location"
fi

DIR=$1
TOOLS=$2

# drop and load the test database

echo Initializing test database
mysql -u test < $DIR/schema/mysql/test.schema.sql || die "failed to load test db schema"
mysql -u test < $DIR/schema/mysql/test.data.sql || die "failed to populate test db schema"

RUNTIME=$TOOLS/runtime

echo "Linking runtime directory"
cd $DIR/shirako
rm runtime
ln -s -f $RUNTIME . || die "failed to link runtime directory"

cd $DIR/cod
rm runtime
ln -s -f $RUNTIME . || die "failed to link runtime directory" 

cd $DIR/policy
rm runtime
ln -s -f $RUNTIME . || die "failed to link runtime directory" 

cd $DIR/manage
rm runtime
ln -s -f $RUNTIME . || die "failed to link runtime directory" 


echo "Finished linking runtime directory"


echo "Preparing core for unit tests"
cd $DIR/shirako
rm -rf local
cp -r config local || die "failed preparing core for unit tests" 

cd $DIR/policy
rm -rf local
ln -s -f ../shirako/local . || die "failed preparing core for unit tests" 

cd $DIR/cod
rm -rf local
ln -s -f ../shirako/local . || die "failed preparing core for unit tests" 

cd $DIR/manage
rm -rf local
ln -s -f ../shirako/local . || die "failed preparing core for unit tests" 

echo "Finished preparing core for unit tests"

echo "Pre-build script completed successfully"
exit 0
