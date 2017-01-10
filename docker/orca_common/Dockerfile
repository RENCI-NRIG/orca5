#FROM orca-base:oracle
#FROM orca-base:openjdk
FROM renci/orca-base:${git.commit.id.abbrev}-${orca.docker.jre.vendor}

LABEL description = "An Orca image with common Orca libraries / configuration installed" \
      build = "docker build -t orca-common:openjdk ."

# add geni users and groups
RUN useradd -ms /bin/bash geni-orca && \
    groupadd nonrenci && \
    usermod -a -G nonrenci geni-orca

# Copy Orca RPMs to /tmp
COPY resources /tmp/

#RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all
# rpm install is quicker than yum install.
# deleting the RPM doesn't make our Image any smaller, but it corrects later RPM install complications
RUN rpm -i /tmp/orca-iaas-*.rpm && rm /tmp/orca-iaas-*.rpm

# Copy default wrapper-overrides.conf file to /tmp
# These contain debug statements for JMX remote monitoring,
# that are only enabled if container is started in debug mode
COPY wrapper-overrides.conf /tmp/

#Expose JMX port for Java profiling
EXPOSE 9010

# Copy base docker-entrypoint.sh file.  Will be modified in later stages
COPY ./docker-entrypoint.sh /
RUN chmod a+x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
