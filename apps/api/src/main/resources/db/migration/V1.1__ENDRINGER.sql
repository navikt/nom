-- funksjonell_id omdøpes til entitet_id
-- forespørsel_id omdøpes til ordre_id
-- (?) legge til kolonne for ordre_offset (indeks/rekkefølge for ordrelinja innad i ordren
-- omdøpe tabellnavn T_Hendelser til T_Ordrelinje ?
-- omdøpe kolonne data_type til entitet_type


-- akkumuler alle hendelser/ordre-linjer fra "approved" ordrer. Dette er det offisielle nå-bildet, "global state"
-- select * from T_hendelser where ordre_id in (select ordre_id from T_ordre where status = 'APPROVED');

-- dette blir å se på branch. akkumuler alle hendelser/ordre-linjer fra approved ordrer, pluss en til vi ser på som "branch"
-- select * from T_hendelser where ordre_id in (select ordre_id from T_ordre where status = 'APPROVED' OR ordre_id = 'den ordren vi skal se på nå');

-- SELECT * FROM T_HENDELSER WHERE APPROVAL_DATE IS NOT NULL OR ORDRE_ID = 'DEN VI SKAL SE PÅ'

ALTER TABLE T_HENDELSER
RENAME COLUMN FORESPORSEL_ID TO ORDRE_ID;

CREATE TABLE T_ORDRE (
    ORDRE_ID                UUID        PRIMARY KEY,
    DATA                    JSONB       NOT NULL
);

CREATE SEQUENCE NAVIDENT_SEQ MINVALUE 0 MAXVALUE 999999
    START WITH 0 INCREMENT BY 1 NO CYCLE;








