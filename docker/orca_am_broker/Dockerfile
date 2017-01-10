#FROM orca-common:openjdk
FROM renci/orca-common:${git.commit.id.abbrev}-${orca.docker.jre.vendor}

LABEL description = "An Orca AM+Broker configured to talk to a named MySQL container" \
      build = "mvn clean package -Pdocker" \
      run = "docker run -d \
                    --net orca \
                    --name orca-am-broker \
                    --hostname orca-am-broker \
                    -v /opt/orca/am+broker/config:/etc/orca/am+broker-12080/config \
                    -v /opt/orca/am+broker/ndl:/etc/orca/am+broker-12080/ndl \
                    renci/orca-am-broker"

# Copy Orca RPMs to /tmp
COPY resources /tmp/

# rpm install is quicker than yum install
#RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all
RUN rpm -i /tmp/orca-iaas-*.rpm

# oom_adj cannot be modified under Docker
RUN sed -e '/oom_adj/ s/^#*/#/' -i /etc/init.d/orca_am+broker-12080

# Configuration files should be volume mounted at run-time

# Expose ports for AM+Broker
EXPOSE 12080 8443

# Modify docker-entrypoint.sh for AM+Broker
RUN sed -e '/prog/s/orca/orca_am+broker-12080/' -i /docker-entrypoint.sh

CMD ["orca_am+broker-12080"]

