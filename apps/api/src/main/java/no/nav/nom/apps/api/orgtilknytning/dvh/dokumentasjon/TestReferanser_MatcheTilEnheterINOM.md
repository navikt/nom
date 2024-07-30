### TESTER: Matche til enhet i NOM

1. Stillingens AgressoId er 'presis' Det vil si 7 eller flere sifre
    1. SINGLE match - Presis enhet på stillingens agressoId matcher kun en enhet i NOM
        2. Perioden til enheten spenner over hele stillingsperioden - ingen klipp av StillingPeriode, og enhet funnet
            ```
             DvhRessursStilling.stillingOrgenhetNavn er satt                     "Single match"
             DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn          "0110001"
             DvhRessursStilling.ressursAgressoId     er lik DvhRessursStilling.stillingAgressoId  
             DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
            ```
            ``` 
             nom_enehter[{
                 "id": "a123bc",
                 "agressoId": "0110001",
                 "navn": "Single match",
                 "orgNivå": "ORGENHET",
                 "gyldigFom": "2022-01-01"
             }]
            ```
            1. ```StillingPeriode.GyldigFom = "2022-01-01"```
            2. ```StillingPeriode.GyldigFom = "2023-01-01"```
            3. ```StillingPeriode.GyldigFom = "2023-01-01", StillingPeriode.GyldigTom = "2024-01-31"```

        3. Perioden til enheten spenner ikke over hele stillingsperioden - sjekk at dette varsles som feil
            ```
             DvhRessursStilling.stillingOrgenhetNavn er satt                     "Single match"
             DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn          "0110001"
             DvhRessursStilling.ressursAgressoId     er lik DvhRessursStilling.stillingAgressoId  
             DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
            ```
            ``` 
             nom_enehter[{
                 "id": "a123bc",
                 "agressoId": "0110001",
                 "navn": "Single match",
                 "orgNivå": "ORGENHET",
                 "gyldigFom": "2022-01-01",
                 "gyldigTom": "2024-12-31"
             }]
            ```
            1. ```StillingPeriode.GyldigFom = "2021-01-01", StillingPeriode.GyldigTom = "2024-01-31"```
            2. ```StillingPeriode.GyldigFom = "2021-01-01", StillingPeriode.GyldigTom = "2026-01-31"```
            3. ```StillingPeriode.GyldigFom = "2023-01-01", StillingPeriode.GyldigTom = "2026-01-31"```
        4. Perioden til enheten spenner ikke over hele stillingsperioden - match mot eksisterede OT'er
            ```
             DvhRessursStilling.stillingOrgenhetNavn er satt                     "Single match"
             DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn          "0110001"
             DvhRessursStilling.ressursAgressoId     er lik DvhRessursStilling.stillingAgressoId  
             DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
            ```
            ``` 
             nom_enehter[{
                 "id": "a123bc",
                 "agressoId": "0110001",
                 "navn": "Single match",
                 "orgNivå": "ORGENHET",
                 "gyldigFom": "2022-01-01",
                 "gyldigTom": "2024-12-31"
             }]
            ```
            ```
             StillingPeriode.GyldigFom = "2021-01-01"
             StillingPeriode.GyldigTom = "2024-01-31"
            ```
            1. Eksisterende OT finnes som matcher StillingPerioden - benytt hele perioden til StillingPerioden
            ``` 
             Orgtilknytning.orgEnhetId = "a123bc",
             Orgtilknytning.GyldigFom = "2021-01-01",
             Orgtilknytning.GyldigTom = "2024-01-31",
            ``` 
            2. Eksisterende OT finnes men matcher ikke hele StillingPerioden, men allikevel mer enn enhetens periode - benytt kombinasjon av enhetens periode og eksisterende OT's periode
                1. Kombinasjonen dekker hele StillingsPerioden
                 ``` 
                  Orgtilknytning.orgEnhetId = "a123bc",
                  Orgtilknytning.GyldigFom = "2021-01-01",
                  Orgtilknytning.GyldigTom = "2023-01-31",
                ```
                2. Kombinasjonen dekker ikke hele StillingsPerioden - Klipp StillingsPeriode
                ``` 
                 Orgtilknytning.orgEnhetId = "a123bc",
                 Orgtilknytning.GyldigFom = "2021-05-01",
                 Orgtilknytning.GyldigTom = "2025-01-31",
                ```
               Ny StillingsPeriode:
                ```
                  StillingPeriode.GyldigFom = "2021-05-01"
                  StillingPeriode.GyldigTom = "2024-01-31
                ```
                3. Kombinasjonen dekker ikke hele StillingsPerioden - Klipp StillingsPeriode
                ``` 
                 Orgtilknytning.orgEnhetId = "a123bc",
                 Orgtilknytning.GyldigFom = "2021-05-01",
                 Orgtilknytning.GyldigTom = "2023-01-31",
                ```
               Ny StillingsPeriode:
                ```
                  StillingPeriode.GyldigFom = "2021-05-01"
                  StillingPeriode.GyldigTom = "2024-01-31
                ```
            3. Matcher ingen eksisterende OT - Klipp StillingsPeriode til enhet
               Ny StillingsPeriode:
                ```
                  StillingPeriode.GyldigFom = "2022-01-01"
                  StillingPeriode.GyldigTom = "2024-01-31
                ```

    2. MULTI match - Presis enhet på stillingens agressoId matcher til flere enheter i NOM
        1. Perioden til enhetene spenner over hele stillingsperioden - ingen klipp av StillingPeriode, og enhet funnet. Splitt StillingPerioden over enhetene.
            ```
             DvhRessursStilling.stillingOrgenhetNavn er satt                     "Nytt navn"
             DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn          "0110001"
             DvhRessursStilling.ressursAgressoId     er lik DvhRessursStilling.stillingAgressoId  
             DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
            ```
            ``` 
             nom_enehter[{
                 "id": "a123bc",
                 "agressoId": "0110001",
                 "navn": "Gammelt navn",
                 "orgNivå": "ORGENHET",
                 "gyldigFom": "2022-01-01",
                 "gyldigTom": "2022-12-31"
             }, {
                 "id": "c321ba",
                 "agressoId": "0110001",
                 "navn": "Nytt navn",
                 "orgNivå": "ORGENHET",
                 "gyldigFom": "2023-01-01"
             }]
            ```
            1. ```StillingPeriode.GyldigFom = "2022-01-01"```
               Nye StillingsPerioder:
              ```
                StillingPeriode.enhet = "a123bc",
                StillingPeriode.GyldigFom = "2022-01-01"
                StillingPeriode.GyldigTom = "2022-12-31"
           
                StillingPeriode.enhet = "c321ba",
                StillingPeriode.GyldigFom = "2023-01-01"
                StillingPeriode.GyldigTom = 
    
              ```
            2. ```StillingPeriode.GyldigFom = "2022-05-01"```
               Nye StillingsPerioder:
              ```
                StillingPeriode.enhet = "a123bc",
                StillingPeriode.GyldigFom = "2022-05-01"
                StillingPeriode.GyldigTom = "2022-12-31"
           
                StillingPeriode.enhet = "c321ba",
                StillingPeriode.GyldigFom = "2023-01-01"
                StillingPeriode.GyldigTom = 
    
              ```        
            3. ```StillingPeriode.GyldigFom = "2022-05-01", StillingPeriode.GyldigTom = "2024-01-31"```
               Nye StillingsPerioder:
               ```
                 StillingPeriode.enhet = "a123bc",
                 StillingPeriode.GyldigFom = "2022-05-01"
                 StillingPeriode.GyldigTom = "2022-12-31"
            
                 StillingPeriode.enhet = "c321ba",
                 StillingPeriode.GyldigFom = "2023-01-01"
                 StillingPeriode.GyldigTom = "2024-01-31"
     
               ```        
        2. Perioden til enhetene overlapper men spenner over hele stillingsperioden - ingen klipp av StillingPeriode, og enhet funnet. Splitt StillingPerioden over enhetene. Benytt 'yngste' enhets periode, varsle om dette.
            ```
             DvhRessursStilling.stillingOrgenhetNavn er satt                     "Nytt navn"
             DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn          "0110001"
             DvhRessursStilling.ressursAgressoId     er lik DvhRessursStilling.stillingAgressoId  
             DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
            ```
            ``` 
             nom_enehter[{
                 "id": "a123bc",
                 "agressoId": "0110001",
                 "navn": "Gammelt navn",
                 "orgNivå": "ORGENHET",
                 "gyldigFom": "2022-01-01",
                 "gyldigTom": "2022-12-31"
             }, {
                 "id": "c321ba",
                 "agressoId": "0110001",
                 "navn": "Nytt navn",
                 "orgNivå": "ORGENHET",
                 "gyldigFom": "2022-07-01"
             }]
            ```
            1. ```StillingPeriode.GyldigFom = "2022-01-01"```
               Nye StillingsPerioder:
              ```
                StillingPeriode.enhet = "a123bc",
                StillingPeriode.GyldigFom = "2022-01-01"
                StillingPeriode.GyldigTom = "2022-06-30"
           
                StillingPeriode.enhet = "c321ba",
                StillingPeriode.GyldigFom = "2022-07-01"
                StillingPeriode.GyldigTom = 
     
              ```
            2. ```StillingPeriode.GyldigFom = "2022-05-01"```
               Nye StillingsPerioder:
              ```
                StillingPeriode.enhet = "a123bc",
                StillingPeriode.GyldigFom = "2022-05-01"
                StillingPeriode.GyldigTom = "2022-06-30"
           
                StillingPeriode.enhet = "c321ba",
                StillingPeriode.GyldigFom = "2022-07-01"
                StillingPeriode.GyldigTom = 
     
              ```        
            3. ```StillingPeriode.GyldigFom = "2022-05-01", StillingPeriode.GyldigTom = "2024-01-31"```
               Nye StillingsPerioder:
               ```
                 StillingPeriode.enhet = "a123bc",
                 StillingPeriode.GyldigFom = "2022-05-01"
                 StillingPeriode.GyldigTom = "2022-06-30"
            
                 StillingPeriode.enhet = "c321ba",
                 StillingPeriode.GyldigFom = "2022-07-01"
                 StillingPeriode.GyldigTom = "2024-01-31"
     
               ```        
            3. ```StillingPeriode.GyldigFom = "2022-08-01", StillingPeriode.GyldigTom = "2024-01-31"```
               MERK: Match på flere enheter, men kun 'yngste' enhetens periode mathcer hele StillingPerioden, ingen splitt
                ```
                  StillingPeriode.enhet = "c321ba",
                  StillingPeriode.GyldigFom = "2022-08-01"
                  StillingPeriode.GyldigTom = "2024-01-31"
      
                ```        
        3. Perioden til enhetene spenner ikke over hele stillingsperioden - sjekk at dette varsles som feil
            ```
             DvhRessursStilling.stillingOrgenhetNavn er satt                     "Nytt navn"
             DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn          "0110001"
             DvhRessursStilling.ressursAgressoId     er lik DvhRessursStilling.stillingAgressoId  
             DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
            ```
            ``` 
             nom_enehter[{
                 "id": "a123bc",
                 "agressoId": "0110001",
                 "navn": "Gammelt navn",
                 "orgNivå": "ORGENHET",
                 "gyldigFom": "2022-01-01",
                 "gyldigTom": "2022-12-31"
             }, {
                 "id": "c321ba",
                 "agressoId": "0110001",
                 "navn": "Nytt navn",
                 "orgNivå": "ORGENHET",
                 "gyldigFom": "2022-07-01",
                 "gyldigTom": "2024-12-31"
             }]
            ```
            1. ```StillingPeriode.GyldigFom = "2021-01-01", StillingPeriode.GyldigTom = "2024-01-31"```
            2. ```StillingPeriode.GyldigFom = "2021-01-01", StillingPeriode.GyldigTom = "2026-01-31"```
            3. ```StillingPeriode.GyldigFom = "2023-01-01", StillingPeriode.GyldigTom = "2026-01-31"```
        4. Perioden til enhetene spenner ikke over hele stillingsperioden - match mot eksisterede OT'er
        ```
         DvhRessursStilling.stillingOrgenhetNavn er satt                     "Nytt navn"
         DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn          "0110001"
         DvhRessursStilling.ressursAgressoId     er lik DvhRessursStilling.stillingAgressoId  
         DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
        ```
        ``` 
         nom_enehter[{
             "id": "a123bc",
             "agressoId": "0110001",
             "navn": "Gammelt navn",
             "orgNivå": "ORGENHET",
             "gyldigFom": "2022-01-01",
             "gyldigTom": "2022-12-31"
         }, {
             "id": "c321ba",
             "agressoId": "0110001",
             "navn": "Nytt navn",
             "orgNivå": "ORGENHET",
             "gyldigFom": "2022-07-01",
             "gyldigTom": "2024-12-31"
         }]
        ```
        ```
         StillingPeriode.GyldigFom = "2021-01-01"
         StillingPeriode.GyldigTom = "2024-01-31
        ```
        1. Eksisterende OT'er finnes som matcher hele StillingPerioden - benytt hele perioden til StillingPerioden
        ``` 
         orgtilknytninger[{
             orgEnhetId = "a123bc",
             GyldigFom = "2021-01-01",
             GyldigTom = "2022-06-30"
         }, {
             orgEnhetId = "c321ba",
             GyldigFom = "2022-07-01",
             GyldigTom = "2023-05-31"
         }]
        ```  
        2. Eksisterende OT'er finnes men matcher ikke hele StillingPerioden, men allikevel mer enn enhetens periode - benytt kombinasjon av enhetens periode og eksisterende OT's periode
            1. Kombinasjonen dekker hele StillingsPerioden
             ``` 
              orgtilknytninger[{
                  orgEnhetId = "a123bc",
                  GyldigFom = "2021-01-01",
                  GyldigTom = "2022-06-30"
              }, {
                  orgEnhetId = "c321ba",
                  GyldigFom = "2022-07-01",
                  GyldigTom = "2023-01-31"
              }]
            ```
           Nye StillingsPerioder:
            ```
             StillingPerioder[{
                 enhet = "a123bc",
                 GyldigFom = "2021-01-01",    <-- Fra OT
                 GyldigTom = "2022-06-30"     <-- Fra neste stillingsperiode
             }, {
                 enhet = "c321ba",
                 GyldigFom = "2022-07-01",    <-- Fra Enhet
                 GyldigTom = "2024-12-31"     <-- Fra Enhet
             }]
            ```   
            2. Kombinasjonen dekker ikke hele StillingsPerioden - Klipp StillingsPeriode
             ``` 
              orgtilknytninger[{
                  orgEnhetId = "a123bc",
                  GyldigFom = "2021-05-01",   
                  GyldigTom = "2022-06-30"
              }, {
                  orgEnhetId = "c321ba",
                  GyldigFom = "2022-07-01",
                  GyldigTom = "2024-12-31"
              }]
            ```
           Nye StillingsPerioder:
            ```
             StillingPerioder[{
                 enhet = "a123bc",
                 GyldigFom = "2021-05-01",    <-- Fra OT (Klippet Stillingsperiode)
                 GyldigTom = "2022-06-30"
             }, {
                 enhet = "c321ba",
                 GyldigFom = "2022-07-01",
                 GyldigTom = "2024-12-31"     
             }]
            ```  
            3. Kombinasjonen dekker ikke hele StillingsPerioden - Klipp StillingsPeriode
             ``` 
              orgtilknytninger[{
                  orgEnhetId = "a123bc",
                  GyldigFom = "2021-05-01",   
                  GyldigTom = "2022-06-30"
              }, {
                  orgEnhetId = "c321ba",
                  GyldigFom = "2022-07-01",
                  GyldigTom = "2023-12-31"
              }]
            ```
           Nye StillingsPerioder:
            ```
             StillingPerioder[{
                 enhet = "a123bc",
                 GyldigFom = "2021-05-01",    <-- Fra OT (Klippet Stillingsperiode)
                 GyldigTom = "2022-06-30"
             }, {
                 enhet = "c321ba",
                 GyldigFom = "2022-07-01",
                 GyldigTom = "2024-12-31"     
             }]
            ``` 
        3. Matcher ingen eksisterende OT - Klipp StillingsPeriode til enhet
           Ny StillingsPeriode:
            ```
             StillingPerioder[{
                 enhet = "a123bc",
                 GyldigFom = "2022-01-01",    
                 GyldigTom = "2022-06-30"
             }, {
                 enhet = "c321ba",
                 GyldigFom = "2022-07-01",
                 GyldigTom = "2024-12-31"     
             }]
            ``` 
    3. INGEN match - Presis enhet på stillingens agressoId matcher ingen enhet i NOM - dette må varsles som feil

2. Stillingens AgressoId er 'upresis' Det vil si færre enn 7 sifre - varsle som feil
    1. Hvis stillingens upresise AgressoId matcher presis ressursAgressoId (OrgenhetPaaRessurs), benytt denne ressursAgressoId (OrgenhetPaaRessurs) på akkurat samme måte som om Stillingens AgressoId hadde vært denne presise Id'en. (E.g. alle testene over gjentas)
    ```
     DvhRessursStilling.stillingOrgenhetNavn er satt                     "Overenhet navn"
     DvhRessursStilling.stillingAgressoId    er upresis men matcher      "01100"
     DvhRessursStilling.ressursAgressoId     er satt og 7+ tegn          "0110001"
     DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
    ```
    2. Hvis stillingens upresise AgressoId IKKE matcher evt presis ressursAgressoId (OrgenhetPaaRessurs) prøv å finne OT som matcher Stillingens upresise AgressoId. Bruk i så fall OT'ens periode og enhet
    ```
     DvhRessursStilling.stillingOrgenhetNavn er satt                     "Overenhet navn"
     DvhRessursStilling.stillingAgressoId    er upresis og matcher ikke  "02200"
     DvhRessursStilling.ressursAgressoId     er satt og 7+ tegn          "0110001"
     DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
    ```
    ```
     nom_enehter[{
         "id": "x123yz",
         "agressoId": "0220002",      <-- 
         "navn": "Annen enhet",
         "orgNivå": "ORGENHET",
         "gyldigFom": "2022-01-01"
     },{
         "id": "z321yx",
         "agressoId": "0110001",      <-- 
         "navn": "Orgenhet paa ressurs",
         "orgNivå": "ORGENHET",
         "gyldigFom": "2022-01-01"
     }]
    ```
    ```
     StillingPeriode.GyldigFom = "2022-01-01"
     StillingPeriode.GyldigTom = "2024-01-31"
    ```   
    1. Det finnes en OT som matcher Stillingens upresise AgressoId - benytt denne OT'en
       ```
        orgtilknytninger[{
            orgEnhetId = "x123yz",
            GyldigFom = "2022-01-01",
            GyldigTom = "2024-01-31"
         }]
       ```
    2. Det finnes en OT som matcher Stillingens upresise AgressoId men dekker bare deler av perioden - benytt denne OT'en og klipp StillingPerioden (Vil ivareta ant korrekt eksisterende OT)
       ```
        orgtilknytninger[{
            orgEnhetId = "x123yz",
            GyldigFom = "2023-01-01",
            GyldigTom = "2024-01-31"
         }]
       ```
       Ny StillingsPeriode:
       ```
        StillingPeriode.GyldigFom = "2023-01-01"
        StillingPeriode.GyldigTom = "2024-01-31
       ```
    3.  Hvis stillingens upresise AgressoId ikke matcher presis ressursAgressoId (OrgenhetPaaRessurs), og det ikke finnes OT som matcher til stillingens agressoId.
        1. Er StillingPerioden aktiv - forsøk å matche eksisterende OT til den presise ressursAgressoId (OrgenhetPaaRessurs), og bruk denne OT'en hvis match
          ```
           orgtilknytninger[{
               orgEnhetId = "z321yx",
               GyldigFom = "2022-01-01",
               GyldigTom = "2024-01-31"
            }]
          ```
        2. Hvis matchende OT ikke dekker hele StillingPerioden, klipp StillingPerioden til OT'en
         ```
           orgtilknytninger[{
               orgEnhetId = "z321yx",
               GyldigFom = "2023-01-01",
               GyldigTom = "2024-01-31"
            }]
         ```
        Ny StillingsPeriode:
         ```
          StillingPeriode.GyldigFom = "2023-01-01"
          StillingPeriode.GyldigTom = "2024-01-31
         ```
        3. Hvis StillingPerioden er historisk, mangler vi høyst sannsynlig historisk enhets data eller OT for å benytte denne. Dekker den historiske perioden til nyere tid enn 2023-06-01 varsle dette spesielt. - Ikke bruk StillingPerioden

        4. Hvis StillingPerioden er fremtidig, er det ant manglende informasjon i U4. Varsle dette spesielt, og forventer at dette korrigeres innen perioden blir aktiv. - Ikke bruk StillingPerioden
3. Hvis begge agressoId'ene er upresise
    1. forsøk å matche eksisterende OT'er mot først Stillingens upresise AgressoId og benytt evt matchende OT.
    2. Hvis ingen match der - så match mot ressursAgressoId (OrgenhetPaaRessurs) og bruk den OT'en hvis match. Dette kan kun gjøres på aktiv periode.
    3. Om ingen eksisterende OT matcher i det hele tatt, har vi ingen sjanse til å tolke DVH - og stillingPerioden vil ikke dekkes - må alarmeres.
4. Hvis det er 'Feilkode' eller manglende id på begge AgressoId'ene (både Stillingens og OrgenhetPaaRessurs) - varsle dette som feil 

