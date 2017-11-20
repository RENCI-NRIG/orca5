This module can be used to build Docker images for Orca.

## Building Docker Images
If you have all of the dependencies setup, you should be able to simply do:
```
./docker_build.sh
```

Optionally, you can specify the JRE vendor to use (Oracle and OpenJDK are supported) using `-j` or `--jre`
```
./docker_build.sh --jre openjdk
```

Or, you can build individual containers manually using Maven:
```
mvn clean package -Pdocker
```

## Running the Docker images
Four runnable Docker images will be created. Use this script to start them all:
```
./docker_run.sh
```

You can specify a specific docker image tag using `-t` or `--tag-name`:
```
./docker_run.sh --tag-name openjdk
./docker_run.sh --tag-name 5.1.7-SNAPSHOT-openjdk
```

### Running the containers manually
Start them manually in this order:

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
           --volume /opt/orca/am+broker/config:/etc/orca/am+broker-12080/config \
           --volume /opt/orca/am+broker/ndl:/etc/orca/am+broker-12080/ndl \
           renci/orca-am-broker
```

The Orca SM:
```
docker run -d \
           --net orca \
           --name orca-sm \
           --hostname orca-sm \
           --volume /opt/orca/sm/config:/etc/orca/sm-14080/config \
           renci/orca-sm
```

The Orca Controller:
```
docker run -d \
           --net orca \
           --name orca-controller \
           --hostname orca-controller \
           --publish 11443:11443 \
           --volume /opt/orca/controller/config:/etc/orca/controller-11080/config \
           renci/orca-controller
```

### Examining a running container
You may find that you need to be able to get shell access inside a running container, for example to look at the Orca logs.  You can use the `docker exec` command start a shell inside a running container.  Just subsititute the name of the container you need:
```
docker exec -it orca-sm bash
```

## Dependencies
1. Docker must be installed on your system.  You will either need to run the above commands with sudo / as root, or add your current user to the 'docker' group.
1. Orca needs many configuration files to operate.  These need to be volume mounted into the running containers (the `--volume` options above).
1. The [docker_build.sh](https://github.com/RENCI-NRIG/orca5/blob/master/docker/docker_build.sh) script will build the necessary Orca RPMs for you. If you are building the Docker images manually, you will need to prepare the Orca RPMs.  See the `redhat` module for more details on building Orca RPMs.
1. The [docker_run.sh](https://github.com/RENCI-NRIG/orca5/blob/master/docker/docker_run.sh) script will create a docker network for Orca to use.  If you are running the Docker containers manually, you will need to create a docker network for Orca to use (referenced in the above docker run commands). e.g. `docker network create orca`

### Using Docker to build RPMs
Use this script to build the orca-rpmbuild container and build the RPMs.  They will be copied into your `~/orca-build/` directory (which will be created, if it does not exist. The RPMs are built automatically for you if you run the top-level `docker_build.sh` script.
```
docker_build_rpms.sh
```

This container makes use of your local/host files in order to build the Orca RPMs from your working copy of the source code.  In each of the below `--volume` statements below, the container expects to find necessary files in the location specified to the right of the `:`.  You may modify the left-side of the `--volume` statement to match your local/host filesystem.

If the `~/orca-build/` directory does not already exist, it will be created by the rpm build.

Including your `~/.m2/` directory and associated local Maven repository can greatly improve your build time.

```
docker run \
           --volume ~/git/orca5:/root/git/orca5 \
           --volume ~/orca-build/:/root/orca-build/ \
           --volume ~/.m2/:/root/.m2/ \
           renci/orca-rpmbuild
```

## Installing Docker on Mac OS X
1. Visit the [docker-for-mac](https://docs.docker.com/docker-for-mac/) page.
1. Review the [System Requirements](https://docs.docker.com/docker-for-mac/#/what-to-know-before-you-install)
1. Download [Docker for Mac](https://docs.docker.com/docker-for-mac/#/download-docker-for-mac) by clicking the [Get Docker for Mac (stable)](https://download.docker.com/mac/stable/Docker.dmg) button.
1. [Install and Run Docker for Mac](https://docs.docker.com/docker-for-mac/#/step-1-install-and-run-docker-for-mac)
1. Add the `/opt` directory (or the directory where you have stored Orca config) to the shared paths available to Docker: Docker -> Preferences... -> File Sharing

## Hopefully-Only-Temporarily Complicated Instructions for Putting it all Together
1. Build the base docker image, that doesn't depend on any RPMs: `cd docker/orca_base && mvn clean package -Pdocker`
1. Build the orca-rpmbuild docker image: `cd ../orca-rpmbuild && mvn clean package -Pdocker`
1. [Build RPMs using Docker](#using-docker-to-build-rpms)
1. Build the remainder of the Orca Docker images: 'cd .. && mvn clean package -Pdocker`

