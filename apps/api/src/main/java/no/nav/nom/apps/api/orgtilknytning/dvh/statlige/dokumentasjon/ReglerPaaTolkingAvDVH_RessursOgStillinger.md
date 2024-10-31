# Forsøk på samle beskrivelse av regler og avarter for tolking av DVH ressurser og stillinger (fra U4 og remedy)
Samt sammenstille referanse til tester som sjekker overholdelse av reglene

Dette er kun for behandling av statlige - kommunale og eksterne skal nå mastres i NOM. 
Gjør ingen faktisk OT oppdatering på Ressurser som er markert som Mastret i NOM. (Men strukturering og prosessering/tolkning gjøres)

## Grunnlag og overordnet tolkning

### DVH endepunkt og SQL 

`/proxy/dvh/json/Hr/Nom/NOM_RESSURS_STILLINGER` 

Henter alle stillinger med 3 års historikk og tilhørende dvh ressurs info. Konverteres til `DvhRessursStilling` objekt

    CREATE EDITIONABLE VIEW DT_HR.NOM_RESSURS_STILLINGER AS
    select distinct
        ressurs.NAV_ID,
        ressurs.RESOURCE_ID,
        ressurs.FIRST_NAME,
        ressurs.SURNAME,
        stilling.FRA_DATO,
        stilling.TIL_DATO,
        stilling.POST_DATE_TO,
        stilling.ANSETTELSESTYPE_BESK,
        stilling.PARTTIME_PCT,
        stilling.ORGENHET,
        stilling.ORGENHET_BESK,
        stilling.LEDER_PAA_STILLING,
        stilling.PERM_KODE,
        stilling.LAST_UPDATE,
        ressurs.ORGENHET_PAA_RESSURS,
        ressurs.ORGENHET_PAA_RESSURS_NIVA,
        ressurs.REMEDY_ENHET_ID,
        stilling.ORGENHET_PAA_RESSURS as "stilling.ORGENHET_PAA_RESSURS",
        ressurs.AGRESSO_DATE_FROM,
        ressurs.AGRESSO_DATE_TO,
        ressurs.DATE_FROM,
        ressurs.DATE_TO,
        ressurs.LEDER_RESOURCE_ID,
        ressurs.SYKEMLEDER_RESOURCE_ID
    from DT_HR.HRRES_STILLINGER stilling
    left join DT_HR.HRRES_RESSURS ressurs
        on ressurs.RESOURCE_ID = stilling.RESOURCE_ID
    where add_months(trunc(sysdate), -36) <= til_dato
    order by NAV_ID, FRA_DATO;

### Stillinger, Orgtilknytninger og Enheter i NOM vs enheter i Unit4 

Kort forklaring - en enhet i NOM er unik på kombinasjonen av U4 feltene `AgressoId`, `OrgNivå` og `Navn`.
1. Hvis en enhet skifter navn i U4, så vil vi opprette en ny enhet i NOM. 
2. Det opprettes også av og til ny enhet i U4 med annet navn, men samme nivå og id - det vil i stor grad se likt ut som tilfellet over for vår del.

En Stilling i U4 skal være tilknyttet en AgressoId.
Denne stillingen kan da i prinsippet i perioden overlappe to eller flere enheter (også i U4 - ref pkt 2. over)
Vi må da matche oppdelte stillingsperioder i NOM til å henge på enheter i NOM som ikke går utover enhetenes perioder. 

En AgressoId i U4 på løvnode/`ORGENHET` har stort sett 7 tegn, men kan være 8 eller 9.

En Stilling kan i U4 i enkelte tilfeller henge på en enhet som ikke er løvnode - `OrgNivå == 'ORGENHET'`
I NOM skal den alltid tilknyttes en 'ORGENHET'

I Unit4/DVH kan det være flere grunner til at vi får mange rader for samme ressurs på det vi ønsker å tolke som en sammenhengende Orgtilknytning (OT). Det kan f.eks. være at ressursen har ulike stillingsrader på prosent, lønn etc.
Vi ønsker derfor å slå sammen disse radene til en 'felles' OT i NOM.

DVH har også tolket `OrgenhetPaaRessursNivaa` for å representere OrgNivå for den tolkede `OrgenhetPaaRessurs`.
Dette vil i de fleste tilfeller være U4 sin 'stiling i bunn' sin OrgNivå 

DVH kan i tilfeller levere tomt på `OrgenhetPaaRessurs` og `OrgenhetPaaRessursNivaa` - dette er en feil i DVH og skal tolkes som 'ingen tilknytning'

### Daglig oppfølging 
Slik jeg har forstått det er DVH sin OrgenhetPaaRessurs det som representerer det de kaller 'stilling/enhet/leder i bunn', og er det vi ønsker å representere som den aktive `Daglig Oppfølgings Leder`-tilknytningen' (DOL) 
Så vi prøver å finne OT med agressoId som matcher OrgenhetPaaRessurs, hvis det er flere orgtilknytninger på samme periode. 

1. I NOM skal vi alltid kun ha en aktiv DOL periode om gangen
2. Og DOL perioder kan ikke overlappe

## Prosessering og regler

Vi gjør først en prosessering av alle `DvhRessursStilling` objektene fra DVh lasten (`DT_HR.NOM_RESSURS_STILLINGER`) for å strukturere alle stillinger på Ressursen til å matche slik vi ønsker å se de i NOM brutt opp i perioder på enhet og DOL. Dette lager da ett sett med `StillingPeriode` objekter for NavIdenten.

StillingPeriode:
```
StillingAgressoOrgIdOgNavn stilling, // AgressoId (stilling), ressursAgressoId (OrgenhetPaaRessurs), ressursAgressoOrgNiv (OrgenhetPaaRessursNivaa) og EnhetsNavn (stillingOrgenhetsNavn/stilling.ORGENHET_BESK) 
LocalDate gyldigFom,
LocalDate gyldigTom,  // Aldri null. Open ended er 2099-12-31
IOrgenhet orgenhet,
DagligOppfolging dagligOppfolging // JA, NEI, UKJENT
```

### Gruppere og slå sammen perioder

Vi grupperer deretter alle DvhRessursStillinger på unike StillingPeriode.StillingAgressoOrgIdOgNavn, og slår sammen sammenhengende perioder til en periode 
Vi dropper også evt stillinger fra U4 som starter på eller etter det som er registrert som sluttdato på ressursen i NOM allerede. Det kan være f.eks. en ikke avsluttet stilling i U4 på en kommunal ressurs, men som har fått en sluttdato i Remedy. Tilsvarende avslutter vi siste StillingPeriode på Ressursens sluttdato.  

Hvis vi skulle få DvhRessursStillinger som for samme StillingAgressoOrgIdOgNavn som har overlappende perioder, så vil det generere en feilmelding i logg.

### Matche til enheter i NOM

Deretter skal vi matche disse DVH periodene til Enheter i NOM. Om DVH perioden spenner over flere enheters gyldighetsperiode, så skal vi splitte perioden i flere perioder.

1. Er agressoId (Stilling/U4) 'presis' - det vil si 7 eller flere sifre, så representerer den en løvnode/ORGENHET.
    Dette vil være tilfellet med korrekt stilling i U4 hvor stillingen henger på en AgressoId for enhet av nivå ORGENHET. 
    Da skal vi finne den enhet i NOM som har samme (exact match) AgressoId og er nivå ORGENHET. 
    Det kan være: 

   1. Single match:  
   Hvis denne enhetens periode ikke spenner over hele StillingPerioden.
   Og dette vil kunne gi en logisk feil i tolkning av dataene fra DVH. E.g. vi har en stillingsperiode vi ikke fullt ut klarer å representere i NOM, eller vi har en OT som går utover enhetens periode
   Dette må logges/advares - og noe må korrigeres ett sted
      1. Hvis vi har eksisterende OT på enheten for identen og om den dekker stillingsperioden (det vil si, enhetens periode er endret i mellomtiden?), så lag StillingPeriode med OT'ens periode, selv om dette går utover enhetens periode
      Dette er for å forhindre uønsket fjerning/endring av evt OT
      2. Hvis ikke klipp StillingPerioden til enhetens periode 

   2. Multi match:
   Vi kan få treff på flere enheter i NOM. Som tilsammen bør dekke hele StillingPerioden. 
   Vi må splitte denne stillingperioden i flere perioder for å matche perioden til disse enhetene. På samme måte som ved singel match må vi advare om vi ikke klarer å representere hele stillingsperioden i NOM, og evt benytte eksisterende OT.
   Dette må logges/advares - og noe må korrigeres ett sted
   Hvis noen av disse enhetene er overlappende i perioden, så må vi også advare om dette.
   Splitten av StillingsPeriode gjøres på gyldigFOM datoen til den nyeste enheten (seneste gyldigFom)
   
   3. Ingen match: 
   Hvis vi av en eller annen grunn ikke klarer å matche presis AgressoId til en enhet i NOM så må dette logges/advares - og noe må korrigeres ett sted
      1. Hvis det eksisterer en OT på ressursen for eksakt samme periode (valget 'eksakt samme periode' er subjektivt) og det er ikke flere andre StillingPerioder på samme periode, så forvent at den eksisterende er/var korrekt, og benytt den som enhet i StillingPerioden for å forhindre uønsket fjerning/endring av OT som antagelig er korrekt    

2. Er stillingens agressoId < 7 tegn henger ikke stillingen i U4 ikke på en løvnode og/eller hvor denne har feilkode i DVH stedet (felte har negativ tallkode - se `DvhRessursStilling`), men det finnes presis ressursAgressoId (OrgenhetPaaRessurs) 
   1. Hvis det ikke er feilkode og denne matcher en presis ressursAgressoId (OrgenhetPaaRessurs), så kan vi med stor sikkerhet anta at DVH tolkningen er korrekt, og vi kan plassere StillingPerioden på denne enheten som ressursAgressoId (OrgenhetPaaRessurs) peker på med samme regler som over [1. Presis enhet på stilling]. 
   2. Hvis det derimot ikke er match mellom stillingens agressoId og ressursAgressoId (OrgenhetPaaRessurs) så forsøk å finne eksisterende OT matcher mot stillingens agressoId - Er det match, benytt den OT'ens periode og enhet. 
    Varsle dette, og evt. om OT'ens periode avviker fra StillingPerioden.
   3. Og om det her ikke er noen match mellom stillingens agressoId og ressursAgressoId (OrgenhetPaaRessurs), men ressursAgressoId (OrgenhetPaaRessurs) er presis så behandle denne på samme måte som for presis agressoId på Stilling [1. Presis enhet på stilling]. Merk at dette bør kun gjøres på aktiv periode
   
3. Hvis stillingens AgressoId ikke er presis eller er feilkode eller null, og ressursAgressoId (OrgenhetPaaRessurs) heller ikke er presis - dvs 7 eller flere sifre 
   1. forsøk å matche eksisterende OT'er mot først Stillingens upresise AgressoId og benytt evt matchende OT. (samme som 2.ii over?)
   2. Hvis ingen match der - så match mot ressursAgressoId (OrgenhetPaaRessurs) og bruk den OT'en hvis match. Dette kan kun gjøres på aktiv periode.
   3. Om ingen eksisterende OT matcher i det hele tatt, har vi ingen sjanse til å tolke DVH - og stillingPerioden vil ikke dekkes - må alarmeres.

### Matche til Daglig Oppfølgings Leder (DOL)

Hvis ulike StillingPerioder overlapper hverandre uten å dekke hverandre helt, så splitt periodene slik at periodene dekker hverandre helt eller ikke overlapper mer. Brekk også opp ift eksisterende OT'er på ressursen.
E.g splitt følgende to:
```
A:     <----------------------->
B:             <----------------------->
OT_A:  <----DOL------>
OT_B:                 <----DOL--------->
```
Til:
```
A:     <------><-----><-------->
B:             <-----><--------><------>
```
Det gjør det lettere å finne DOL perioder.
Perioder som da kun dekkes av en StillingPeriode vil da være DOL     

Perioder som dekker hverandre må vi finne DOL ved å:
1. Hvis historisk periode
   Slå opp eksisterende OT på ressursen for samme periode og enheter. Finnes historikk med DOL som matcher, bruk historikkens DOL.<br> 
   Hvis ingen historikk matcher, sjekk om StillingPeriodens AgressoId matcher `ORGENHET_PAA_RESSURS` - bruk da dette som DOL.<br>
   Hvis ingen treff over, så logg og advar 
2. Hvis aktiv periode
   Match med `ORGENHET_PAA_RESSURS` og bruk matchende StillingPeriode som DOL.<br>
   Finnes ingen så logg og advar om dette - vi vil da ende opp uten aktiv DOL periode
3. Hvis fremtidig periode
   Sjekk om StillingPeriodens AgressoId matcher `ORGENHET_PAA_RESSURS` - bruk da denne som DOL. <br>
   Hvis ingen treff over, så logg - men vi forventer at dette korrigerer seg før/når perioden blir aktiv

Avslutt med å gjøre kontroll på at det ikke finnes overlappende DOL perioder og at det må finnes en aktiv periode med DOL.  

Slå (eventuelt) sammen igjen evt StillingPerioder som er sammenhengende på enhet og DOL.
E.g. (* markerer DOL)
```
A: <---*---><----------*----->
B:          <----------------><---*--->
```
Kan slås sammen til:
```
A: <----------*-------------->
B:          <----------------><------>
```
Og
```
A: <---*---><---------------->
B:          <---------*------><---*--->
```
Kan slås sammen til:
```
A: <---*---><---------------->
B:          <-------------*----------->
```
Men
```
A: <---*---><----------------><---*--->
B:          <---------*------>
```
Der kan ikke A slås sammen siden det er reelt brudd på DOL




