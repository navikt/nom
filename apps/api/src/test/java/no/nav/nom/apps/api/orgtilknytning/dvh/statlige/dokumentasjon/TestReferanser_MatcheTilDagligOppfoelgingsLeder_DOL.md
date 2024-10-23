### TESTER: Matche til Daglig Oppfølgings Leder (DOL)
SP -> Stillingsperiode (strukturert fra DVH)
OT -> Eksisterende orgtilknytning
OPR -> Orgenhet På Ressurs

1. Historiske stillingperioder
   1. Match mot eksisterende OT'er og benytt DOl fra disse 
      1. En single stillingperiode vil alltid få DOL
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
      OT:
        Enhet A:      <-----------A----------->
        Enhet B:      <---------B-DOL--------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <--------A-DOL---------->
      ``` 
      2. Stillingperiode dekkes av en 'DOL OT' for enhet
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:      <---------A-DOL--------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
      ``` 
      3.  Stillingperiode dekkes av flere OT'er men kun en 'DOL OT'
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT: 
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
        Enhet C:      <-----------C----------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
      ```
      4.  Stillingperiode dekkes av flere OT'er men kun en 'DOL OT' - ulike overlapp
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:      <---------A-DOL--------->
        Enhet B:   <-----B------>
        Enhet C:           <----------C---------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
      ```
      5. Stillingperiode dekkes av en 'DOL OT' som har lengre periode
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:   <-----------A-DOL------------->
        Enhet B:      <-----------B----------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
      ``` 
      6.  Stillingperiode dekkes av flere OT'er med DOL
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:                  <-------A-DOL--------->
        Enhet A:     <---A-DOL--->
        Enhet B:  <------B----->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
      ``` 
      7. Stillingperiode dekkes av flere 'OT' i samme periode på samme enhet - varsle feil
      ```
      Dato            A                       B      DD
      OPR: 
        Enhet X
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:      <-----------A----------->
        Enhet A:      <-----------A----------->
        Enhet B:      <---------B-DOL--------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <---------B-DOL--------->
      Og error 
        DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_I_HISTORISK_PERIODE
      ``` 
      8. Stillingperiode dekkes av flere OT'er med DOL - faller tilbake på match mot Orgenhet På Ressurs (OPR)
      ```
      Dato            A                       B      DD
      OPR: 
        Enhet A
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT: 
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <---------B-DOL--------->
        Enhet C:      <-----------C----------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
      Og error
        DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_MED_DOL_I_HISTORISK_PERIODE
      ```   
   2. Klarte ikke sette DOL basert på eksisterende OT - match mot Orgenhet På Ressurs (OPR)  
      1. Det finnes ikke eksisterende OT - OPR matcher en SP
      ```
      Dato            A                       B      DD
      OPR: 
        Enhet A
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Ingen 
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
      ```   
      2. Det finnes ikke eksisterende OT med DOL - OPR matcher en SP
      ```
      Dato            A                       B      DD
      OPR: 
        Enhet B
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <---------B-DOL--------->
      ```   
      3. Det finnes ikke eksisterende OT med DOL - OPR delvis matcher en SP 
      ```
      Dato            A                       B      DD
      Enhet A - '0110021'
      Enhet B - '0110010'
      OPR: 
        '011001'
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <---------B-DOL--------->
      ```   
      4. Det finnes eksisterende OT med DOL som ikke matcher SP enhet - OPR matcher en SP
      ```
      Dato            A                       B      DD
      OPR: 
        Enhet A
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet B:      <-----------B----------->
        Enhet C:      <---------C-DOL--------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
      ```   
      5. Det finnes ikke eksisterende OT med DOL - OPR matcher ikke en SP
      ```
      Dato            A                       B      DD
      OPR: 
        Enhet C
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      Og error
        HISTORIC_PERIODE_KAN_IKKE_SETTE_DOL
      ``` 
      6. Det finnes flere eksisterende OT med DOL - Gir Error
      ```
      Dato            A                       B      DD
      OPR: 
        Enhet C
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:      <-----------A DOL------->
        Enhet B:      <-----------B BOL------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      Og error
        DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_MED_DOL_I_HISTORISK_PERIODE
      ```
      7. Det finnes flere eksisterende OT med DOL - Gir Error - matcher heller ikke på OPR - Gir Feil nr 2
      ```
      Dato            A                       B      DD
      OPR: 
        Enhet C
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        Enhet A:      <-----------A DOL------->
        Enhet B:      <-----------B BOL------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      Og error
        DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_MED_DOL_I_HISTORISK_PERIODE
        HISTORIC_PERIODE_KAN_IKKE_SETTE_DOL 
      ```      
      8. Det finnes ingen eksisterende OT - matcher heller ikke på OPR - Gir Feil
      ```
      Dato            A                       B      DD
      OPR: 
        Enhet C
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      OT:
        INGEN
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      Og error
        HISTORIC_PERIODE_KAN_IKKE_SETTE_DOL 
      ```
      9. Det er hull i historisk periode på stillingPerioder - gir error
      ```
      Dato            A                                  B      DD
      OPR: 
        Enhet C
      SP:
        Hull:                        <---->
        Enhet A:      <------A------>
        Enhet B:                           <------B------>
      OT:
        INGEN
      ```
      Resultat:
      ```
      Dato            A                                   B      DD
      SP:
        Enhet A:      <----A DOL---->
        Enhet B:                           <----B DOL---->
      Og error
        HULL_I_STILLINGSPERIODE 
      ```                     
      10. Det finnes ikke eksisterende OT med DOL - OPR delvis matcher flere SP
      ```
      Dato            A                       B      DD
      Enhet A - '0110021'
      Enhet B - '0110010'
      OPR: 
        '01100'
      SP:
        Enhet A:      <-----------A----------->
        Enhet B:      <-----------B----------->
      ```
      Resultat:
      ```
      Dato            A                       B      DD
      SP:
        Enhet A:      <---------A-DOL--------->
        Enhet B:      <-----------B----------->
      Og error
        MULTI_DOL_MATCH_MOT_OPR 
      ```         
2. Aktive/current StillingPerioder
   1. En single stillingperiode vil alltid få DOL
      ```
      Dato            A          DD             B
      OPR: 
        Enhet X
      SP:
        Enhet A:      <------------A------------>
      ```
      Resultat:
      ```
      Dato            A          DD             B
      SP:
        Enhet A:      <---------A-DOL----------->
      ```  
   2. Match mot OPR for å sette DOL
      ```
      Dato            A          DD             B
      OPR: 
        Enhet A
      SP:
        Enhet A:      <------------A------------>
        Enhet B:      <------------B------------>
      OT:
        Ingen 
      ```
      Resultat:
      ```
      Dato            A          DD             B
      SP:
        Enhet A:      <----------A-DOL---------->
        Enhet B:      <------------B------------>
      ```   
   3. Det finnes eksisterende OT med DOL - Skal uansett matche mot OPR
      ```
      Dato            A          DD             B
      OPR: 
        Enhet B
      SP:
        Enhet A:      <------------A------------>
        Enhet B:      <------------B------------>
      OT:
        Enhet A:      <----------A-DOL---------->
        Enhet B:      <------------B------------>
      ```
      Resultat:
      ```
      Dato            A          DD             B
      SP:
        Enhet A:      <------------A------------>
        Enhet B:      <----------B-DOL---------->
      ```   
   4. OPR delvis matcher en SP
      ```
      Dato            A          DD             B
      Enhet A - '0110021'
      Enhet B - '0110010'
      OPR: 
        '011001'
      SP:
        Enhet A:      <------------A------------>
        Enhet B:      <------------B------------>
      ```
      Resultat:
      ```
      Dato            A          DD             B
      SP:
        Enhet A:      <------------A------------>
        Enhet B:      <----------B-DOL---------->
      ```   
   5. OPR matcher ikke en SP
      ```
      Dato            A          DD             B
      OPR: 
        Enhet C
      SP:
        Enhet A:      <------------A------------>
        Enhet B:      <------------B------------>
      ```
      Resultat:
      ```
      Dato            A          DD             B
      SP:
        Enhet A:      <------------A------------>
        Enhet B:      <------------B------------>
      Og error
        CURRENT_PERIODE_KAN_IKKE_MATCHE_ORGENHET_PAA_RESSURS_FOR_DOL
      ```
   6. Det finnes ikke eksisterende OT med DOL - OPR delvis matcher flere SP
      ```
      Dato            A          DD             B
      Enhet A - '0110021'
      Enhet B - '0110010'
      OPR: 
        '01100'
      SP:
        Enhet A:      <------------A------------>
        Enhet B:      <------------B------------>
      ```
      Resultat:
      ```
      Dato            A          DD             B
      SP:
        Enhet A:      <----------A-DOL---------->
        Enhet B:      <------------B------------>
      Og error
        MULTI_DOL_MATCH_MOT_OPR 
      ```               
3. Fremtidige StillingPerioder
    1. En single stillingperiode vil alltid få DOL
       ```
       Dato            DD   A                       B
       OPR: 
         Enhet X
       SP:
         Enhet A:           <-----------A----------->
       ```
       Resultat:
       ```
       Dato            DD   A                       B
       SP:
         Enhet A:           <--------A-DOL---------->
       ```  
    2. Match mot OPR for å sette DOL
       ```
       Dato            DD   A                       B
       OPR: 
         Enhet A
       SP:
         Enhet A:           <-----------A----------->
         Enhet B:           <-----------B----------->
       OT:
         Ingen 
       ```
       Resultat:
       ```
       Dato            DD   A                       B
       SP:
         Enhet A:           <---------A-DOL--------->
         Enhet B:           <-----------B----------->
       ```   
    3. Det finnes eksisterende OT med DOL - Skal uansett matche mot OPR
       ```
       Dato            DD   A                       B
       OPR: 
         Enhet B
       SP:
         Enhet A:           <-----------A----------->
         Enhet B:           <-----------B----------->
       OT:
         Enhet A:           <---------A-DOL--------->
         Enhet B:           <-----------B----------->
       ```
       Resultat:
       ```
       Dato            DD   A                       B
       SP:
         Enhet A:           <-----------A----------->
         Enhet B:           <---------B-DOL--------->
       ```   
    4. OPR delvis matcher en SP
       ```
       Dato            DD   A                       B  
       Enhet A - '0110021'
       Enhet B - '0110010'
       OPR: 
         '011001'
       SP:
         Enhet A:           <-----------A----------->
         Enhet B:           <-----------B----------->
       ```
       Resultat:
       ```
       Dato            DD   A                       B 
       SP:
         Enhet A:           <-----------A----------->
         Enhet B:           <---------B-DOL--------->
       ```   
    5. OPR matcher ikke en SP
       ```
       Dato            DD   A                       B 
       OPR: 
         Enhet C
       SP:
         Enhet A:           <-----------A----------->
         Enhet B:           <-----------B----------->
       ```
       Resultat:
       ```
       Dato            DD   A                       B
       SP:
         Enhet A:           <-----------A----------->
         Enhet B:           <-----------B----------->
       Og error
         FREMTIDIG_PERIODE_KAN_IKKE_MATCHE_ORGENHET_PAA_RESSURS_FOR_DOL
       ```
    6. Det er hull i fremtidig periode på stillingPerioder - gir error
       ```
       Dato            DD   A                                  B 
       OPR: 
         Enhet C
       SP:
         Hull:                             <---->
         Enhet A:           <------A------>
         Enhet B:                                <------B------>
       OT:
         INGEN
       ```
       Resultat:
       ```
       Dato            DD   A                                   B
       SP:
         Enhet A:           <----A DOL---->
         Enhet B:                                <----B DOL---->
       Og error
         HULL_I_STILLINGSPERIODE 
       ```                
   7. Det finnes ikke eksisterende OT med DOL - OPR delvis matcher flere SP
      ```
      Dato            DD   A                       B
      Enhet A - '0110021'
      Enhet B - '0110010'
      OPR: 
        '01100'
       SP:
         Enhet A:           <-----------A----------->
         Enhet B:           <-----------B----------->
      ```
      Resultat:
      ```
      Dato            DD   A                       B
      SP:
        Enhet A:           <---------A-DOL--------->
        Enhet B:           <-----------B----------->
      Og error
        MULTI_DOL_MATCH_MOT_OPR 
      ```                  