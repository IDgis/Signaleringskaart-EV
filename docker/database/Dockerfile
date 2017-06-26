FROM postgres:9.6

# add PostGIS extension
RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		postgresql-9.6-postgis-2.3 \
		postgresql-9.6-postgis-2.3-scripts \
	&& rm -rf /var/lib/apt/lists/*
	

# create database
COPY 000_create_db.sql /docker-entrypoint-initdb.d/

