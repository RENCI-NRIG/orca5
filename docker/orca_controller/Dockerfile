FROM orca_common:openjdk

LABEL description = "An Orca controller configured to talk to a named linked MySQL container" \
      cmd = "docker run -d --name orca_controller --link orca_mysql orca_controller"

# In the future, hopefully we will use Maven to automatically copy these files after they are built.
COPY resources /tmp/

RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all

RUN sed -e '/oom_adj/ s/^#*/#/' -i /etc/init.d/orca_controller-11080

# Tell Orca which MySQL server to talk to (use a named linked docker container as hostname)
#RUN sed -e '/mysql.server/s/localhost/orca_mysql/' -i /etc/orca/controller-11080/config/orca.properties

EXPOSE 11080 8443

# this comes in via the ONBUILD COPY in orca_common
#COPY ./supervisord.conf /etc/supervisord.conf

#ENTRYPOINT ["/usr/bin/supervisord", "-n"]
