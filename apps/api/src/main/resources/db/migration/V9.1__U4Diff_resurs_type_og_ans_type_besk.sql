DROP TABLE T_U4_DIFF; -- Data i tabellen blir uansett overskrevet, så dette er ok

CREATE TABLE T_U4_DIFF (
    id                              SERIAL  NOT NULL,
    navident                        TEXT    NOT NULL,
    kilde                           TEXT,
    nom_sektor                      TEXT,
    type                            TEXT    NOT NULL,
    u4_orgenhet_id                  TEXT,
    u4_orgenhet_navn                TEXT,
    u4_gyldig_fom                   DATE,
    u4_gyldig_tom                   DATE,
    u4_dol                          CHAR(1),
    u4_sektor_paa_stilling          TEXT,
    u4_sektor_paa_ressurs           TEXT,
    u4_stilling_periode             TEXT,
    u4_stilling_ansettelsestype_besk TEXT,
    u4_ressurs_ansettelsestype_besk TEXT,
    u4_ressurs_type                 TEXT,
    nom_orgenhet_id                 TEXT,
    nom_orgenhet_navn               TEXT,
    nom_gyldig_fom                  DATE,
    nom_gyldig_tom                  DATE,
    nom_dol                         CHAR(1),
    nom_ot_sektor                   TEXT,
    nom_orgtilknytning              TEXT,
    PRIMARY KEY (id)
);

CREATE INDEX idx_t_u4iff_navident ON T_U4_DIFF (navident);