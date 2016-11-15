FROM orca_common:openjdk

LABEL description = "An Orca SM configured to talk to a named linked MySQL container" \
      cmd = "docker run -d --name orca_sm --link orca_mysql orca_sm"

# In the future, hopefully we will use Maven to automatically copy these files after they are built.
COPY resources /tmp/
#COPY resources/orca-iaas-common-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/
#COPY resources/orca-iaas-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/
#COPY resources/orca-iaas-exogeni-am+broker-config-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/


RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all


RUN sed -e '/oom_adj/ s/^#*/#/' -i /etc/init.d/orca_sm-14080

# Tell Orca which MySQL server to talk to (use a named linked docker container as hostname)
#RUN sed -e '/mysql.server/s/localhost/orca_mysql/' -i /etc/orca/sm-14080/config/orca.properties

RUN cp /etc/orca/sm-14080/config/config.xml /etc/orca/sm-14080/config/config.xml.rpmnew && \
    cp /etc/orca/sm-14080/config/orca.properties /etc/orca/sm-14080/config/orca.properties.rpmnew

COPY config /etc/orca/sm-14080/config/

EXPOSE 14080 8443

# this comes in via the ONBUILD COPY in orca_common
#COPY ./supervisord.conf /etc/supervisord.conf

#ENTRYPOINT ["/usr/bin/supervisord", "-n"]
