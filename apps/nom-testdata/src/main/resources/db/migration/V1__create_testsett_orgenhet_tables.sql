CREATE TABLE test_sett
(
    id             BIGSERIAL        PRIMARY KEY,
    navn           TEXT             NOT NULL,
    beskrivelse    TEXT,
    opprettet      TIMESTAMP        NOT NULL,
    opprettet_av   TEXT             NOT NULL,
    sist_endret    TIMESTAMP        NOT NULL,
    sist_endret_av TEXT             NOT NULL
);

CREATE TABLE orgenhet_state
(
    id                    BIGSERIAL     PRIMARY KEY,
    test_sett_id          BIGINT        NOT NULL REFERENCES test_sett (id),
    sort_idx              BIGINT        NOT NULL,
    state_timestamp       TIMESTAMP     NOT NULL,
    fid                   TEXT,
    orgenhet_id           TEXT          NOT NULL,
    navn                  TEXT,
    gyldig_fom            DATE,
    gyldig_tom            DATE,
    orgenhets_type        TEXT,
    nom_nivaa             TEXT,
    unique_agresso_id     TEXT,
    agresso_id            TEXT,
    agresso_org_nivaa     TEXT,
    agresso_orgenhet_type TEXT,
    tilgangsenhet_id      TEXT,
    kostnadssted_id       TEXT,
    opprettet             TIMESTAMP     NOT NULL,
    opprettet_av          TEXT          NOT NULL,
    sist_endret           TIMESTAMP     NOT NULL,
    sist_endret_av        TEXT          NOT NULL,
    CONSTRAINT uq_orgenhet_state_test_sett_id UNIQUE (test_sett_id, id),
    CONSTRAINT uq_orgenhet_state_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE orgenhet_evtsrc_msg
(
    id                BIGSERIAL         PRIMARY KEY,
    test_sett_id      BIGINT            NOT NULL REFERENCES test_sett (id),
    orgenhet_state_id BIGINT,
    sort_idx          BIGINT            NOT NULL,
    evtsrc_timestamp  TIMESTAMP         NOT NULL,
    object_type       TEXT              NOT NULL,
    evtsrc_key        TEXT              NOT NULL,
    evtsrc_message    TEXT              NOT NULL,
    opprettet         TIMESTAMP         NOT NULL,
    opprettet_av      TEXT              NOT NULL,
    sist_endret       TIMESTAMP         NOT NULL,
    sist_endret_av    TEXT              NOT NULL,

    CONSTRAINT uq_orgenhet_evtsrc_msg_sort_idx UNIQUE (test_sett_id, sort_idx) DEFERRABLE INITIALLY IMMEDIATE,
    CONSTRAINT fk_orgenhet_evtsrc_msg_state FOREIGN KEY (test_sett_id, orgenhet_state_id) REFERENCES orgenhet_state (test_sett_id, id)
);

CREATE INDEX idx_orgenhet_state_test_sett_orgenhet_sort ON orgenhet_state (test_sett_id, orgenhet_id, sort_idx);

CREATE INDEX idx_orgenhet_evtsrc_msg_test_sett_state ON orgenhet_evtsrc_msg (test_sett_id, orgenhet_state_id);
