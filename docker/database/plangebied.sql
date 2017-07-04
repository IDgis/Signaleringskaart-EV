-- View: public.plangebieden

-- DROP VIEW public.plangebieden;

CREATE OR REPLACE VIEW public.plangebieden AS
 SELECT survey_476659.id,
    st_geomfromtext(survey_476659."476659X46X378", 28992) AS geom
   FROM survey_476659
  WHERE length(survey_476659."476659X46X378") > 0;

ALTER TABLE public.plangebieden
    OWNER TO postgres;