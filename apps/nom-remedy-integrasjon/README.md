# Nom Remedy Integrasjon

Konsumerer kafka og skriver til en integrasjonstabell hos Remedy.

Felter i integrasjonstabell
```
ENTRY_ID (VARCHAR2(15))
ENTRY_STATUS (VARCHAR2(255))
ENTRY_OPERASJON (VARCHAR2(255))
ENTRY_BESTILLER (VARCHAR(255))
TEMP CHAR1 (VARCHAR2(255))
TEMP CHAR 2 (VARCHAR2(255))
TEMP INTEGER (NUMBER(15))
FORNAVN (VARCHAR2(30))
ETTERNAVN (VARCHAR2(30))
NAV_ID (VARCHAR2(7))
FØDSELSNUMMER (VARCHAR2(11))
SEKTOR (NUMBER)
ENHETS_ID (VARCHAR2(255))
ENHETSNAVN (VARCHAR2(255))
FYSISK_BRUKERSTED_ID (VARCHAR2(255))
FYSISK_BRUKERSTED_NAVN (VARCHAR2(255))
TILTREDELSESDATO (NUMBER)
UTLØPSDATO (NUMBER)
MOBIL (VARCHAR2(15))
```

Beskrivelse av felter fra team-idm (https://nav-it.slack.com/archives/C040RV7KCH5/p169649842673905)
```
ENTRY_ID økes stegvis for hver ny rad som NOM skriver. Kan gjerne begynne på '000000000000001'. Hver verdi må være unik.
ENTRY_STATUS settes alltid til verdien 'Ubehandlet'.
ENTRY_BESTILLER settes lik NAV-ID til den lederen som utfører bestillingen i NOM.
SEKTOR settes til:
  0 for statlig ansatte
  1 for kommunalt ansatte
  2 for eksternt ansatte
FORNAVN og ETTERNAVN skal ha stor forbokstav i hvert ord, og resten små bokstaver.
(Eksempel: Anne-Lise Berg Hansen)
TILTREDELSESDATO skal inneholde en tallverdi som representerer antall sekunder siden 01.01.1970.
UTLØPSDATO skal stå blank for statlige og kommunalt ansatte. For eksterne skal den inneholde en tallverdi som representerer antall sekunder siden 01.01.1970. Utløpsdatoen kan maksimalt settes til ett åt fra dagens dato.
ENTRY_OPERASJON settes til verdien 'Slette' hvis ansettelsesforholdet til en eksisterende identitet opphører.
ENTRY_OPERASJON settes til verdien 'Ny' i alle andre tilfeller
```