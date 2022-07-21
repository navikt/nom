CREATE TABLE SOURCE_EVENT (
    ID              BIGINT          PRIMARY KEY,
    EVENT_TYPE      VARCHAR(8)      NOT NULL,
    EVENT_TS        TIMESTAMP       NOT NULL,
    BESTILLER       VARCHAR(16)     NOT NULL,
    NAVIDENT        CHAR(8)         NOT NULL,
    PERSONID        CHAR(8)         NOT NULL,
    IDENT_TYPE      VARCHAR(32)     NOT NULL
);
