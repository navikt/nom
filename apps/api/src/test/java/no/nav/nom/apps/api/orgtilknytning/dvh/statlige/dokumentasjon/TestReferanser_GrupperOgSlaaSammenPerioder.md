# TESTER: Gruppere og slå sammen perioder

1. Slår sammen like perioder fra DVH.
    1. Hvor perioder henger sammen, og (presis enhet på stilling, og på orgenhetPaaRessurs)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt
    DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn
    DvhRessursStilling.ressursAgressoId     er lik stillingAgressoId
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ``` 
    2. Hvor to stillinger med perioder som henger sammen, og
       (presis enhet på stilling, den ene med lik orgenhetPaaRessurs, den andre ulik på orgenhetPaaRessurs)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt 
    DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn  
    DvhRessursStilling.ressursAgressoId     er lik DvhRessursStilling.stillingAgressoId
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ``` 
   ``` 
    DvhRessursStilling.stillingOrgenhetNavn er satt (ulik den over)
    DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn (ulik den over)
    DvhRessursStilling.ressursAgressoId     er ulik DvhRessursStilling.stillingAgressoId
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ```
   Forvent 2 StillingPerioder - for hver av de stillingAgressoId over

    3. Hvor perioder henger sammen, og (presis enhet på stilling, og på orgenhetPaaRessurs men ulik disse imellom)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt
    DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn  
    DvhRessursStilling.ressursAgressoId     er lik stillingAgressoId
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ```
   ``` 
    DvhRessursStilling.stillingOrgenhetNavn er satt (lik den over)
    DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn (lik den over)
    DvhRessursStilling.ressursAgressoId     er ulik DvhRessursStilling.stillingAgressoId
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ```
   Forvent 2 StillingPerioder - en for hver av de ressursAgressoId over

    4. Hvor perioder henger sammen, og (presis enhet på stilling, ikke presis på orgenhetPaaRessurs - matcher)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt
    DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn  
    DvhRessursStilling.ressursAgressoId     er lik stillingAgressoId men på mindre enn 7 tegn
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGNIV3'
   ```
    5. Hvor perioder henger sammen, og (ikke presis enhet på stilling, presis på orgenhetPaaRessurs, matcher)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt
    DvhRessursStilling.stillingAgressoId    er lik ressursAgressoId men på mindre enn 7 tegn
    DvhRessursStilling.ressursAgressoId     er satt og 7+ tegn
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ```
    6. Hvor perioder henger sammen, og (ikke presis enhet på stilling, presis på orgenhetPaaRessurs, matcher ikke)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt
    DvhRessursStilling.stillingAgressoId    er mindre enn 7 tegn
    DvhRessursStilling.ressursAgressoId     er satt og 7+ tegn - men ulik stillingAgressoId
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ```
    7. Hvor perioder henger sammen, og (ikke presis enhet på stilling, ikke presis på orgenhetPaaRessurs, matcher)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt
    DvhRessursStilling.stillingAgressoId    er mindre enn 7 tegn
    DvhRessursStilling.ressursAgressoId     er lik stillingAgressoId men på mindre enn 7 tegn
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGNIV3'
   ```
   8. Hvor perioder henger sammen, og (ikke presis enhet på stilling, ikke presis på orgenhetPaaRessurs, matcher ikke)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt
    DvhRessursStilling.stillingAgressoId    er mindre enn 7 tegn
    DvhRessursStilling.ressursAgressoId     er lik stillingAgressoId men på mindre enn 7 tegn
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGNIV3'
   ```   
   9. Hvor perioder henger sammen, og  (mangler enhet på stilling, presis på orgenhetPaaRessurs)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er 'Mangler'
    DvhRessursStilling.stillingAgressoId    er '-4'  
    DvhRessursStilling.ressursAgressoId     er 7+ tegn
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ```
   10. Hvor perioder henger sammen, og  (mangler enhet på stilling, upresis på orgenhetPaaRessurs)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er 'Mangler'
    DvhRessursStilling.stillingAgressoId    er '-4'  
    DvhRessursStilling.ressursAgressoId     er mindre enn 7 tegn
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGNIV3'
   ```
    11. Hvor perioder henger sammen, og  (mangler enhet på stilling, mangler på orgenhetPaaRessurs - tror ikke dette finnes i reelt)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er 'Mangler'
    DvhRessursStilling.stillingAgressoId    er '-4'  
    DvhRessursStilling.ressursAgressoId     er '-4'
    DvhRessursStilling.ressursOrgenhetNiv   er null
   ```

2. Slå sammen perioder som går utover ressursens startdato
    1. Hvor perioder henger sammen, og første periode slutter før ressursen startdato
    2. Hvor perioder henger sammen, og første periode starter før ressursen startdato, men slutter etter
    3. Hvor ulike perioder henger sammen, og den ene med periode før ressursen startdato og en periode som starter før ressursens startdato
   
3. Slå sammen perioder som går utover ressursens sluttdato
    1. Hvor perioder henger sammen, og siste periode er slutter etter ressursen sluttdato
    2. Hvor perioder henger sammen, og siste periode starter etter ressursen sluttdato
    3. Hvor ulike perioder henger sammen, og den ene med periode etter ressursen sluttdato

4. Slå sammen perioder med overlapp
    1. Hvor perioder henger sammen men det er overlapp - sjekk varsling
    2. Hvor perioder henger sammen men det er overlapp på flere - sjekk varsling

5. Splitt på ulike sektor og tilknytninger, men sammenhengende datoer
    1. Hvor første to stillinger har samme ansattTypeBesk
    2. Hvor siste har annen ansattTypeBesk
    3. Hvor perioder henger sammen, men har ulike ansattTypeBesk

6. Slå sammen sammenhengende perioder med like sektor og tilknytninger
    1. Hvor perioder henger sammen, og har samme ansattTypeBesk

7. Ikke slå sammen sammenhegende perioder med ulike sektor og tilknytninger alternativ
    1. Hvor perioder henger sammen, men har ulike ansattTypeBesk som ikke skal slås sammen
