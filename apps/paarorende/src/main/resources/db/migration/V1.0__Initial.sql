CREATE SEQUENCE KONTAKTINFO_SEQ START WITH 100000000 increment by 1;

CREATE TABLE KONTAKTINFO (
    ID              BIGINT          PRIMARY KEY,
    KONTAKT_ID      TEXT            UNIQUE,
    NAVIDENT        TEXT            NOT NULL,
    FORNAVN         TEXT            NOT NULL,
    ETTERNAVN       TEXT            NOT NULL,
    RELASJON        TEXT,
    TELEFON         TEXT            NOT NULL,
    ADRESSE         TEXT,
    POSTNR          TEXT,
    POSTSTED        TEXT,
    LAND            TEXT,
    PRIORITET       SMALLINT,
    OPPRETTET_AV    TEXT            NOT NULL,
    DATO_OPPRETTET  DATE            NOT NULL,
    ENDRET_AV       TEXT            NOT NULL,
    DATO_ENDRET     DATE            NOT NULL,
    DATO_SLETTET    DATE
);
