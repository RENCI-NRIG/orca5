#!/bin/bash

# Define top-level build directory
export ORCA_BLD="${HOME}/orca-build"

# Define RPM build directory
RPM_BUILD_DIR="${ORCA_BLD}/rpmbuild"

# remove stopped or running containers
f_rm_f_docker_container ()
{
  #container_name="$1"
  RUNNING=$(docker inspect --format="{{ .State.Running }}" $1 2> /dev/null)

  # if it's not there at all, we don't need to remove it
  if [ $? -ne 1 ]; then
    echo -n "Removing container: "
    docker rm -f $1

    # check exit status, and kill script if not successful
    if [ $? -ne 0 ]
    then
      exit $?
    fi
  fi
}

# Delete local RPMs
echo -n "Deleting ${RPM_BUILD_DIR} at user request..."
rm -rf "${RPM_BUILD_DIR}"
echo "done."

# Create RPM build directories
mkdir -p "${RPM_BUILD_DIR}/RPMS"

# start in the right directory
cd "$( dirname "$0" )"

# Build new docker container, that contains current sources
mvn clean package -Pdocker || exit $?

# Remove any previous docker containers of name
DOCKER_NAME_RPMBUILD="orca-rpmbuild"
f_rm_f_docker_container ${DOCKER_NAME_RPMBUILD}

# Run rpmbuild inside container
# we should fix the user/permission issue before we mount ~/.m2/ inside the container
docker run --name ${DOCKER_NAME_RPMBUILD} renci/orca-rpmbuild

# check exit status, and kill script if not successful
if [ $? -ne 0 ]
then
  exit $?
fi

# Copy built RPMs out of container, into local storage
echo -n "Copying RPMs to ${RPM_BUILD_DIR} ... "
docker cp orca-rpmbuild:/root/orca-build/rpmbuild/RPMS/ "${RPM_BUILD_DIR}"

# save the return value
var_ret=$?

# check exit status, and kill script if not successful
if [ $var_ret -ne 0 ]
then
  echo "failed."
  exit $var_ret
fi

echo "done."

# check for existence of files in RPM dir
if [ "$(ls -a ${RPM_BUILD_DIR})" ]; then
  echo "Directory not empty. Good!"
else
  echo "ERROR: No RPM files found in ${RPM_BUILD_DIR}."
  exit 1
fi

# Copy Orca maven repository out of container, into local storage
# could potentially use mvn to get local repo dir, but it's not made for programmatic access:
# mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]' | grep -v '\[WARNING\]'
echo -n "Copying Orca artifacts in container Maven repository to ~/.m2/ ... "
docker cp orca-rpmbuild:/root/orca-build/repository/orca/ ~/.m2/repository/ || exit $?
echo "done."

