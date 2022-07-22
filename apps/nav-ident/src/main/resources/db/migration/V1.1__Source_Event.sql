CREATE SEQUENCE EVENT_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE SOURCE_EVENT (
      ID              BIGINT          PRIMARY KEY,
      EVENT_TYPE      TEXT            NOT NULL,
      EVENT_TS        TIMESTAMP       NOT NULL,
      BESTILLER       TEXT            NOT NULL,
      NAVIDENT        TEXT            NOT NULL,
      PERSONID        TEXT            NOT NULL,
      IDENT_TYPE      TEXT            NOT NULL
);
