#FROM centos:centos6.8
# Pull from ${project.version} of orca-base, since it doesn't need to change very often
FROM renci/orca-base:${project.version}-${orca.docker.jre.vendor}

# yum ant also install openjdk
#RUN yum -y install rpm-build git ant && yum clean all
RUN yum -y install rpm-build git && yum clean all

# Install Apache Maven
ENV apache_maven_tgz apache-maven-3.3.9-bin.tar.gz

RUN cd /opt && \
    curl "http://mirror.cc.columbia.edu/pub/software/apache/maven/maven-3/3.3.9/binaries/${apache_maven_tgz}" > ${apache_maven_tgz} && \
    tar zxf ${apache_maven_tgz} && \
    rm -f ${apache_maven_tgz}

# Install Apache Ant
ENV apache_ant_tgz apache-ant-1.9.7-bin.tar.gz

RUN cd /opt && \
    curl "http://mirror.cc.columbia.edu/pub/software/apache//ant/binaries/${apache_ant_tgz}" > ${apache_ant_tgz} && \
    tar zxf ${apache_ant_tgz} && \
    rm -f ${apache_ant_tgz}

ENV ANT_HOME /opt/apache-ant-1.9.7

# Setup PATH variable with Maven and Ant
ENV PATH /opt/apache-maven-3.3.9/bin:$PATH:$ANT_HOME/bin

## Add Ant libraries
#ENV ant_contrib_tgz ant-contrib-1.0b3-bin.tar.gz
#
#RUN cd /opt/apache-ant-1.9.7/lib && \
    #curl --location "http://downloads.sourceforge.net/project/ant-contrib/ant-contrib/1.0b3/${ant_contrib_tgz}" | tar zx ant-contrib/ant-contrib-1.0b3.jar --strip=1
#
#COPY resources /opt/apache-ant-1.9.7/lib/

# The Orca source must be volume mounted from the local/host machine
#  (at the `docker run` command)
#  for this entrypoint to work.
ENTRYPOINT ["/root/git/orca5/redhat/buildrpm.sh"]
