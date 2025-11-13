DROP INDEX idx_t_gql_field_usage_field; -- Disse var laget for feil tabell..
DROP INDEX idx_t_gql_field_usage_path;

DROP TABLE T_GQL_FIELD_USAGE;

CREATE TABLE T_GQL_FIELD_USAGE (
    id                  SERIAL  NOT NULL,
    field_path_array    JSONB   NOT NULL,
    operation_name      TEXT    NOT NULL,
    consumer            TEXT    NOT NULL,
    timestamp           TIMESTAMP WITHOUT TIME ZONE,

    PRIMARY KEY (id)
);

CREATE INDEX idx_t_gql_field_consumer ON T_GQL_FIELD_USAGE(consumer);
