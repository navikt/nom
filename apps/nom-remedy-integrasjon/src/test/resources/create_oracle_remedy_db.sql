create table if not exists NOM_INTEGRASJON
(
    ENTRY_ID               VARCHAR2(15),
    ENTRY_STATUS           VARCHAR2(255),
    ENTRY_OPERASJON        VARCHAR2(255),
    ENTRY_BESTILLER        VARCHAR2(255),
    TEMP_CHAR1             VARCHAR2(255),
    TEMP_CHAR2             VARCHAR2(255),
    TEMP_INTEGER           NUMBER(15),
    FORNAVN                VARCHAR2(30),
    ETTERNAVN              VARCHAR2(30),
    NAV_ID                 VARCHAR2(7),
    FODSELSNUMMER          VARCHAR2(11),
    SEKTOR                 NUMBER,
    ENHETS_ID              VARCHAR2(255),
    ENHETSNAVN             VARCHAR2(255),
    FYSISK_BRUKERSTED_ID   VARCHAR2(255),
    FYSISK_BRUKERSTED_NAVN VARCHAR2(255),
    TILTREDELSESDATO       NUMBER,
    UTLOPSDATO             NUMBER,
    MOBIL                  VARCHAR2(15)
);