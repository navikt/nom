CREATE TABLE T_GQL_QUERY (
    id      SERIAL  NOT NULL,
    hash    TEXT    NOT NULL    UNIQUE,
    query   TEXT,

    PRIMARY KEY (id)
);

CREATE INDEX idx_t_gql_query_hash ON T_GQL_QUERY (hash);


CREATE TABLE T_GQL_QUERY_USAGE (
    id          SERIAL  NOT NULL,
    hash        TEXT    NOT NULL,
    consumer    TEXT    NOT NULL,
    timestamp   TIMESTAMP WITHOUT TIME ZONE,

    PRIMARY KEY (id)
);

CREATE INDEX idx_t_gql_query_usage_hash ON T_GQL_QUERY_USAGE (hash);


CREATE TABLE T_GQL_FIELD_USAGE (
    id              SERIAL  NOT NULL,
    field           TEXT    NOT NULL,
    path            TEXT    NOT NULL,
    operation_name  TEXT    NOT NULL,
    consumer        TEXT    NOT NULL,
    timestamp       TIMESTAMP WITHOUT TIME ZONE,

    PRIMARY KEY (id)
);

CREATE INDEX idx_t_gql_field_usage_field ON T_GQL_QUERY_USAGE (hash);
CREATE INDEX idx_t_gql_field_usage_path ON T_GQL_QUERY_USAGE (hash);
