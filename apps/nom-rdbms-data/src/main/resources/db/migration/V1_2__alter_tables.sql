ALTER TABLE ressurs
    ADD COLUMN sist_endret_av text;

ALTER TABLE orgtilknytning
    ADD COLUMN masseflyttet_fra_fid text,
    ADD COLUMN sist_endret_av       text;