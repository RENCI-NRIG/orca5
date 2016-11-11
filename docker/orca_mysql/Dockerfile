FROM mysql:5.7.16

LABEL description = "A MySQL server configured with Orca databases." \
      cmd = "docker run -d --name=orca_mysql orca_mysql"

# we could just put these in a volume, instead of having our own image.
# but this makes it a little easier to run
# The file full.schema.sql needs to be present in ./resources/ (locally) in order to build this image correctly.
# In the future, hopefully we will use Maven to automatically copy this file.
COPY resources /docker-entrypoint-initdb.d

ENV MYSQL_ALLOW_EMPTY_PASSWORD=yes

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["mysqld"]

