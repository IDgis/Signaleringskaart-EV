FROM postgres:9.4

# add PostGIS extension
RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		postgresql-9.4-postgis-2.2 \
		postgresql-9.4-postgis-2.2-scripts \
	&& rm -rf /var/lib/apt/lists/*
	

# create database
COPY 000_create_db.sql /docker-entrypoint-initdb.d/
COPY 001_create_ev_services_tables.sql /docker-entrypoint-initdb.d/

