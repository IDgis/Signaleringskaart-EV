
\c ev_services_ov

  -- Table: basisnet_spoor_ov_pr10_6
  
CREATE TABLE basisnet_spoor_ov_pr10_6
(
  id serial NOT NULL,
  geom geometry(MultiPolygon,28992),
  nieuwe_cod character varying(254),
  pr10_6 character varying(254),
  pr10_7 numeric,
  pr10_8 numeric,
  pag character varying(254),
  a numeric,
  b2 numeric,
  b3 numeric,
  c3 numeric,
  d3 numeric,
  d4 numeric,
  awk numeric,
  b2wk numeric,
  breedte character varying(254),
  pr10_6i character varying(254),
  CONSTRAINT basisnet_spoor_ov_pr10_6_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE basisnet_spoor_ov_pr10_6
  OWNER TO postgres;

CREATE INDEX sidx_basisnet_spoor_ov_pr10_6_geom
  ON basisnet_spoor_ov_pr10_6
  USING gist
  (geom);

  -- Table: basisnet_spoor_ov_pr10_7

CREATE TABLE basisnet_spoor_ov_pr10_7
(
  id serial NOT NULL,
  geom geometry(MultiPolygon,28992),
  nieuwe_cod character varying(254),
  pr10_6 character varying(254),
  pr10_7 numeric,
  pr10_8 numeric,
  pag character varying(254),
  a numeric,
  b2 numeric,
  b3 numeric,
  c3 numeric,
  d3 numeric,
  d4 numeric,
  awk numeric,
  b2wk numeric,
  breedte character varying(254),
  CONSTRAINT basisnet_spoor_ov_pr10_7_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE basisnet_spoor_ov_pr10_7
  OWNER TO postgres;

CREATE INDEX sidx_basisnet_spoor_ov_pr10_7_geom
  ON basisnet_spoor_ov_pr10_7
  USING gist
  (geom);

-- Table: basisnet_spoor_ov_pr10_8

CREATE TABLE basisnet_spoor_ov_pr10_8
(
  id serial NOT NULL,
  geom geometry(MultiPolygon,28992),
  nieuwe_cod character varying(254),
  pr10_6 character varying(254),
  pr10_7 numeric,
  pr10_8 numeric,
  pag character varying(254),
  a numeric,
  b2 numeric,
  b3 numeric,
  c3 numeric,
  d3 numeric,
  d4 numeric,
  awk numeric,
  b2wk numeric,
  breedte character varying(254),
  CONSTRAINT basisnet_spoor_ov_pr10_8_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE basisnet_spoor_ov_pr10_8
  OWNER TO postgres;

CREATE INDEX sidx_basisnet_spoor_ov_pr10_8_geom
  ON basisnet_spoor_ov_pr10_8
  USING gist
  (geom);