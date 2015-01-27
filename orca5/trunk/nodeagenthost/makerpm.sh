#!/bin/bash

TARGET=`pwd`/target/rpm
VERSION=`svn info | grep Revision | awk '{print $2}'`

rm -rf $TARGET/noarch
(perl -pne "s/<VERSION>/$VERSION/ ;
s,<RMPBUILDROOT>,$TARGET, " pkg/rpm/nodeagent.spec) > nodeagent.spec

#for f in `cd $TARGET; find .`; do
#	f=$(echo $f | sed 's/^\.//')
#	if [ ! -z $f ]; then
#		echo "%{_prefix}$f" >> nodeagent.spec
#	fi
#done

# move drivers.xml to the temp directory
# so that we do not overwrite driver.xml if it exists on the installed machine
mkdir target/rpm/opt/orca/nodeagent/temp
mv target/rpm/opt/orca/nodeagent/data/drivers.xml target/rpm/opt/orca/nodeagent/temp

chmod go-w -R $TARGET
rpmbuild -bb nodeagent.spec
mv target/rpm/noarch/orca-nodeagent-${VERSION}-1.noarch.rpm target/orca-nodeagent.rpm


