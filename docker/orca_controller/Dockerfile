#FROM orca-common:openjdk
FROM renci/orca-common:${git.commit.id.abbrev}

LABEL description = "An Orca controller configured to talk to a named linked MySQL container" \
      build = "docker build -t orca-controller ." \
      run = "docker run -d \
                    --net orca \
                    --name orca-controller \
                    --hostname orca-controller \
                    -p 11443:11443 \
                    -v /opt/orca/controller/config:/etc/orca/controller-11080/config \
                    renci/orca-controller"

# In the future, hopefully we will use Maven to automatically copy these files after they are built.
COPY resources /tmp/

RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all

RUN sed -e '/oom_adj/ s/^#*/#/' -i /etc/init.d/orca_controller-11080

# Tell Orca which MySQL server to talk to (use a named linked docker container as hostname)
#RUN sed -e '/mysql.server/s/localhost/orca_mysql/' -i /etc/orca/controller-11080/config/orca.properties

#COPY config /etc/orca/controller-11080/config/

RUN chown -R geni-orca:nonrenci /etc/orca

EXPOSE 11080 8443 11443

COPY ./docker-entrypoint.sh /

#RUN sed -e '/eval \$COMMAND_LINE/s/eval/echo/' -i /opt/orca-controller/bin/xmlrpcd

RUN chmod a+x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["orca_controller-11080"]

