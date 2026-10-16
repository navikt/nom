CREATE TABLE outbox
(
    sporings_id       BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    sf_status         TEXT DEFAULT 'IKKE_SENDT'           NOT NULL,
    feilmelding       TEXT,
    orgenhet_id       VARCHAR(6)                          NOT NULL,
    endret_av         TEXT[],
    endringshendelser TEXT[],
    orgenhet          JSONB                               NOT NULL,
    CONSTRAINT pk_outbox PRIMARY KEY (sporings_id)
);

ALTER TABLE behandling
    ADD COLUMN outbox_sporings_id BIGINT,
    ADD CONSTRAINT fk_behandling_outbox
        FOREIGN KEY (outbox_sporings_id) REFERENCES outbox (sporings_id);

CREATE INDEX idx_behandling_outbox_sporings_id ON behandling (outbox_sporings_id);

CREATE INDEX idx_orgenhet_id on outbox (orgenhet_id);
