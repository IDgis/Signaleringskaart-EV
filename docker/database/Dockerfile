FROM postgres:9.6

# add PostGIS extension
RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		postgresql-9.6-postgis-2.3 \
		postgresql-9.6-postgis-2.3-scripts \
	&& rm -rf /var/lib/apt/lists/*
	

# create database
COPY data/ev_lime_ov.sql /tmp/
COPY data/ev_services_ov.sql /tmp/
COPY 000_create_db.sql /docker-entrypoint-initdb.d/
COPY 001_restore_db.sql /docker-entrypoint-initdb.d/




