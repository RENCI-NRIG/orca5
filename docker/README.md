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

### Examining a running container
You may find that you need to be able to get shell access inside a running container, for example to look at the Orca logs.  You can use the `docker exec` command start a shell inside a running container.  Just subsititute the name of the container you need:
```
docker exec -it orca-sm bash
```

## Dependencies
1. Docker must be installed on your system.  You will either need to run the above commands with sudo / as root, or add your current user to the 'docker' group.
1. Orca needs many configuration files to operate.  These need to be volume mounted into the running containers (the `-v` options above).
1. The Orca RPMs must be built before the Docker images can be built.  See the `redhat` module for more details.
1. Before you run your first Orca docker container, you will need to create a docker network for Orca to use (referenced in the above docker run commands): `docker network create orca`

### Using Docker to build RPMs

```
docker run \
           -v ~/git/orca5:/root/git/orca5 \
           -v ~/orca-build/:/root/orca-build/ \
           -v ~/.m2/:/root/.m2/ \
           renci/orca-rpmbuild
```

## Installing Docker on Mac OS X
1. Visit the [docker-for-mac](https://docs.docker.com/docker-for-mac/) page.
1. Review the [System Requirements](https://docs.docker.com/docker-for-mac/#/what-to-know-before-you-install)
1. Download [Docker for Mac](https://docs.docker.com/docker-for-mac/#/download-docker-for-mac) by clicking the [Get Docker for Mac (stable)](https://download.docker.com/mac/stable/Docker.dmg) button.
1. [Install and Run Docker for Mac](https://docs.docker.com/docker-for-mac/#/step-1-install-and-run-docker-for-mac)


