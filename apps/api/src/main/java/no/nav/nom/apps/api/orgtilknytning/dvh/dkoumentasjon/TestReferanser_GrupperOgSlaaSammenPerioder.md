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
    3. Hvor perioder henger sammen, og (presis enhet på stilling, og på orgenhetPaaRessurs men ulik disse imellom)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt
    DvhRessursStilling.stillingAgressoId    er satt og 7+ tegn  
    DvhRessursStilling.ressursAgressoId     er ulik stillingAgressoId
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ```
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
    7. Hvor perioder henger sammen, og (ikke presis enhet på stilling, ikke presis på orgenhetPaaRessurs, matcher ikke)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er satt
    DvhRessursStilling.stillingAgressoId    er mindre enn 7 tegn
    DvhRessursStilling.ressursAgressoId     er lik stillingAgressoId men på mindre enn 7 tegn
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGNIV3'
   ```
    8. Hvor perioder henger sammen, og  (mangler enhet på stilling, presis på orgenhetPaaRessurs)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er 'Mangler'
    DvhRessursStilling.stillingAgressoId    er '-4'  
    DvhRessursStilling.ressursAgressoId     er 7+ tegn
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGENHET'
   ```
    9. Hvor perioder henger sammen, og  (mangler enhet på stilling, upresis på orgenhetPaaRessurs)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er 'Mangler'
    DvhRessursStilling.stillingAgressoId    er '-4'  
    DvhRessursStilling.ressursAgressoId     er mindre enn 7 tegn
    DvhRessursStilling.ressursOrgenhetNiv   er 'ORGNIV3'
   ```
    10. Hvor perioder henger sammen, og  (mangler enhet på stilling, mangler på orgenhetPaaRessurs - tror ikke dette finnes i reelt)
   ```
    DvhRessursStilling.stillingOrgenhetNavn er 'Mangler'
    DvhRessursStilling.stillingAgressoId    er '-4'  
    DvhRessursStilling.ressursAgressoId     er '-4'
    DvhRessursStilling.ressursOrgenhetNiv   er null
   ```

2. Slå sammen perioder som går utover ressursens sluttdato
    1. Hvor perioder henger sammen, og siste periode er slutter etter ressursen sluttdato
    2. Hvor perioder henger sammen, og siste periode starter etter ressursen sluttdato
    3. Hvor ulike perioder henger sammen, og den ene med periode etter ressursen sluttdato

3. Slå sammen perioder med overlapp
    1. Hvor perioder henger sammen men det er overlapp - sjekk varsling
    2. Hvor perioder henger sammen men det er overlapp på flere - sjekk varsling
