# Hvordan oppdatere orgtilknytninger for å speile de tolkede stillingsperiodene

Etter å ha behandlet DvhRessursStillinger har vi fått ett tolket sett med StillingPerioder som er grunnlaget for hvordan sluttresultatet av orgtilknytninger skal se ut. Her er et forsøk på å beskrive hvordan dette skal gjennomføres forr å få korrekt konsistens på orgtilknytningene.

## Knytte eksisterende orgtilknytning til stillingperiode
Vi benytter oss av orgenhet og startdato som nøkkel for å knytte en orgtilknytning til stillingsperiode, for sammenligning og evt endring/sletting.
Det vil si, alle orgtilknytninger vi ikke finner en tilsvarende stillingperiode for (basert på startdato og enhet) skal slettes.
Alle orgtilknytninger som matches, skal evt få endret DOL og/eller sluttdato.
Alle stillingsperioder som ikke har en tilsvarende orgtilknytning, skal behandles som en ny orgtilknytning.

## Konsekvens på match mot startdato
Dette regelverket forenkler i stor grad hvordan lage endrings meldinger på orgiltknytninger. Men samtidig vil ikke den ikke gjøre endringer på eksisterende orgtilknytningers startdato, siden da er startdato ulik, og vi matcher ikke eksisterende OT til Stillingspreiode som angir ny start dato.
Det vil i stedet ende opp med at det foretas en slett av eksisterende OT, og opprettelse av en ny.

## Bygg opp liste på endringsmeldinger per navident, før faktisk endring
I tolkningen over på å lage evtsrc meldinger på orgtilknytninger, så samle disse i 'StrukturerteStillingPerioderForNavident'
Da har vi all sammenstilt informasjon om DVH synkroniseringen per navident, for rapport, kontroll og feilsøking.

### kjør ut endringsmeldinger for navidenten, men ikke for de som er 'Mastret i NOM'
Når alle endringsmeldinger for navidenten er generert, så send disse ut.<br>
Ikke send ut noen meldinger hvis ressursen har status 'Mastret i NOM'. 
Dette bør sikre en 'clean' overgang på sektorbytte.

