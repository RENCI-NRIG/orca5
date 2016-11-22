#FROM orca-common:openjdk
FROM renci/orca-common:${git.commit.id.abbrev}-${orca.docker.jre.vendor}

LABEL description = "An Orca AM+Broker configured to talk to a named linked MySQL container" \
      build = "docker build -t orca-am-broker ." \
      run = "docker run -d \
                    --net orca \
                    --name orca-am-broker \
                    --hostname orca-am-broker \
                    -v /opt/orca/am+broker/config:/etc/orca/am+broker-12080/config \
                    -v /opt/orca/am+broker/ndl:/etc/orca/am+broker-12080/ndl \
                    renci/orca-am-broker"

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

#COPY config /etc/orca/am+broker-12080/config/
#COPY ndl /etc/orca/am+broker-12080/ndl/

RUN chown -R geni-orca:nonrenci /etc/orca

#VOLUME /etc/orca/am+broker-12080/config /opt/orca/am+broker/config
#VOLUME /etc/orca/am+broker-12080/ndl /opt/orca/am+broker/ndl

EXPOSE 12080 8443

COPY ./docker-entrypoint.sh /

RUN chmod a+x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["orca_am+broker-12080"]

