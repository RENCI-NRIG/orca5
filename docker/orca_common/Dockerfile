#FROM orca_base:oracle
FROM orca_base:openjdk

LABEL description = "An Orca image with common Orca libraries / configuration installed" 

# add geni users and groups
RUN useradd -ms /bin/bash geni-orca && \
    groupadd nonrenci && \
    usermod -a -G nonrenci geni-orca

# In the future, hopefully we will use Maven to automatically copy these files after they are built.
COPY resources /tmp/
#COPY resources/orca-iaas-common-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/
#COPY resources/orca-iaas-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/
#COPY resources/orca-iaas-exogeni-am+broker-config-5.0.0-201611101008git5e2079af.x86_64.rpm /tmp/


RUN yum -y install /tmp/orca-iaas-*.rpm && yum clean all


ONBUILD COPY ./supervisord.conf /etc/supervisord.conf

ENTRYPOINT ["/usr/bin/supervisord", "-n"]
