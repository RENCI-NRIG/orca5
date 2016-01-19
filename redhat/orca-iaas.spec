%global commit @@COMMIT@@
%global shortcommit @@SHORTCOMMIT@@

Summary: ORCA - An infrastructure-as-a-service control framework
Name: orca-iaas
Version: 5.0.0
# NOTE:
# DO NOT MODIFY "Release", UNLESS:
# 1) We move to a revision structure that isn't based on the subversion GlobalRev.
# 2) Packaging up a tarball for others to use with "rpmbuild -ta"
Release: @@DATE@@git%{shortcommit}
#
BuildRoot: %{_builddir}/%{name}-root
Source: https://github.com/RENCI-NRIG/orca5/archive/%{commit}/%{name}-%{version}-%{shortcommit}.tar.gz
Group: Applications/System
Vendor: RENCI/ExoGENI
Packager: RENCI/ExoGENI
License: Eclipse Public License
URL: https://github.com/RENCI-NRIG/orca5.git

# NOTE:
# Maven is required for build, but there's not a RHEL package for it.
# We presume it's in the environment.
# For a "real" SRPM/source tarball, we'll need to have a BuildRequire for Maven,
# probably from the JPackage repo.
# Also - we require the Oracle "jdk", rather than using the more standard "java" for a Require.
#BuildRequires:  jdk >= 1.7.0 ant
BuildRequires:  jdk >= 1.7.0
Requires:       jdk >= 1.7.0
Requires:       orca-iaas-common = %{version}-%{release}

# Set up some useful definitions...
%define daemon_common_dir /opt/orca
%define controller_common_dir /opt/orca-controller
%define conf_dir %{_sysconfdir}/orca
%define log_dir %{_localstatedir}/log/orca
%define pid_dir %{_localstatedir}/run/orca
%define maven_opts "-XX:MaxPermSize=512m -Xms40m -Xmx1024m"
%define ant_opts "-XX:MaxPermSize=512m -Xms40m -Xmx1024m"

# NOTE:
# The definitions below are ExoGENI specific.
# We might want something a bit more "generic" for a release-able spec.
%define exogeni_user_id geni-orca
%define exogeni_group_id nonrenci

# These definitions are workarounds.
# I couldn't find another way to disable the brp-java-repack-jars which was called in __os_install_post.
%define __os_install_post %{nil}
# This is needed to get around the generated wrapper binaries creating pointless "debug" packages.
%global debug_package %{nil}

%description
ORCA is an infrastructure-as-a-service control framework.
ORCA provisions virtual networked systems via secure and distributed
management of heterogeneous resources over federated substrate sites
and domains.

%package common
Summary: Common configuration items for all of ORCA
Group: Applications/System

%description common
ORCA is an infrastructure-as-a-service control framework.
ORCA provisions virtual networked systems via secure and distributed
management of heterogeneous resources over federated substrate sites
and domains.
This package contains configuration shared among all ORCA packages.

%package controller
Summary: ORCA XML-RPC Controller
Group: Applications/System
Requires: jdk
Requires: orca-iaas-common = %{version}-%{release}

%description controller
ORCA is an infrastructure-as-a-service control framework.
ORCA provisions virtual networked systems via secure and distributed
management of heterogeneous resources over federated substrate sites
and domains.
This package contains the ORCA XMLRPC controller, which provides both
ORCA and GENI interfaces.

%package exogeni-am+broker-config
Summary: ORCA ExoGENI-specific AM+Broker configuration
Group: Applications/System
Requires: orca-iaas = %{version}-%{release}
Requires: orca-iaas-common = %{version}-%{release}
Requires(preun): chkconfig

%description exogeni-am+broker-config
ORCA is an infrastructure-as-a-service control framework.
ORCA provisions virtual networked systems via secure and distributed
management of heterogeneous resources over federated substrate sites
and domains.
This package contains the ORCA_HOME directory specific to the
container housing both the AM and Broker actors.

%package exogeni-sm-config
Summary: ORCA ExoGENI-specific SM configuration
Group: Applications/System
Requires: orca-iaas = %{version}-%{release}
Requires: orca-iaas-common = %{version}-%{release}
Requires(preun): chkconfig

%description exogeni-sm-config
ORCA is an infrastructure-as-a-service control framework.
ORCA provisions virtual networked systems via secure and distributed
management of heterogeneous resources over federated substrate sites
and domains.
This package contains the ORCA_HOME directory specific to the
container housing the SM actor.

%package exogeni-controller-config
Summary: ORCA ExoGENI-specific Controller configuration
Group: Applications/System
Requires: orca-iaas-controller = %{version}-%{release}
Requires: orca-iaas-common = %{version}-%{release}
Requires(preun): chkconfig

%description exogeni-controller-config
ORCA is an infrastructure-as-a-service control framework.
ORCA provisions virtual networked systems via secure and distributed
management of heterogeneous resources over federated substrate sites
and domains.
This package contains the ORCA_HOME directory specific to the
container housing the SM actor.

%prep
%setup -q -n %{name}-%{version}-%{shortcommit}

%build
LANG=en_US.UTF-8 MAVEN_OPTS=%{maven_opts} mvn ${MAVEN_ARGS} clean install -DskipTests=true
pushd server
LANG=en_US.UTF-8 MAVEN_OPTS=%{maven_opts} mvn ${MAVEN_ARGS} clean package
LANG=en_US.UTF-8 ANT_OPTS=%{ant_opts} ant ${ANT_ARGS} package
popd
pushd controllers/xmlrpc
LANG=en_US.UTF-8 MAVEN_OPTS=%{maven_opts} mvn ${MAVEN_ARGS} clean package
LANG=en_US.UTF-8 ANT_OPTS=%{ant_opts} ant ${ANT_ARGS} package
popd

%install
# Prep the install locations.
rm -rf %{buildroot}
mkdir -p %{buildroot}%{daemon_common_dir}
mkdir -p %{buildroot}%{controller_common_dir}

# Untar daemon code into desired location
tar -xzf server/target/orcad.tgz -C %{buildroot}%{daemon_common_dir}
# Untar startup code into desired location
tar -xzf server/target/orca-startup.tgz -C %{buildroot}%{daemon_common_dir}
# Untar xmlrpc controller code into desired location
tar -xzf controllers/xmlrpc/target/xmlrpcd.tgz -C %{buildroot}%{controller_common_dir}

# Fix up generated daemon script to use a proper PID directory ...
sed -i -e 's;^PIDDIR="$BASEDIR/logs";PIDDIR="$ORCA_HOME/run";' %{buildroot}%{daemon_common_dir}/bin/orcad
# ... and to put stdout in the right place.
sed -i -e 's;wrapper.daemonize=TRUE;wrapper.logfile=\\"$ORCA_HOME/logs/orca-stdout.log\\" wrapper.daemonize=TRUE;' %{buildroot}%{daemon_common_dir}/bin/orcad
# Finally, fix the wrapper.conf to include a overrides file, that can be separately managed.
echo -e "\n# Finally, include overrides to things set in this file\n#include %{daemon_common_dir}/conf/wrapper-overrides.conf\n" >> %{buildroot}%{daemon_common_dir}/conf/wrapper.conf

# Fix up generated controller script to use a proper PID directory
sed -i -e 's;^PIDDIR="$BASEDIR/logs";PIDDIR="$ORCA_CONTROLLER_HOME/run";' %{buildroot}%{controller_common_dir}/bin/xmlrpcd
# ... and to put stdout in the right place.
sed -i -e 's;wrapper.daemonize=TRUE;wrapper.logfile=\\"$ORCA_CONTROLLER_HOME/logs/controller-stdout.log\\" wrapper.daemonize=TRUE;' %{buildroot}%{controller_common_dir}/bin/xmlrpcd
# Finally, fix the wrapper.conf to include a overrides file, that can be separately managed.
echo -e "\n# Finally, include overrides to things set in this file\n#include %{controller_common_dir}/conf/wrapper-overrides.conf\n" >> %{buildroot}%{controller_common_dir}/conf/wrapper.conf

# Ensure the all utilities are executable.
chmod 755 %{buildroot}%{daemon_common_dir}/bin/*
chmod 755 %{buildroot}%{controller_common_dir}/bin/*
# Clean the bin directories of Windows batch files.
rm -rf %{buildroot}%{daemon_common_dir}/bin/*.bat
rm -rf %{buildroot}%{controller_common_dir}/bin/*.bat

# Create a config directory.
mkdir -p %{buildroot}%{conf_dir}
# Create a log directory.
mkdir -p %{buildroot}%{log_dir}
# Create a run directory to store pid files.
mkdir -p %{buildroot}%{pid_dir}

# Create ORCA_HOME directories for:
# 1) AM + Rack Broker
# 2) Rack SM
# and both log and run directories for each.
mkdir -p %{buildroot}%{conf_dir}/am+broker-12080
mkdir -p %{buildroot}%{log_dir}/am+broker-12080
mkdir -p %{buildroot}%{pid_dir}/am+broker-12080
mkdir -p %{buildroot}%{conf_dir}/sm-14080
mkdir -p %{buildroot}%{log_dir}/sm-14080
mkdir -p %{buildroot}%{pid_dir}/sm-14080
# Create ORCA_CONTROLLER_HOME directory and
# the corresponding log and run directories.
mkdir -p %{buildroot}%{conf_dir}/controller-11080
mkdir -p %{buildroot}%{log_dir}/controller-11080
mkdir -p %{buildroot}%{pid_dir}/controller-11080

# Populate am+broker-12080
cp -R server/orca/axis2repository %{buildroot}%{conf_dir}/am+broker-12080
cp -R server/orca/config %{buildroot}%{conf_dir}/am+broker-12080
cp -R server/orca/lib %{buildroot}%{conf_dir}/am+broker-12080
mkdir -p %{buildroot}%{conf_dir}/am+broker-12080/ssl
# Unique to the AM - an NDL directory.
mkdir -p %{buildroot}%{conf_dir}/am+broker-12080/ndl
# Populate sm-14080
cp -R server/orca/axis2repository %{buildroot}%{conf_dir}/sm-14080
cp -R server/orca/config %{buildroot}%{conf_dir}/sm-14080
cp -R server/orca/lib %{buildroot}%{conf_dir}/sm-14080
mkdir -p %{buildroot}%{conf_dir}/sm-14080/ssl
# Populate controller-11080
cp -R controllers/xmlrpc/xmlrpc/config %{buildroot}%{conf_dir}/controller-11080

# Clean up .git* files that came as a result of the copy
find %{buildroot}%{conf_dir} -type d -name .git -print0 | xargs -0 rm -rf
find %{buildroot}%{conf_dir} -type f -name .gitignore -print0 | xargs -0 rm -rf

# Create a sysconfig directory
mkdir -p %{buildroot}%{_sysconfdir}/sysconfig
# Copy in the appropriate sysconfig script template for each of the above actors,
# and modify it appropriately.
install -p -D -m 644 redhat/orcad-sysconfig.tmpl %{buildroot}%{_sysconfdir}/sysconfig/orca_am+broker-12080
sed -i -e 's;@@ORCA_HOME@@;/etc/orca/am+broker-12080;' %{buildroot}%{_sysconfdir}/sysconfig/orca_am+broker-12080
sed -i -e 's;@@ORCA_SERVER_PORT@@;12080;' %{buildroot}%{_sysconfdir}/sysconfig/orca_am+broker-12080
sed -i -e 's;@@ORCA_SSL_SERVER_PORT@@;12443;' %{buildroot}%{_sysconfdir}/sysconfig/orca_am+broker-12080
install -p -D -m 644 redhat/orcad-sysconfig.tmpl %{buildroot}%{_sysconfdir}/sysconfig/orca_sm-14080
sed -i -e 's;@@ORCA_HOME@@;/etc/orca/sm-14080;' %{buildroot}%{_sysconfdir}/sysconfig/orca_sm-14080
sed -i -e 's;@@ORCA_SERVER_PORT@@;14080;' %{buildroot}%{_sysconfdir}/sysconfig/orca_sm-14080
sed -i -e 's;@@ORCA_SSL_SERVER_PORT@@;14443;' %{buildroot}%{_sysconfdir}/sysconfig/orca_sm-14080
install -p -D -m 644 redhat/xmlrpcd-sysconfig.tmpl %{buildroot}%{_sysconfdir}/sysconfig/orca_controller-11080
sed -i -e 's;@@ORCA_CONTROLLER_HOME@@;/etc/orca/controller-11080;' %{buildroot}%{_sysconfdir}/sysconfig/orca_controller-11080

# Create an init.d directory
mkdir -p %{buildroot}%{_initrddir}
# Copy in the appropriate init script template for each of the above actors,
# and modify it appropriately.
install -p -D -m 755 redhat/orcad-init.tmpl %{buildroot}%{_initrddir}/orca_am+broker-12080
sed -i -e 's;@@SYSCONFIG@@;"orca_am+broker-12080";' %{buildroot}%{_initrddir}/orca_am+broker-12080
install -p -D -m 755 redhat/orcad-init.tmpl %{buildroot}%{_initrddir}/orca_sm-14080
sed -i -e 's;@@SYSCONFIG@@;"orca_sm-14080";' %{buildroot}%{_initrddir}/orca_sm-14080
install -p -D -m 755 redhat/xmlrpcd-init.tmpl %{buildroot}%{_initrddir}/orca_controller-11080
sed -i -e 's;@@SYSCONFIG@@;"orca_controller-11080";' %{buildroot}%{_initrddir}/orca_controller-11080

%clean
rm -rf %{buildroot}
rm -rf %{_builddir}/orca-%{version}

%preun exogeni-am+broker-config
if [ "$1" == "0" ]; then
	/sbin/chkconfig --del orca_am+broker-12080
	[ -x "/etc/init.d/orca_am+broker-12080" ] && /etc/init.d/orca_am+broker-12080 stop
        [ -e %{conf_dir}/am+broker-12080/logs ] && rm %{conf_dir}/am+broker-12080/logs
        [ -e %{conf_dir}/am+broker-12080/run ] && rm %{conf_dir}/am+broker-12080/run
        [ -e %{conf_dir}/am+broker-12080/startup ] && rm %{conf_dir}/am+broker-12080/startup
        [ -d %{conf_dir}/am+broker-12080/packages ] && rm -rf %{conf_dir}/am+broker-12080/packages
        [ -d %{conf_dir}/am+broker-12080/handlers ] && rm -rf %{conf_dir}/am+broker-12080/handlers
        [ -d %{conf_dir}/am+broker-12080/scripts ] && rm -rf %{conf_dir}/am+broker-12080/scripts
fi
# Force a successful exit even if we didn't exit cleanly.
exit 0

%post exogeni-am+broker-config
[ -L %{conf_dir}/am+broker-12080/logs ] || ln -s %{log_dir}/am+broker-12080 %{conf_dir}/am+broker-12080/logs 2>/dev/null
[ -L %{conf_dir}/am+broker-12080/run ] || ln -s %{pid_dir}/am+broker-12080 %{conf_dir}/am+broker-12080/run 2>/dev/null
[ -L %{conf_dir}/am+broker-12080/startup ] || ln -s %{daemon_common_dir}/startup %{conf_dir}/am+broker-12080/ 2>/dev/null
# Force a successful exit even if we didn't exit cleanly.
exit 0

%preun exogeni-sm-config
if [ "$1" == "0" ]; then
	/sbin/chkconfig --del orca_sm-14080
	[ -x "/etc/init.d/orca_sm-14080" ] && /etc/init.d/orca_sm-14080 stop
        [ -e %{conf_dir}/sm-14080/logs ] && rm %{conf_dir}/sm-14080/logs
        [ -e %{conf_dir}/sm-14080/run ] && rm %{conf_dir}/sm-14080/run
        [ -e %{conf_dir}/sm-14080/startup ] && rm %{conf_dir}/sm-14080/startup
        [ -d %{conf_dir}/sm-14080/packages ] && rm -rf %{conf_dir}/sm-14080/packages
        [ -d %{conf_dir}/sm-14080/handlers ] && rm -rf %{conf_dir}/sm-14080/handlers
        [ -d %{conf_dir}/sm-14080/scripts ] && rm -rf %{conf_dir}/sm-14080/scripts
fi
# Force a successful exit even if we didn't exit cleanly.
exit 0

%post exogeni-sm-config
[ -L %{conf_dir}/sm-14080/logs ] || ln -s %{log_dir}/sm-14080 %{conf_dir}/sm-14080/logs 2>/dev/null
[ -L %{conf_dir}/sm-14080/run ] || ln -s %{pid_dir}/sm-14080 %{conf_dir}/sm-14080/run 2>/dev/null
[ -L %{conf_dir}/sm-14080/startup ] || ln -s %{daemon_common_dir}/startup %{conf_dir}/sm-14080/ 2>/dev/null
# Force a successful exit even if we didn't exit cleanly.
exit 0

%preun exogeni-controller-config
if [ "$1" == "0" ]; then
	/sbin/chkconfig --del orca_controller-11080
	[ -x "/etc/init.d/orca_controller-11080" ] && /etc/init.d/orca_controller-11080 stop
        [ -e %{conf_dir}/controller-11080/logs ] && rm %{conf_dir}/controller-11080/logs
        [ -e %{conf_dir}/controller-11080/run ] && rm %{conf_dir}/controller-11080/run
fi
# Force a successful exit even if we didn't exit cleanly.
exit 0

%post exogeni-controller-config
[ -L %{conf_dir}/controller-11080/logs ] || ln -s %{log_dir}/controller-11080 %{conf_dir}/controller-11080/logs 2>/dev/null
[ -L %{conf_dir}/controller-11080/run ] || ln -s %{pid_dir}/controller-11080 %{conf_dir}/controller-11080/run 2>/dev/null
# Force a successful exit even if we didn't exit cleanly.
exit 0

%files
%defattr(-, root, root)
%attr(755, root, root) %dir %{daemon_common_dir}
%{daemon_common_dir}/bin
%{daemon_common_dir}/conf
%{daemon_common_dir}/lib
%{daemon_common_dir}/startup

%files common
%defattr(-, %{exogeni_user_id}, %{exogeni_group_id})
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{conf_dir}
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{log_dir}
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{pid_dir}

%files controller
%defattr(-, root, root)
%attr(755, root, root) %dir %{controller_common_dir}
%{controller_common_dir}/bin
%{controller_common_dir}/conf
%{controller_common_dir}/lib

%files exogeni-am+broker-config
%defattr(-, %{exogeni_user_id}, %{exogeni_group_id})
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{conf_dir}/am+broker-12080
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{conf_dir}/am+broker-12080/config
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{log_dir}/am+broker-12080
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{pid_dir}/am+broker-12080
%{conf_dir}/am+broker-12080/axis2repository
%{conf_dir}/am+broker-12080/lib
%{conf_dir}/am+broker-12080/ndl
%{conf_dir}/am+broker-12080/ssl
%attr(755, root, root) %{_initrddir}/orca_am+broker-12080
%config(noreplace) %{_sysconfdir}/sysconfig/orca_am+broker-12080
%config(noreplace) %{conf_dir}/am+broker-12080/config/*

%files exogeni-sm-config
%defattr(-, %{exogeni_user_id}, %{exogeni_group_id})
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{conf_dir}/sm-14080
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{conf_dir}/sm-14080/config
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{log_dir}/sm-14080
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{pid_dir}/sm-14080
%{conf_dir}/sm-14080/axis2repository
%{conf_dir}/sm-14080/lib
%{conf_dir}/sm-14080/ssl
%attr(755, root, root) %{_initrddir}/orca_sm-14080
%config(noreplace) %{_sysconfdir}/sysconfig/orca_sm-14080
%config(noreplace) %{conf_dir}/sm-14080/config/*

%files exogeni-controller-config
%defattr(-, %{exogeni_user_id}, %{exogeni_group_id})
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{conf_dir}/controller-11080
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{conf_dir}/controller-11080/config
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{log_dir}/controller-11080
%attr(755, %{exogeni_user_id}, %{exogeni_group_id}) %dir %{pid_dir}/controller-11080
%attr(755, root, root) %{_initrddir}/orca_controller-11080
%config(noreplace) %{_sysconfdir}/sysconfig/orca_controller-11080
%config(noreplace) %{conf_dir}/controller-11080/config/*

%changelog
*Tue Jan 19 2016 Victor J. Orlikowski <vjo@duke.edu>
- Rebuild for production deployment, take 2.

*Mon Jan 18 2016 Ilya Baldin <ibaldin@renci.org>
- Rebuild for production deployment.

*Thu Dec 18 2015 Ilya Baldin <ibaldin@renci.org>
- Rebuild again to test pubsub at RCI
