FROM centos:centos6.8

LABEL description = "A base Orca image, with common packages installed, including either Oracle JDK or OpenJDK" \
      build = "docker build -t orca-base:openjdk ."

RUN yum -y install epel-release; yum clean all
RUN yum -y install initscripts glibc nss-softokn-freebl && yum clean all


# conditional build and tag from Maven: oracle
RUN if [ "${orca.docker.jre.vendor}" == "oracle" ]; then \
		cd /tmp && \
		curl --location --insecure --header "Cookie: oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/7u79-b15/jre-7u79-linux-x64.rpm" > jre-7u79-linux-x64.rpm && \
		yum -y install *.rpm && \
		yum clean all; \
    fi

# conditional build and tag from Maven: openjdk
RUN if [ "${orca.docker.jre.vendor}" == "openjdk" ]; then \
		yum -y install java-1.7.0-openjdk && yum clean all; \
	fi
