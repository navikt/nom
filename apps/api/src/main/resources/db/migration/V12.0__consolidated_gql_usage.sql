CREATE INDEX idx_t_gql_field_usage_timestamp ON T_GQL_FIELD_USAGE(timestamp);
CREATE INDEX idx_t_gql_query_usage_timestamp ON T_GQL_QUERY_USAGE(timestamp);
CREATE INDEX idx_t_gql_query_usage_consumer ON T_GQL_QUERY_USAGE (consumer);

CREATE TABLE T_GQL_CONSOLIDATED_FIELD_USAGE (
    id                  SERIAL  NOT NULL,
    field_path_array    JSONB   NOT NULL,
    consumer            TEXT    NOT NULL,
    year                INT     NOT NULL,
    month               INT     NOT NULL,
    day                 INT     NOT NULL,
    count               INT     NOT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX idx_t_gql_consolidated_field_usage_consumer ON T_GQL_CONSOLIDATED_FIELD_USAGE(consumer);
CREATE INDEX idx_t_gql_consolidated_field_usage_date ON T_GQL_CONSOLIDATED_FIELD_USAGE(year, month, day);


CREATE TABLE T_GQL_CONSOLIDATED_QUERY_USAGE (
    id                  SERIAL  NOT NULL,
    hash                TEXT  NOT NULL,
    consumer            TEXT    NOT NULL,
    year                INT     NOT NULL,
    month               INT     NOT NULL,
    day                 INT     NOT NULL,
    count               INT     NOT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX idx_t_gql_consolidated_query_usage_hash ON T_GQL_CONSOLIDATED_QUERY_USAGE(hash);
CREATE INDEX idx_t_gql_consolidated_query_usage_consumer ON T_GQL_CONSOLIDATED_QUERY_USAGE(consumer);
CREATE INDEX idx_t_gql_consolidated_query_usage_date ON T_GQL_CONSOLIDATED_QUERY_USAGE(year, month, day);
