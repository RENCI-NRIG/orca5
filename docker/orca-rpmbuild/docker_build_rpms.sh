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
mvn clean package -Pdocker

# Remove any previous docker containers of name
DOCKER_NAME_RPMBUILD="orca-rpmbuild"
f_rm_f_docker_container ${DOCKER_NAME_RPMBUILD}

# Run rpmbuild inside container
docker run --volume ~/.m2/:/root/.m2/ --name ${DOCKER_NAME_RPMBUILD} renci/orca-rpmbuild

# Copy built RPMs out of container, into local storage
echo -n "Copying RPMs to ${RPM_BUILD_DIR} ... "
docker cp orca-rpmbuild:/root/orca-build/rpmbuild/RPMS/ "${RPM_BUILD_DIR}"
echo "done."

