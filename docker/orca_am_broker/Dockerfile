FROM orca_common:openjdk

LABEL description = "An Orca AM+Broker configured to talk to a named linked MySQL container" \
      cmd = "docker run -d --name orca_am_broker --link orca_mysql orca_am_broker"

COPY resources /tmp/
#COPY resources/orca-iaas-common-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/
#COPY resources/orca-iaas-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/
#COPY resources/orca-iaas-exogeni-am+broker-config-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/

RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all

RUN sed -e '/oom_adj/ s/^#*/#/' -i /etc/init.d/orca_am+broker-12080

# Tell Orca which MySQL server to talk to (use a named linked docker container as hostname)
#RUN sed -e '/mysql.server/s/localhost/orca_mysql/' -i /etc/orca/am+broker-12080/config/orca.properties

RUN cp /etc/orca/am+broker-12080/config/config.xml /etc/orca/am+broker-12080/config/config.xml.rpmnew && \
    cp /etc/orca/am+broker-12080/config/orca.properties /etc/orca/am+broker-12080/config/orca.properties.rpmnew

COPY config /etc/orca/am+broker-12080/config/

EXPOSE 12080 8443

