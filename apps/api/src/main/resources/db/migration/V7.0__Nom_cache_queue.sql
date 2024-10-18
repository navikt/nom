CREATE TABLE T_CACHE_QUEUE (
    CACHE_KEY           TEXT         NOT NULL,
    PAYLOAD             JSONB        NOT NULL,
    CACHE_TYPE          TEXT         NOT NULL,
    constraint PK_T_CACHE_QUEUE primary key (CACHE_KEY, CACHE_TYPE)
);