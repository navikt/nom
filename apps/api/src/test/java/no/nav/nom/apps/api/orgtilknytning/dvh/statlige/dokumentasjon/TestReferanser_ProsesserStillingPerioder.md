### TESTER: Prosessering av StillingPerioder

* En liste over eksisterende OT’er (OT A, OT B etc)
* En liste over hvordan vi har tolket Unit4 sine stillinger, kalt StillingPeriode (SP A, SP B)
* Og så en liste over de ulike eventene som skal lages for å endre eksisterende OT’er til å se ut som U4 sin representasjon av virkeligheten.

1. Ingen eksisterende Orgtilknytninger
    1. Single StillingPeriode med sluttdato
        ```
        DATO:  2022-01-01               2024-02-01
                    |                        |
        SP A:        <------- A DOL --------->
        ```
       Gir orgtilknytning events:
        ```
        opprettet    A, 2022-01-01
        endretDato   A, 2022-01-01, 2024-31-01
        endretDOL    JA
        ``` 
    2. To StillingPerioder, overlappende med overtakende DOL
        ```
        DATO:  2022-01-01            2023-01-01   2024-02-01
                    |                     |            |
        SP A:       <------- A DOL -------------------->
        SP B:                              <---- B ---->
        SP B:                                           <----- B DOL ------ 
        ```
       Gir orgtilknytning events:
        ```
        opprettet    A, 2022-01-01
        endretDato   A, 2022-01-01, 2024-31-01
        endretDOL    A, JA
        
        opprettet    B, 2023-01-01 
        endretDato   B, 2023-01-01, 2024-31-01
        
        opprettet    B, 2024-02-01
        endretDOL    B, JA
        ``` 
2. Eksisterende OT(er)
   1. Single OT, single StillingPeriode - full match 
        ```
        DATO:  2022-01-01               2024-02-01
                    |                        |
        OT A:        <------- A DOL --------->
      
        SP A:        <------- A DOL --------->
        ```
      Gir orgtilknytning events:
        ```
        INGEN
        ``` 
   2. Single OT, single StillingPeriode ulik DOL
        ```
        DATO:  2022-01-01               2024-02-01
                    |                        |
        OT A:        <---------- A ---------->
      
        SP A:        <------- A DOL --------->
        ```
      Gir orgtilknytning events:
        ```
        endretDOL    A, JA
        ``` 
   3. Single OT, single StillingPeriode ikke match på periode
        ```
        DATO:  2022-01-01            2023-01-01   22024-02-01
                    |                     |            |
        OT A:        <--------------- A --------------->
      
        SP A:        <------- A DOL ------>
        ```
      Gir orgtilknytning events:
        ```
        endretDato   A, 2022-01-01, 2022-12-31 
        endretDOL    A, JA
        ``` 
   4. Single OT, single StillingPeriode ikke match på periode
        ```
        DATO:  2022-01-01        2023-01-01       2024-02-01
                    |                 |                |
        OT A:        <------------- A DOL ------------->
      
        SP A:                          <---- A DOL -----------
        ```
      Gir orgtilknytning events:
        ```
        fjernet      A
      
        opprettet    A, 2023-01-01
        endretDOL    A, JA
       ```
   5. Single OT, to StillingPerioder en match
        ```
        DATO:  2022-01-01        2023-01-01       2024-02-01
                    |                 |                |
        OT A:                          <---- A DOL -----------
      
        SP A:                          <---- A DOL -----------
        SP B:        <---- B DOL ----->
        ```
      Gir orgtilknytning events:
        ``` 
        opprettet    B, 2022-01-01
        endretDato   B, 2022-01-01, 2022-12-31
        endretDOL    B, JA
       ```
   6. Single OT, to StillingPerioder en delvis match
        ```
        DATO:  2022-01-01        2023-01-01       2024-02-01
                    |                 |                |
        OT A:                          <------ A ------>
      
        SP A:                          <---- A DOL -----------
        SP B:        <---- B DOL ----->
        ```
      Gir orgtilknytning events:
        ``` 
        opprettet    B, 2022-01-01
        endretDato   B, 2022-01-01, 2022-12-31
        endretDOL    B, JA
      
        endretDato   A, 2023-01-01, INGEN
        endretDOL    A, JA
       ```
   7. Single OT, to StillingPerioder ny tar over
        ```
        DATO:  2022-01-01        2023-01-01   
                    |                 |       
        OT A:        <------- A DOL----------
      
        SP A:        <---- A DOL ----->
        SP B:                          <---- B DOL -----------
        ```
      Gir orgtilknytning events:
        ``` 
        avviklet     A, 2022-12-31
      
        opprettet    B, 2023-01-01
        endretDOL    B, JA
       ```         
   8. Single OT, to StillingPerioder ny tar over fremtidig
       ```
        DATO:  2022-01-01        Fremtidig sluttdato   
                    |                 |       
        OT A:        <------- A DOL ----------
      
        SP A:        <---- A DOL ----->
        SP B:                          <---- B DOL -----------
       ``` 
      Gir orgtilknytning events:
       ``` 
        endretDato   A, 2022-01-01, Fremtidig sluttdato
      
        opprettet    B, Fremtidig sluttdato + 1 dag
        endretDOL    B, JA
       ```
   9. Flere OT'er, en match på stillingperiode, en delvis match
       ```
        DATO:  2022-01-01       2023-01-01  2024-02-01       Fremtidig sluttdato   
                    |                |           |                 |       
        OT A:        <---- A DOL ---->
        OT B:        <---- B -------------------->
        OT C:                        <----------- C DOL ----------->
      
        SP A:        <---- A DOL ----->
        SP C:                        <----------------- C DOL -----------------
        ```
      Gir orgtilknytning events:
        ``` 
        fjernet      B
      
        endretDato   C, 2023-01-01, ingen
       ```                
   10. Flere OT'er, en match på stillingperiode, en delvis match
       ```
        DATO:  2022-01-01       2023-01-01  2024-02-01          
                    |                |           |                  
        OT C:        <---- A DOL ---->
        OT B:        <---- B DOL ---------------->
        OT C:                                     <----------- C -------------- 
      
        SP A:        <---- B DOL ---->
        SP C:                         <----------------- C DOL -----------------
        ```
      Gir orgtilknytning events:
      ```
        fjernet      A
      
        endretDato   B, 2022-01-01, 2022-12-31

        fjernet      C

        opprettet    C, 2023-01-01
        endretDOL    C, JA
      ```               
