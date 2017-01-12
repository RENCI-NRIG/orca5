#FROM orca-common:openjdk
FROM renci/orca-common:${git.commit.id.abbrev}-${orca.docker.jre.vendor}

LABEL description = "An Orca SM configured to talk to a named MySQL container" \
      build = "mvn clean package -Pdocker" \
      run = "docker run -d \
                    --net orca \
                    --name orca-sm \
                    --hostname orca-sm \
                    -v /opt/orca/sm/config:/etc/orca/sm-14080/config \
                    renci/orca-sm"

# Copy Orca RPMs to /tmp
COPY resources /tmp/

# rpm install is quicker than yum install
#RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all
RUN rpm -i /tmp/orca-iaas-*.rpm

# oom_adj cannot be modified under Docker
RUN sed -e '/oom_adj/ s/^#*/#/' -i /etc/init.d/orca_sm-14080

# Configuration files should be volume mounted at run-time

# Expose ports for SM
EXPOSE 14080 8443

# Modify docker-entrypoint.sh for AM+Broker
RUN sed -e '/prog/s/orca/orca_sm-14080/' -i /docker-entrypoint.sh

CMD ["orca_sm-14080"]

