CREATE TABLE ressurs_hendelse
(
    id                      bigint generated always as identity     PRIMARY KEY,
    sporings_id             text                                    NOT NULL,
    hendelse_type           text                                    NOT NULL,
    mottatt_tidspunkt       timestamp with time zone                NOT NULL,
    endret_av               text                                    NOT NULL,
    navident                text                                    NOT NULL,
    data                    jsonb                                   NOT NULL,
    behandlings_id          bigint
);

CREATE INDEX idx_navident on ressurs_hendelse(navident);
