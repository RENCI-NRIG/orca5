FROM centos:centos6.9

LABEL description = "A base Orca image, with common packages installed, including either Oracle JDK or OpenJDK" \
      build = "docker build -t orca-base:openjdk ."

RUN yum -y update && yum clean all
RUN yum -y install epel-release; yum clean all
RUN yum -y install initscripts glibc glibc.i686 nss-softokn-freebl nss-softokn-freebl.i686 && yum clean all

# Oracle Java 7 JDK is no longer available for download
ENV JAVA_VERSION 8u131
ENV BUILD_VERSION b11
ENV oracle_rpm jdk-${JAVA_VERSION}-linux-x64.rpm

# conditional build and tag from Maven: oracle
RUN if [ "${orca.docker.jre.vendor}" == "oracle_1.8" ]; then \
		cd /tmp && \
		curl \
			--location \
			--retry 3 \
			--insecure \
			--show-error \
			--header "Cookie: oraclelicense=accept-securebackup-cookie" \
			"http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION}-${BUILD_VERSION}/d54c1d3a095b4ff2b6607d096fa80163/${oracle_rpm}" > ${oracle_rpm} && \
		yum -y install *.rpm && \
		yum clean all && \
		rm -f /tmp/${oracle_rpm}; \
    fi

# conditional build and tag from Maven: openjdk
RUN if [ "${orca.docker.jre.vendor}" == "openjdk_1.8" ]; then \
		yum -y install java-1.8.0-openjdk && yum clean all; \
	fi


RUN echo $'export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:/bin/java::")' >> /etc/bashrc; \
	echo $'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/bashrc;

