FROM centos:centos6.8

LABEL description = "A base Orca image, with common packages installed, including either Oracle JDK or OpenJDK" 

RUN yum -y install epel-release; yum clean all
RUN yum -y install supervisor initscripts glibc nss-softokn-freebl && yum clean all

#ONBUILD COPY ./supervisord.conf /etc/supervisord.conf


# It should be possible to use Oracle Java, but their website isn't even letting me download the JDK at the moment.
#RUN cd /tmp && \
    #curl --location --insecure --header "Cookie: oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/7u79-b15/jre-7u79-linux-x64.rpm" > jre-7u79-linux-x64.rpm && \
    #yum -y install *.rpm && \
    #yum clean all

RUN yum -y install java-1.7.0-openjdk && yum clean all

ENTRYPOINT ["/usr/bin/supervisord", "-n"]
