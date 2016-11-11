FROM centos:centos6.8

LABEL description = "An Orca SM configured to talk to a named linked MySQL container" \
      cmd = "docker run -d --name orca_sm --link orca_mysql orca_sm"

# It should be possible to use Oracle Java, but their website isn't even letting me download the JDK at the moment.
#RUN cd /tmp && curl --insecure --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/7u79-b15/jre-7u79-linux-x64.rpm" && yum -y install *.rpm && yum clean all
RUN yum -y install epel-release; yum clean all
RUN yum -y install initscripts java-1.7.0-openjdk glibc nss-softokn-freebl && yum clean all
RUN yum -y install supervisor && yum clean all
#RUN yum -y install mysql mysql-server && yum clean all

# The appropriate Orca RPMs need to be present in ./resources/ (locally) in order to build this image.
# In the future, hopefully we will use Maven to automatically copy these files after they are built.
COPY resources /tmp/
#COPY resources/orca-iaas-common-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/
#COPY resources/orca-iaas-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/
#COPY resources/orca-iaas-exogeni-am+broker-config-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/

RUN useradd -ms /bin/bash geni-orca && \
    groupadd nonrenci && \
    usermod -a -G nonrenci geni-orca


#RUN chkconfig mysqld on && \
    #service mysqld start && \
    #mysql -e "create database orca" && \
    #mysql -e "CREATE USER 'orca'@'localhost';" && \
    #mysql -e "GRANT ALL PRIVILEGES ON orca.* to 'orca'@'localhost';" && \
    #mysql -u orca -D orca < /tmp/full.schema.sql && \
    #service mysqld stop

RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all

# install sudo ?
#RUN echo '%geni-orca ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

RUN sed -e '/oom_adj/ s/^#*/#/' -i /etc/init.d/orca_sm-14080

# Tell Orca which MySQL server to talk to (use a named linked docker container as hostname)
RUN sed -e '/mysql.server/s/localhost/orca_mysql/' -i /etc/orca/sm-14080/config/orca.properties

EXPOSE 14080 8443

COPY ./supervisord.conf /etc/supervisord.conf

ENTRYPOINT ["/usr/bin/supervisord", "-n"]
