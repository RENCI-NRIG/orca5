#!/bin/bash

TARGET=target/deb
VERSION=`svn info | grep Revision | awk '{print $2}'`

chmod 755 $TARGET/DEBIAN/p*
chmod 644 $TARGET/DEBIAN/c*
chmod u+x $TARGET/opt/orca/nodeagent/bin/*

mkdir $TARGET/opt/orca/nodeagent/temp
mv $TARGET/opt/orca/nodeagent/data/drivers.xml $TARGET/opt/orca/nodeagent/temp

perl -i -pne "s/<VERSION>/$VERSION/g ;
s/<SIZE>/`du -ks $TARGET | cut -f1`/" $TARGET/DEBIAN/control

chmod go-w -R $TARGET
find $TARGET -type f | xargs md5sum > $TARGET/DEBIAN/md5sums | sed -e "s,$TARGET/,,"
CMD="dpkg --build $TARGET target/orca-nodeagent.deb"
fakeroot bash -c "chown root.root -R $TARGET ; $CMD"

