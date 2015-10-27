#!/bin/bash

# Set up tools
export JAVA_HOME=/usr/java/latest
export PATH=/opt/jdev/maven/bin:$PATH

# Define top-level build directory
export ORCA_BLD="${HOME}/orca-build"

# Define version of ORCA being built
export VERSION="5.0.0"

# Define ORCA source directory
BASE_SRC_DIR="orca-iaas-${VERSION}"
BASE_SRC_DIRPATH="${ORCA_BLD}/${BASE_SRC_DIR}"

# Define RPM build directory
RPM_BUILD_DIR="${ORCA_BLD}/rpmbuild"

# Define Maven artifact repository
MVN_REPO="${ORCA_BLD}/repository"

# Define usage function
usage() { echo "Usage: $0 [-i] [-c] [-f]" 1>&2; exit 1; }

# Process args
while getopts ":icf" opt; do
    case "${opt}" in
        i)
            echo "User requested automatic installation after build complete."
            INSTALL_RPM="1"
            ;;
        c)
            echo -n "Deleting ${RPM_BUILD_DIR} at user request..."
            rm -rf "${RPM_BUILD_DIR}"
            echo "done."
            ;;
        f)
            echo -n "Deleting ${MVN_REPO} at user request..."
            rm -rf "${MVN_REPO}"
            echo "done."
            ;;
        *)
            usage
            ;;
    esac
done
shift $((OPTIND-1))

# Let the user know we're about to proceed
echo "Preparing to build RPMs; this may take some time..."

# Create RPM build directories
mkdir -p "${RPM_BUILD_DIR}"
mkdir -p "${RPM_BUILD_DIR}/BUILD"
mkdir -p "${RPM_BUILD_DIR}/RPMS"
mkdir -p "${RPM_BUILD_DIR}/RPMS/x86_64"
mkdir -p "${RPM_BUILD_DIR}/RPMS/noarch"
mkdir -p "${RPM_BUILD_DIR}/SOURCES"
mkdir -p "${RPM_BUILD_DIR}/SPECS"
mkdir -p "${RPM_BUILD_DIR}/SRPMS"
mkdir -p "${RPM_BUILD_DIR}/tmp"

# Create Maven settings.xml
cat << EOF > "${ORCA_BLD}/settings.xml"
<settings xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <mirrorOf>*,!geni-imf-libs,!geni-orca-libs,!geni-orca-snapshot</mirrorOf>
      <name>remote-repos</name>
        <url>http://ci-dev.renci.org/nexus/content/groups/public/</url>
      <id>remote-repos</id>
    </mirror>
  </mirrors>
  <localRepository>${MVN_REPO}</localRepository>
</settings>
EOF

# Clean and create source directory
rm -rf "${BASE_SRC_DIRPATH}"
mkdir -p "${BASE_SRC_DIRPATH}"

# Copy the source to it
cd "$( dirname "$0" )"
cp -a ../. "${BASE_SRC_DIRPATH}"

# Post-process source
export BLD_DATE=`date "+%Y%m%d%H%M"`
export COMMIT=`git rev-parse HEAD`
export SHORTCOMMIT=`git rev-parse --short=8 HEAD`
sed -i -e "s;@@DATE@@;${BLD_DATE};" "${BASE_SRC_DIRPATH}/redhat/orca-iaas.spec"
sed -i -e "s;@@COMMIT@@;${COMMIT};" "${BASE_SRC_DIRPATH}/redhat/orca-iaas.spec"
sed -i -e "s;@@SHORTCOMMIT@@;${SHORTCOMMIT};" "${BASE_SRC_DIRPATH}/redhat/orca-iaas.spec"

# Change directory to build location
cd "${ORCA_BLD}"

# Create tarball
rm -rf ${BASE_SRC_DIR}-*
mv ${BASE_SRC_DIR} ${BASE_SRC_DIR}-${SHORTCOMMIT}
tar -czf ${BASE_SRC_DIR}-${SHORTCOMMIT}.tar.gz ${BASE_SRC_DIR}-${SHORTCOMMIT}

# Place some command-line arguments for Maven in an environment variable,
# and export.
export MAVEN_ARGS="-s ${ORCA_BLD}/settings.xml"

# Build RPM from tarball
rpmbuild --define "_topdir ${RPM_BUILD_DIR}" --define '_tmppath %{_topdir}/tmp' --define '%packager RENCI/ExoGENI <exogeni-ops@renci.org>' -ta ${BASE_SRC_DIR}-${SHORTCOMMIT}.tar.gz

BLD_STATUS=$?
if [ $BLD_STATUS -ne 0 ]; then
    echo "RPM build failed."
    exit $BLD_STATUS
fi

if [ -n "${INSTALL_RPM}" ]; then
    echo "Preparing to install RPMs..."
    cd "${RPM_BUILD_DIR}/RPMS/x86_64"
    sudo rpm -Uvh --force "*${BLD_DATE}git${SHORTCOMMIT}*.rpm"
fi

echo "Done."
exit 0
