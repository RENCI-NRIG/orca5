FROM mysql:5.7.16

LABEL description = "A MySQL server configured with Orca databases." \
      cmd = "docker run -d --net orca --name orca-mysql --hostname orca-mysql renci/orca-mysql"

# we could just put these in a volume, instead of having our own image.
# but this makes it a little easier to run
# The file full.schema.sql needs to be present in ./resources/ (locally) in order to build this image correctly.
# In the future, hopefully we will use Maven to automatically copy this file.
COPY resources /docker-entrypoint-initdb.d

# renaming and modifying this file is difficult to do in Maven. Easier to do it here
RUN cp /docker-entrypoint-initdb.d/full.schema.sql /docker-entrypoint-initdb.d/full.schema.12080.sql && \
    mv /docker-entrypoint-initdb.d/full.schema.sql /docker-entrypoint-initdb.d/full.schema.14080.sql && \
    sed -e '/USE orca/ s/orca/orca12080/' -i /docker-entrypoint-initdb.d/full.schema.12080.sql && \
    sed -e '/USE orca/ s/orca/orca14080/' -i /docker-entrypoint-initdb.d/full.schema.14080.sql

ENV MYSQL_ALLOW_EMPTY_PASSWORD=yes

