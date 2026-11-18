CREATE TABLE ressurs_state
(
    id                        BIGSERIAL PRIMARY KEY,
    test_sett_id              BIGINT    NOT NULL REFERENCES test_sett (id),
    sort_idx                  BIGINT    NOT NULL,
    state_timestamp           TIMESTAMP NOT NULL,
    fid                       TEXT,
    personident               TEXT,
    navident                  TEXT,
    sektor                    TEXT,
    fornavn                   TEXT,
    mellomnavn                TEXT,
    etternavn                 TEXT,
    jobbtelefon_nummer        TEXT,
    jobbtelefon_type          TEXT,
    primary_telefonnummer     TEXT,
    sluttdato                 DATE,
    startdato                 DATE,
    primary_fysisk_brukersted TEXT,
    fysiske_brukersteder      TEXT[],
    ident_type                TEXT,
    state_sist_endret_av      TEXT,
    tilknytningtype           TEXT,
    opprettet                 TIMESTAMP NOT NULL,
    opprettet_av              TEXT      NOT NULL,
    sist_endret               TIMESTAMP NOT NULL,
    sist_endret_av            TEXT      NOT NULL,
    CONSTRAINT uq_ressurs_state_test_sett_id UNIQUE (test_sett_id, id),
    CONSTRAINT uq_ressurs_state_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE ressurs_evtsrc_msg
(
    id               BIGSERIAL PRIMARY KEY,
    test_sett_id     BIGINT    NOT NULL REFERENCES test_sett (id),
    ressurs_state_id BIGINT,
    sort_idx         BIGINT    NOT NULL,
    evtsrc_timestamp TIMESTAMP NOT NULL,
    object_type      TEXT      NOT NULL,
    evtsrc_key       TEXT      NOT NULL,
    evtsrc_message   TEXT      NOT NULL,
    opprettet        TIMESTAMP NOT NULL,
    opprettet_av     TEXT      NOT NULL,
    sist_endret      TIMESTAMP NOT NULL,
    sist_endret_av   TEXT      NOT NULL,
    CONSTRAINT uq_ressurs_evtsrc_msg_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE,
    CONSTRAINT fk_ressurs_evtsrc_msg_state FOREIGN KEY (test_sett_id, ressurs_state_id) REFERENCES ressurs_state (test_sett_id, id)
);

CREATE INDEX idx_ressurs_state_test_sett_fid_sort ON ressurs_state (test_sett_id, fid, sort_idx);
CREATE INDEX idx_ressurs_evtsrc_msg_test_sett_state ON ressurs_evtsrc_msg (test_sett_id, ressurs_state_id);

CREATE TABLE organisering_state
(
    id              BIGSERIAL PRIMARY KEY,
    test_sett_id    BIGINT    NOT NULL REFERENCES test_sett (id),
    sort_idx        BIGINT    NOT NULL,
    state_timestamp TIMESTAMP NOT NULL,
    fid             TEXT,
    organisering_id TEXT,
    navn            TEXT,
    id_over         TEXT,
    navn_over       TEXT,
    gyldig_fom      DATE,
    gyldig_tom      DATE,
    opprettet       TIMESTAMP NOT NULL,
    opprettet_av    TEXT      NOT NULL,
    sist_endret     TIMESTAMP NOT NULL,
    sist_endret_av  TEXT      NOT NULL,
    CONSTRAINT uq_organisering_state_test_sett_id UNIQUE (test_sett_id, id),
    CONSTRAINT uq_organisering_state_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE organisering_evtsrc_msg
(
    id                    BIGSERIAL PRIMARY KEY,
    test_sett_id          BIGINT    NOT NULL REFERENCES test_sett (id),
    organisering_state_id BIGINT,
    sort_idx              BIGINT    NOT NULL,
    evtsrc_timestamp      TIMESTAMP NOT NULL,
    object_type           TEXT      NOT NULL,
    evtsrc_key            TEXT      NOT NULL,
    evtsrc_message        TEXT      NOT NULL,
    opprettet             TIMESTAMP NOT NULL,
    opprettet_av          TEXT      NOT NULL,
    sist_endret           TIMESTAMP NOT NULL,
    sist_endret_av        TEXT      NOT NULL,
    CONSTRAINT uq_organisering_evtsrc_msg_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE,
    CONSTRAINT fk_organisering_evtsrc_msg_state FOREIGN KEY (test_sett_id, organisering_state_id) REFERENCES organisering_state (test_sett_id, id)
);

CREATE INDEX idx_organisering_state_test_sett_fid_sort ON organisering_state (test_sett_id, fid, sort_idx);
CREATE INDEX idx_organisering_evtsrc_msg_test_sett_state ON organisering_evtsrc_msg (test_sett_id, organisering_state_id);

CREATE TABLE orgenhet_leder_state
(
    id                   BIGSERIAL PRIMARY KEY,
    test_sett_id         BIGINT    NOT NULL REFERENCES test_sett (id),
    sort_idx             BIGINT    NOT NULL,
    state_timestamp      TIMESTAMP NOT NULL,
    fid                  TEXT,
    navident             TEXT,
    org_enhet_id         TEXT,
    startdato            DATE,
    sluttdato            DATE,
    state_sist_endret_av TEXT,
    opprettet            TIMESTAMP NOT NULL,
    opprettet_av         TEXT      NOT NULL,
    sist_endret          TIMESTAMP NOT NULL,
    sist_endret_av       TEXT      NOT NULL,
    CONSTRAINT uq_orgenhet_leder_state_test_sett_id UNIQUE (test_sett_id, id),
    CONSTRAINT uq_orgenhet_leder_state_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE orgenhet_leder_evtsrc_msg
(
    id                      BIGSERIAL PRIMARY KEY,
    test_sett_id            BIGINT    NOT NULL REFERENCES test_sett (id),
    orgenhet_leder_state_id BIGINT,
    sort_idx                BIGINT    NOT NULL,
    evtsrc_timestamp        TIMESTAMP NOT NULL,
    object_type             TEXT      NOT NULL,
    evtsrc_key              TEXT      NOT NULL,
    evtsrc_message          TEXT      NOT NULL,
    opprettet               TIMESTAMP NOT NULL,
    opprettet_av            TEXT      NOT NULL,
    sist_endret             TIMESTAMP NOT NULL,
    sist_endret_av          TEXT      NOT NULL,
    CONSTRAINT uq_orgenhet_leder_evtsrc_msg_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE,
    CONSTRAINT fk_orgenhet_leder_evtsrc_msg_state FOREIGN KEY (test_sett_id, orgenhet_leder_state_id) REFERENCES orgenhet_leder_state (test_sett_id, id)
);

CREATE INDEX idx_orgenhet_leder_state_test_sett_fid_sort ON orgenhet_leder_state (test_sett_id, fid, sort_idx);
CREATE INDEX idx_orgenhet_leder_evtsrc_msg_test_sett_state ON orgenhet_leder_evtsrc_msg (test_sett_id, orgenhet_leder_state_id);

CREATE TABLE orgtilknytning_state
(
    id                                     BIGSERIAL PRIMARY KEY,
    test_sett_id                           BIGINT    NOT NULL REFERENCES test_sett (id),
    sort_idx                               BIGINT    NOT NULL,
    state_timestamp                        TIMESTAMP NOT NULL,
    fid                                    TEXT,
    navident                               TEXT,
    org_enhet_id                           TEXT,
    sektor                                 TEXT,
    er_daglig_oppfolging                   BOOLEAN   NOT NULL,
    gyldig_fom                             DATE,
    gyldig_tom                             DATE,
    orgtilknytning_type                    TEXT,
    primary_fysisk_brukersted_nummer       TEXT,
    additional_fysiske_brukersteder_nummer TEXT[],
    masseflyttet_fra_fid                   TEXT,
    state_sist_endret_av                   TEXT,
    opprettet                              TIMESTAMP NOT NULL,
    opprettet_av                           TEXT      NOT NULL,
    sist_endret                            TIMESTAMP NOT NULL,
    sist_endret_av                         TEXT      NOT NULL,
    CONSTRAINT uq_orgtilknytning_state_test_sett_id UNIQUE (test_sett_id, id),
    CONSTRAINT uq_orgtilknytning_state_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE orgtilknytning_evtsrc_msg
(
    id                      BIGSERIAL PRIMARY KEY,
    test_sett_id            BIGINT    NOT NULL REFERENCES test_sett (id),
    orgtilknytning_state_id BIGINT,
    sort_idx                BIGINT    NOT NULL,
    evtsrc_timestamp        TIMESTAMP NOT NULL,
    object_type             TEXT      NOT NULL,
    evtsrc_key              TEXT      NOT NULL,
    evtsrc_message          TEXT      NOT NULL,
    opprettet               TIMESTAMP NOT NULL,
    opprettet_av            TEXT      NOT NULL,
    sist_endret             TIMESTAMP NOT NULL,
    sist_endret_av          TEXT      NOT NULL,
    CONSTRAINT uq_orgtilknytning_evtsrc_msg_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE,
    CONSTRAINT fk_orgtilknytning_evtsrc_msg_state FOREIGN KEY (test_sett_id, orgtilknytning_state_id) REFERENCES orgtilknytning_state (test_sett_id, id)
);

CREATE INDEX idx_orgtilknytning_state_test_sett_fid_sort ON orgtilknytning_state (test_sett_id, fid, sort_idx);
CREATE INDEX idx_orgtilknytning_evtsrc_msg_test_sett_state ON orgtilknytning_evtsrc_msg (test_sett_id, orgtilknytning_state_id);
