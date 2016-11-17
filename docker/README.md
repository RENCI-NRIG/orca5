This module can be used to build Docker images for Orca.

## Building Docker Images
If you have all of the dependencies setup, you should be able to simply do:
```
mvn clean package
```

## Running the Docker images
Four runnable Docker images will be created. Start them in this order:

A MySQL container configured for Orca:
```
docker run -d --net orca --name orca-mysql --hostname orca-mysql renci/orca-mysql
```

The Orca AM+Broker:
```
docker run -d \
           --net orca \
           --name orca-am-broker \
           --hostname orca-am-broker \
           -v /opt/orca/am+broker/config:/etc/orca/am+broker-12080/config \
           -v /opt/orca/am+broker/ndl:/etc/orca/am+broker-12080/ndl \
           renci/orca-am-broker
```

The Orca SM:
```
docker run -d \
           --net orca \
           --name orca-sm \
           --hostname orca-sm \
           -v /opt/orca/sm/config:/etc/orca/sm-14080/config \
           renci/orca-sm
```

The Orca Controller:
```
docker run -d \
           --net orca \
           --name orca-controller \
           --hostname orca-controller \
           -p 11443:11443 \
           -v /opt/orca/controller/config:/etc/orca/controller-11080/config \
           renci/orca-controller
```

## Dependencies
1. Docker must be installed on your system.  You will either need to run the above commands with sudo / as root, or add your current user to the 'docker' group.
1. Orca needs many configuration files to operate.  These need to be volume mounted into the running containers (the `-v` options above).
1. The Orca RPMs must be built before the Docker images can be built.  See the `redhat` module for more details.

