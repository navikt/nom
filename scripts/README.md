# Script for ekstra DEV-miljø

> [!WARNING]
> Dette er et script som i hovedsak er laget utelukkende av AI og det er selve resultatet som er gjennomgått, ikke scriptkoden i seg selv. 
> <p>Alle genererte filer _må_ gjennomgåes manuelt før det pushes til repo!!
> <p>Det bør gjøres sammenligning på applikasjons konfig filer mellom variant konfig, hoved konfig og dev konfig. Appene får spring profile DEV og &ltsuffix&gt, og vil derfor lese 'application.yaml' som ioverstyres av verdier i 'application-dev.yaml' og til slutt overstyres av verdier i 'application-&ltsuffix&gt.yaml' konfig filen 

`create_additional_dev_deployment.py` lager lokale forslag til et nytt NOM DEV-miljø med eget suffix. Det oppretter variantkonfigurasjon og Nais-manifester for appene og stream-appene, én fil med 16 Kafka-topics og en egen topic-workflow for DEV. Scriptet legger også **aktive** variantreferanser i eksisterende deploy-workflows. Det committer, pusher eller deployer ikke.

Kjør fra prosjektroten med et nytt suffix, for eksempel `q9`:

```bash
python3 -B scripts/create_additional_dev_deployment.py q9
```

Suffix må bestå av små bokstaver, sifre og bindestreker, uten bindestrek først eller sist. Scriptet nekter å overskrive genererte variantfiler. Det endrer eksisterende deploy-workflows når det kjøres. Testene bruker en midlertidig kopi, ikke prosjektfilene:

Manifestfilene får suffix før miljøleddet, for eksempel `api-q9-dev-gcp.yaml`. Øverst i genererte manifester og profilfiler står kildefilene og commit-SHA for siste endring av hver fil. Scriptet avviser kilder som avviker fra `HEAD`, slik at SHA-en beskriver innholdet som ble kopiert. `application-q9.yaml` overstyrer DEV-konfigurasjon når manifestet aktiverer `dev,q9`. Gjennomgå alle markerte verdier fra `application-dev.yaml` før bruk.

Scriptet legger til `nom-ui2-<suffix>` inbound i det delte DEV-manifestet for `nom-azure`, og `nom-api-<suffix>` i DEV-manifestet for `nom-proxy`. Det beholder tidligere variantregler, `org-token-tool` og `org-ekstern-proxy` der de finnes, samt eksisterende regler for `logging`, `norg2` og `orgenhet-kostnadssted-mapping-api`. PROD-manifestene endres ikke. Kontroller også disse to endringene før push.

## Før commit og push av generert variant-testmiljø

!! KONTROLLER ALLE GENERERTE FILER OG MANIFESTER !!

Genererte YAML-filer har `@` først på linjer som krever avgjørelse. Linjene beholder den opprinnelige innrykkingen og verdien, og gjør YAML ugyldig med vilje. Kontroller dem og fjern `@` først når du har valgt riktig verdi og validert filene. Gå også gjennom:

- Kafka-topicnavn, ACL-er og Streams application ID-er: Ingen vanlig DEV-app, andre varianter eller eksterne konsumenter skal ha tilgang.
- SQL, Redis, buckets, interne URL-er og scopes: Ressurser og interne kall skal peke på riktig suffix.
- Azure-klienter, `azp`, ingress, secrets og tillatelser til delte tjenester som PDL, `nom-proxy` og `nom-azure`.
- Endringene i deploy-workflowene og topic-workflowen. Referansene er aktive og kan starte deploy ved push til en branch som matcher workflowens triggere.

Kjør testene og gjennomgå `git diff` før du committer eller pusher. Scriptet er et utgangspunkt for manuell kontroll, ikke en godkjenning av tilgangene.

### Følgende applikasjoner må få satt opp inbound acl for angitte variant apper

Generatoren legger inn disse reglene i eksisterende DEV-manifester og beholder regler for tidligere varianter. Kontroller at de finnes før push:

- `nom-azure` (`.nais/apps/nom-azure/nom-azure-dev-gcp.yaml`) må tillate `nom-ui2-<suffix>`.
- `nom-proxy` (`.nais/apps/proxy/proxy-dev-fss.yaml`) må tillate `nom-api-<suffix>`.

Tilgang hos disse tjenestene må avklares med ansvarlig team. Generatoren setter ikke opp inbound-regler eller bestiller tilgang hos dem:

- PDL: Bestill tilgang for `nom-api-<suffix>` og `nav-ident-<suffix>`.
- Kostnadsstedmapping: Bestill tilgang for `nom-api-<suffix>`.
- Norg2: Sørg for at `nom-api-<suffix>` får tilgang.
- TKat (`team-catalog-backend`): Sørg for at `nom-ui2-<suffix>` får tilgang.

Foreslåtte oppføringer under `accessPolicy.inbound.rules`. De to første legges inn av generatoren. De øvrige er eksempler til ansvarlig team; avklar også eventuell token-autorisasjon hos mottakeren:

```yaml
# nom-azure
- application: nom-ui2-<suffix>
  namespace: org
  cluster: dev-gcp

# nom-proxy
- application: nom-api-<suffix>
  namespace: nom
  cluster: dev-gcp

# PDL
- application: nom-api-<suffix>
  namespace: nom
  cluster: dev-gcp
- application: nav-ident-<suffix>
  namespace: org
  cluster: dev-gcp

# kostnadsstedmapping
- application: nom-api-<suffix>
  namespace: nom
  cluster: dev-gcp

# norg2
- application: nom-api-<suffix>
  namespace: nom
  cluster: dev-gcp

# TKat (team-catalog-backend)
- application: nom-ui2-<suffix>
  namespace: org
  cluster: dev-gcp
```

## Nav-ident SEQUENCE må settes

I variant miljøe må sekvens for nav-ident telleren settes. 
Denne må settes til å være høyere enn siste benyttede navident nr som er lastet inn som data før noen starter å opprette nye navidenter. Dette gjøres ved å kjøre følgende SQL mot nav-ident databasen:

```sql
SELECT setval('navident_seq', <ny neste verdi>, false)
```

## Følgende applikasjoner får egen deploy

- `nom-api`
- `nom-ui2`
- `nav-ident` med egen `nav-ident-redis`
- `nom-admin`
- `nom-admin-web`
- `nom-rdbms-data`
- `nom-paarorende`
- `nom-sf-integrasjon`

Disse stream-appene får også egen deploy:

- `nom-intern-organisering-state`
- `nom-intern-orgenhet-leder-state-v2`
- `nom-intern-orgenhet-state`
- `nom-intern-orgtilknytning-state`
- `nom-intern-ressurs-state`
- `nom-kafka-api-orgenhet-state`
- `nom-kafka-api-ressurs-state-v4`

`nom-azure` og `nom-proxy` er delte tjenester og får ikke egen variant-deploy.

## Følgende topics blir opprettet

Generatoren lager én topic-fil per miljø: `.nais/topics/topics-additional-dev-<suffix>.yaml`. Disse 16 topicene får alle `-<suffix>` i navnet:

- `nom.ressurs-eventsource-<suffix>`
- `nom.intern-ressurs-state-<suffix>`
- `nom.orgenhet-eventsource-<suffix>`
- `nom.intern-orgenhet-state-<suffix>`
- `nom.orgtilknytning-eventsource-<suffix>`
- `nom.intern-orgtilknytning-state-<suffix>`
- `nom.organisering-eventsource-<suffix>`
- `nom.intern-organisering-state-<suffix>`
- `nom.nom-authorize-eventsource-<suffix>`
- `nom.orgenhet-leder-eventsource-<suffix>`
- `nom.intern-orgenhet-leder-state-<suffix>`
- `nom.nom-mastered-ressurs-<suffix>`
- `nav-ident-<suffix>`
- `nav-ident-rnr-<suffix>`
- `nom.api-ressurs-state-v4-<suffix>`
- `nom.api-orgenhet-state-v3-<suffix>`

Topic-filen har en egen DEV-workflow. Den oppretter ikke topics før workflowen kjøres etter push eller startes manuelt.

## Unit test kjøring av scriptet

```bash
python3 -B -m unittest discover -s scripts/test -p 'test_*.py'
```
