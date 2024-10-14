###  E2E - Fra DVH lastet data til generering av events

* En liste over evt eksisterende OT’er (OT R1/A, OT R1/B etc, )
* Og så en liste over de ulike eventene som skal lages for å endre eksisterende OT’er til å se ut som U4 sin representasjon av virkeligheten.

Alle tester har følgende Enheter til gjengelig
```
Enhet A: FID_A a100xy   2015-01-01  -               0AA0001
Enhet B: FID_B b100xy   2015-01-01  -               0BB0001
Enhet C: FID_C c100xy   2015-01-01  -               0CC0001
Enhet D: FID_D1 d100xy  2015-01-01  - 2023-31-01    0DD0001
Enhet E: FID_D2 d200xy  2023-02-01  -               0DD0001                  
```
Og ressurser:
```
Ressurs 1: A000001  null - null     NAV_STATLIG
Ressurs 2: B000002  null - null     NAV_STATLIG
```

### Tester

1. En DvhStilling, ingen eksisterende OT
   ```
    DATO:     2022-01-01               2024-02-01
                   |                        |
    DVH R1/A:       <--------- R1/A -------->
  
    SP R1/A:        <------- R1/A DOL ------>
   ```
   Gir orgtilknytning events:
   ```
    opprettet    R1/A, 2022-01-01
    endretDato   R1/A, 2022-01-01, 2024-31-01
    endretDOL    R1/A, JA
   ``` 
2. To sammenhengende DvhStillinger, ingen eksisterende OT
   ```
    DATO:     2022-01-01       2023-02-01        2024-02-01
                   |                |                 |
    DVH R1/A:       <----- R1/A ---->
    DVH R1/A:                        <----- R1/A ------
  
    SP R1/A:        <----------- R1/A DOL -------------
   ```
   Gir orgtilknytning events:
   ```
    opprettet    R1/A, 2022-01-01
    endretDato   R1/A, 2022-01-01, 2024-31-01
    endretDOL    R1/A, JA
   ``` 
3. To sammenhengende DvhStillinger, matcher eksisterende OT
   ```
    DATO:     2022-01-01       2023-02-01        2024-02-01
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
    DATO:     2022-01-01           2023-02-01        2024-02-01
                   |                    |                 |
    DVH R1/D1:      <-------- R1/D1 - 0DD0001 ---------------

    SP R1/D1:       <---- R1/D1 DOL ---->
    SP R1/D2:                            <---- R1/D2 DOL ---- 
   ```
   Gir orgtilknytning events:
   ```
    opprettet    R1/D1, 2022-01-01
    endretDato   R1/D1, 2022-01-01, 2023-01-31
    endretDOL    R1/D1, JA

    opprettet    R1/D2, 2023-02-01
    endretDOL    R1/D2, JA
   ```
5. To DvhStillinger, en matcher med agressoId mot to Enheter, en matcher ikke Orgenhet På Ressurs, ulike OTer
   ```
    ORGENHET PÅ RESSURS: 0DD0001
   
    DATO:     2022-01-01          2022-05-01            2023-02-01        2024-02-01
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
    endretDato   R1/A, 2022-01-01, 2022-04-30

    opprettet    R1/A, 2022-05-01
    endretDato   R1/A, 2022-05-01, 2024-01-31

    opprettet    R1/D1, 2022-05-01
    endretDato   R1/D1, 2022-05-01, 2023-01-31
    endretDOL    R1/D1, JA

    opprettet    R1/D2, 2023-02-01 
    endretDOL    R1/D2, JA
   
    fjernet      R1/B
   ```   