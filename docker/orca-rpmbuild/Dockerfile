#FROM centos:centos6.8
# Pull from ${project.version} of orca-base, since it doesn't need to change very often
FROM renci/orca-base:${project.version}-${orca.docker.jre.vendor}

# 'ant' also install openjdk
RUN yum -y install rpm-build git ant && yum clean all

ENV apache_maven_tgz apache-maven-3.3.9-bin.tar.gz

RUN cd /opt && \
    curl "http://mirror.cc.columbia.edu/pub/software/apache/maven/maven-3/3.3.9/binaries/${apache_maven_tgz}" > ${apache_maven_tgz} && \
    tar zxf ${apache_maven_tgz} && \
    rm -f ${apache_maven_tgz}

ENV PATH /opt/apache-maven-3.3.9/bin:$PATH

ENTRYPOINT ["/root/git/orca5/redhat/buildrpm.sh"]
