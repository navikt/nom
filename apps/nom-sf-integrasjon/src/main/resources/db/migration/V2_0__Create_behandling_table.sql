CREATE TABLE behandling
(
    id                      bigint generated always as identity         PRIMARY KEY,
    behandlings_id          bigint                                      NOT NULL,
    status                  text                                        NOT NULL,
    behandling_tidspunkt     timestamp with time zone                   NOT NULL
);

CREATE SEQUENCE behandlings_id;
