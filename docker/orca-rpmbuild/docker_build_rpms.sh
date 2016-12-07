#!/bin/bash

# Define top-level build directory
export ORCA_BLD="${HOME}/orca-build"

# Define RPM build directory
RPM_BUILD_DIR="${ORCA_BLD}/rpmbuild"

# Delete local RPMs
echo -n "Deleting ${RPM_BUILD_DIR} at user request..."
rm -rf "${RPM_BUILD_DIR}"
echo "done."

# Create RPM build directories
mkdir -p "${RPM_BUILD_DIR}/RPMS"

# Build new docker container, that contains current sources
mvn clean package -Pdocker

# Remove any previous docker containers of name
DOCKER_NAME_RPMBUILD="orca-rpmbuild"
docker rm -f ${DOCKER_NAME_RPMBUILD}

# Run rpmbuild inside container
docker run --volume ~/.m2/:/root/.m2/ --name ${DOCKER_NAME_RPMBUILD} renci/orca-rpmbuild

# Copy built RPMs out of container, into local storage
echo -n "Copying RPMs to ${RPM_BUILD_DIR} ... "
docker cp orca-rpmbuild:/root/orca-build/rpmbuild/RPMS/ "${RPM_BUILD_DIR}"
echo "done."

