#FROM orca-common:openjdk
FROM renci/orca-common:${git.commit.id.abbrev}-${orca.docker.jre.vendor}

LABEL description = "An Orca controller configured to talk to a named MySQL container" \
      build = "mvn clean package -Pdocker" \
      run = "docker run -d \
                    --net orca \
                    --name orca-controller \
                    --hostname orca-controller \
                    -p 11443:11443 \
                    -v /opt/orca/controller/config:/etc/orca/controller-11080/config \
                    renci/orca-controller"

# Copy Orca RPMs to /tmp
COPY resources /tmp/

# rpm install is quicker than yum install
#RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all
RUN rpm -i /tmp/orca-iaas-*.rpm

# oom_adj cannot be modified under Docker
RUN sed -e '/oom_adj/ s/^#*/#/' -i /etc/init.d/orca_controller-11080

# Configuration files should be volume mounted at run-time

# Expose ports for Controller
EXPOSE 11080 8443 11443

# Modify docker-entrypoint.sh for Controller
RUN sed -e '/prog/s/orca/orca_controller-11080/' -i /docker-entrypoint.sh && \
    sed -e 's!/opt/orca!/opt/orca-controller!g' -i /docker-entrypoint.sh && \
    sed -e 's/ORCA_HOME/ORCA_CONTROLLER_HOME/g' -i /docker-entrypoint.sh && \
    sed -e 's/orcad/xmlrpcd/g' -i /docker-entrypoint.sh && \
    sed -e 's/orca-stdout/controller-stdout/g' -i /docker-entrypoint.sh

CMD ["orca_controller-11080"]

