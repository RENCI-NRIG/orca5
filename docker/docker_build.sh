#!/bin/bash

DOCKER_MVN_CMD="mvn clean package -Pdocker"

f_mvn_build_dir ()
{
  var_dir="$1"
  pushd "$var_dir"
  eval "$DOCKER_MVN_CMD"

  # save the return value from mvn
  var_ret=$?
  popd

  # check exit status from mvn, and kill script if not successful
  if [ $var_ret -ne 0 ]
  then
    exit $var_ret
  fi
}
  

# Build orca_base container
f_mvn_build_dir "orca_base"

# Build rpmbuild docker container and RPMs
pushd orca-rpmbuild
./docker_build_rpms.sh

# save the return value from rpmbuild
var_ret=$?
popd

# check exit status from rpmbuild, and kill script if not successful
if [ $var_ret -ne 0 ]
then
  exit $var_ret
fi

# Build the rest of the orca containers
f_mvn_build_dir "orca_mysql"
f_mvn_build_dir "orca_common"
f_mvn_build_dir "orca_am_broker"
f_mvn_build_dir "orca_sm"
f_mvn_build_dir "orca_controller"

