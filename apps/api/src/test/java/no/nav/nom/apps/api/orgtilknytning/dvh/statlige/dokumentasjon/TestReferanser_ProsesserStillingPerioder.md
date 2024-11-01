### TESTER: Prosessering av StillingPerioder

* En liste over eksisterende OT’er (OT A, OT B etc)
* En liste over hvordan vi har tolket Unit4 sine stillinger, kalt StillingPeriode (SP A, SP B)
* Og så en liste over de ulike eventene som skal lages for å endre eksisterende OT’er til å se ut som U4 sin representasjon av virkeligheten.
* Datoer er angitt med DD_2Y etc eksempler: 
  * DD_2Y  => dagens dato - minus to år første dag i måneden
  * DD_6M => dagens dato - minus 6 måneder første dag i måneden
  * DD_6M_1D => Dagen før DD_6M (altså siste dag i måneden før)

1. Ingen eksisterende Orgtilknytninger
    1. Single StillingPeriode med sluttdato
        ```
        DATO:     DD_2Y                    DD_6M
                    |                        |
        SP A:        <------- A DOL --------->
        ```
       Gir orgtilknytning events:
        ```
        opprettet    A, DD_2Y
        endretDato   A, DD_2Y, DD_6M_1D
        endretDOL    JA
        ``` 
    2. To StillingPerioder, overlappende med overtakende DOL
        ```
        DATO:     DD_2Y                 DD_1Y        DD_6M
                    |                     |            |
        SP A:       <------- A DOL -------------------->
        SP B:                              <---- B ---->
        SP B:                                           <----- B DOL ------ 
        ```
       Gir orgtilknytning events:
        ```
        opprettet    A, DD_2Y
        endretDato   A, DD_2Y, DD_6M_1D
        endretDOL    A, JA
        
        opprettet    B, DD_1Y  
        endretDato   B, DD_1Y, DD_6M_1D
        
        opprettet    B, DD_6M
        endretDOL    B, JA
        ``` 
2. Eksisterende OT(er)
   1. Single OT, single StillingPeriode - full match 
        ```
        DATO:     DD_2Y                    DD_6M
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
        DATO:     DD_2Y                    DD_6M
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
        DATO:     DD_2Y                 DD_1Y        DD_6M
                    |                     |            |
        OT A:        <--------------- A --------------->
      
        SP A:        <------- A DOL ------>
        ```
      Gir orgtilknytning events:
        ```
        endretDato   A, DD_2Y, DD_1Y_1D
        endretDOL    A, JA
        ``` 
   4. Single OT, single StillingPeriode ikke match på periode
        ```
        DATO:     DD_2Y                 DD_1Y        DD_6M
                    |                 |                |
        OT A:        <------------- A DOL ------------->
      
        SP A:                          <---- A DOL -----------
        ```
      Gir orgtilknytning events:
        ```
        fjernet      A
      
        opprettet    A, DD_1Y
        endretDOL    A, JA
       ```
   5. Single OT, to StillingPerioder en match
        ```
        DATO:     DD_2Y             DD_1Y            DD_6M
                    |                 |                |
        OT A:                          <---- A DOL -----------
      
        SP A:                          <---- A DOL -----------
        SP B:        <---- B DOL ----->
        ```
      Gir orgtilknytning events:
        ``` 
        opprettet    B, DD_2Y
        endretDato   B, DD_2Y, DD_1Y_1D
        endretDOL    B, JA
       ```
   6. Single OT, to StillingPerioder en delvis match
        ```
        DATO:     DD_2Y             DD_1Y            DD_6M
                    |                 |                |
        OT A:                          <------ A ------>
      
        SP A:                          <---- A DOL -----------
        SP B:        <---- B DOL ----->
        ```
      Gir orgtilknytning events:
        ``` 
        opprettet    B, DD_2Y
        endretDato   B, DD_2Y, DD_1Y_1D
        endretDOL    B, JA
      
        endretDato   A, DD_1Y, INGEN
        endretDOL    A, JA
       ```
   7. Single OT, to StillingPerioder ny tar over
        ```
        DATO:     DD_2Y             DD_1Y     
                    |                 |       
        OT A:        <------- A DOL----------
      
        SP A:        <---- A DOL ----->
        SP B:                          <---- B DOL -----------
        ```
      Gir orgtilknytning events:
        ``` 
        avviklet     A, DD_1Y_1D
      
        opprettet    B, DD_1Y
        endretDOL    B, JA
       ```         
   8. Single OT, to StillingPerioder ny tar over fremtidig
       ```
        DATO:     DD_2Y        Fremtidig sluttdato   
                    |                 |       
        OT A:        <------- A DOL ----------
      
        SP A:        <---- A DOL ----->
        SP B:                          <---- B DOL -----------
       ``` 
      Gir orgtilknytning events:
       ``` 
        endretDato   A, DD_2Y, Fremtidig sluttdato
      
        opprettet    B, Fremtidig sluttdato + 1 dag
        endretDOL    B, JA
       ```
   9. Flere OT'er, en match på stillingperiode, en delvis match
       ```
        DATO:     DD_2Y            DD_1Y       DD_6M       Fremtidig sluttdato   
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
      
        endretDato   C, DD_1Y, ingen
       ```                
   10. Flere OT'er, en match på stillingperiode, en delvis match
       ```
        DATO:     DD-2Y            DD-15M      DD-6M          
                    |                |           |                  
        OT A:        <---- A DOL ---->
        OT B:        <---- B DOL ---------------->
        OT C:                                     <----------- C -------------- 
      
        SP A:        <---- B DOL ---->
        SP C:                         <----------------- C DOL -----------------
        ```
       Gir orgtilknytning events:
        ```
         fjernet      A
      
         endretDato   B, DD-2Y, DD-15M

         fjernet      C

         opprettet    C, DD-15M
         endretDOL    C, JA
        ```               
   11. Match på Eksisterende OT som er +3år gammel
       ```
        DATO:     DD-3Y2M    DD_3Y            DD_2Y                 
                    |          |                |                           
        OT A:        <--------- A DOL ---------->
        OT B:                                    <---- B DOL ----------------
      
        SP A:                   <---- A DOL ---->    // Denne har fått klippet bort historikk som er +3år 
        SP B:                                    <---- B DOL ----------------
        ```
       Gir orgtilknytning events:
        ```
         INGEN
        ```
   12. Match på Eksisterende OT som er +3år gammel
        ```
         DATO:     DD-5Y              DD_4Y                 
                     |                  |                           
         OT A:        <----- A DOL ----->

         INGEN SP      
         ```
        Gir orgtilknytning events:
         ```
          INGEN
         ```       