###  E2E - Fra DVH lastet data til generering av events

* En liste over evt eksisterende OT’er (OT R1/A, OT R1/B etc, )
* Og så en liste over de ulike eventene som skal lages for å endre eksisterende OT’er til å se ut som U4 sin representasjon av virkeligheten.
* Datoer er angitt med DD_2Y etc eksempler:
    * DD_2Y  => dagens dato - minus to år første dag i måneden
    * DD_6M => dagens dato - minus 6 måneder første dag i måneden
    * DD_6M_1D => Dagen før DD_6M (altså siste dag i måneden før)

Alle tester har følgende Enheter til gjengelig
```
Enhet A: FID_A a100xy   DD_10Y  -               0AA0001
Enhet B: FID_B b100xy   DD_10Y  -               0BB0001
Enhet C: FID_C c100xy   DD_10Y  -               0CC0001
Enhet D: FID_D1 d100xy  DD_10Y  - DD_1Y_1D      0DD0001
Enhet E: FID_D2 d200xy  DD_1Y  -                0DD0001                  
```
Og ressurser:
```
Ressurs 1: A000001  null - null     NAV_STATLIG
Ressurs 2: B000002  null - null     NAV_STATLIG
```

### Tester

1. En DvhStilling, ingen eksisterende OT
   ```
    DATO:        DD_2Y                    DD_6M
                   |                        |
    DVH R1/A:       <--------- R1/A -------->
  
    SP R1/A:        <------- R1/A DOL ------>
   ```
   Gir orgtilknytning events:
   ```
    opprettet    R1/A, DD_2Y
    endretDato   R1/A, DD_2Y, DD_6M_1D
    endretDOL    R1/A, JA
   ``` 
2. To sammenhengende DvhStillinger, ingen eksisterende OT
   ```
    DATO:        DD_2Y            DD_1Y             DD_6M
                   |                |                 |
    DVH R1/A:       <----- R1/A ---->
    DVH R1/A:                        <----- R1/A ------
  
    SP R1/A:        <----------- R1/A DOL -------------
   ```
   Gir orgtilknytning events:
   ```
    opprettet    R1/A, DD_2Y
    endretDOL    R1/A, JA
   ``` 
3. To sammenhengende DvhStillinger, matcher eksisterende OT
   ```
    DATO:        DD_2Y            DD_1Y             DD_6M
                   |                |                 |
    DVH R1/A:       <----- R1/A ---->
    DVH R1/A:                        <----- R1/A ------

    SP R1/A:        <----------- R1/A DOL -------------
   
    OT R1/A:        <----------- R1/A DOL ------------- 
   ```
   Gir orgtilknytning events:
   ```
    INGEN
   ```
4. En DvhStilling, matcher med agressoId mot to Enheter
   ```
    DATO:        DD_2Y                DD_1Y             DD_6M
                   |                    |                 |
    DVH R1/D1:      <-------- R1/D1 - 0DD0001 ---------------

    SP R1/D1:       <---- R1/D1 DOL ---->
    SP R1/D2:                            <---- R1/D2 DOL ---- 
   ```
   Gir orgtilknytning events:
   ```
    opprettet    R1/D1, DD_2Y
    endretDato   R1/D1, DD_2Y, DD_1Y_1D
    endretDOL    R1/D1, JA

    opprettet    R1/D2, DD_1Y
    endretDOL    R1/D2, JA
   ```
5. To DvhStillinger, en matcher med agressoId mot to Enheter, en matcher ikke Orgenhet På Ressurs, ulike OTer
   ```
    ORGENHET PÅ RESSURS: 0DD0001
   
    DATO:        DD_3Y               DD_2Y                 DD_1Y             DD_6M
                   |                   |                     |                 |
    DVH R1/A:       <----------------- R1/A ----------------->
    DVH R1/A:                                                 <----- R1/A ----->
    DVH R1/D1:                          <-------- R1/D1 - 0DD0001 ------------------

    SP R1/A:        <---- R1/A DOL ---->
    SP R1/A:                            <------------------ R1/A -------------->
    SP R1/D1:                           <---- R1/D1 DOL ---->
    SP R1/D2:                                                <---- R1/D2 DOL --------
   
    OT R1/A:        <----------- R1/A DOL ------------------> 
    OT R1/B:                                                 <---- R1/B DOL ---------  
   ```
   Gir orgtilknytning events:
   ```
    endretDato   R1/A, DD_3Y, DD_2Y_1D

    opprettet    R1/D1, DD_2Y
    endretDato   R1/D1, DD_2Y, DD_1Y_1D
    endretDOL    R1/D1, JA

    opprettet    R1/A, DD_2Y
    endretDato   R1/A, DD_2Y, DD_6M_1D

    opprettet    R1/D2, DD_1Y
    endretDOL    R1/D2, JA
   
    fjernet      R1/B
   ```   
6. Tre DvhStillinger, to med samme sektor, en uten, men de to med sektor har forskjellige tilknytningtyper
   ```
    ORGENHET PÅ RESSURS: 0AA0001
    TRE DVH STILLINGER MED 3 FORSKJELLIGE AnsattTypeBesk
   
    DATO:        DD_3Y                DD_1Y                DD_6M
                   |                    |                    |
    DVH R1/A:      <------ R1/A ------>                                             INGEN ANSATTTYPEBESK
    DVH R1/A:                           <------ R1/A ------>                        ANSATTTYPEBESK Midlertidig karakter § 9a
    DVH R1/A:                                                 <------ R1/A ------   ANSATTTYPEBESK Fast
   
    SP R1/A:       <---- R1/A DOL ---->                                             
    SP R1/A:                           <---- R1/A DOL ---->                         
    SP R1/A:                                                 <---- R1/A DOL -----   
   ```
    Gir orgtilknytning events:
    ```
    opprettet          R1/A, DD_3Y
    endreDol           R1/A, JA
    endreDato          R1/A, DD_3Y, DD_1Y_1D
   
    opprettet          R1/A, DD_1Y
    endreDol           R1/A, JA
    endreDatot         R1/A, DD_1Y, DD_6M_1D
   
    opprettet          R1/A, DD_6M
    endreDol           R1/A, JA
    endreTilknytning   R1/A, FAST
    endreSektor        R1/A, NAV_STATLIG
    ```

7. To DvhStillinger, en matcher med agressoId mot to Enheter, en matcher ikke Orgenhet På Ressurs, ulike OTer, like sektor/tilknytning
   ```
    ORGENHET PÅ RESSURS: 0DD0001
   
    DATO:        DD_3Y               DD_2Y                 DD_1Y             DD_6M
                   |                   |                     |                 |
    DVH R1/A:       <----------------- R1/A ----------------->                         ANSATTTYPEBESK Fast
    DVH R1/A:                                                 <----- R1/A ----->       ANSATTTYPEBESK Fast
    DVH R1/D1:                          <-------- R1/D1 - 0DD0001 ------------------   ANSATTTYPEBESK Fast

    SP R1/A:        <---- R1/A DOL ---->
    SP R1/A:                            <------------------ R1/A -------------->
    SP R1/D1:                           <---- R1/D1 DOL ---->
    SP R1/D2:                                                <---- R1/D2 DOL --------
   
    OT R1/A:        <----------- R1/A DOL ------------------>                          Sektor: NAV_STATLIG, Tilknytning: FAST
    OT R1/B:                                                 <---- R1/B DOL ---------  Sektor: NAV_STATLIG, Tilknytning: MIDLERTIDIG
   ```
   Gir orgtilknytning events:
   ```
    endretDato         R1/A, DD_3Y, DD_2Y_1D

    opprettet          R1/D1, DD_2Y
    endretDato         R1/D1, DD_2Y, DD_1Y_1D
    endretDOL          R1/D1, JA

    opprettet          R1/A, DD_2Y
    endretDato         R1/A, DD_2Y, DD_6M_1D

    opprettet          R1/D2, DD_1Y
    endretDOL          R1/D2, JA
    endreTilknytning   R1/D2, FAST
    endreSektor        R1/D2, NAV_STATLIG
   
    fjernet            R1/B
   ```   
