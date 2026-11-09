CREATE TABLE sf_hendelse_behandling
(
    id                      bigint generated always as identity     PRIMARY KEY,
    navident                char(7)                                 NOT NULL,
    endring                 jsonb                                   NOT NULL,
    behandlings_id          bigint
);
