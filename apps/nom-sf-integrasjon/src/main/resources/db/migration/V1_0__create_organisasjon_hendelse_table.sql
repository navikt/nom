CREATE TABLE organisasjon_hendelse
(
    id                  bigint generated always as identity  PRIMARY KEY,
    mottatt_tidspunkt   timestamp with time zone             NOT NULL,
    fid                 text                                 NOT NULL,
    event_key           text                                 NOT NULL,
    orgenhet_id         varchar(6)                           NOT NULL,
    event_type          text                                 NOT NULL,
    melding             jsonb                                NOT NULL,
    behandlings_id      bigint
);
