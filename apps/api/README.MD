# Lokal kjøring

[Hent aad-properties fra dev-gcp](../../README.md#hente-aad-properties)


## Api

Kopier `application-local.yaml.template` til `application-local.yaml` og bruke denne profilen ved oppstart.

## Kafka

Apiet bruker kafka. Oppsett av Kafka håndteres via docker-compose.
Før du gjør noe annet så kjør `docker-compose up -d` fra rot. Husk at Docker Desktop må kjøre på maskinen din.

For at all funksjonalitet i api skal fungere må du også kjøre _streams_-appene. Under `apps/streams` starter du samtlige av state appene:
* orgenhet-state
* orgtilknytning-state
* ressurs-state
* api-orgenhet-state
* api-ressurs-state

Alle _må_ ikke nødvendigvis kjøre. Men er greit å ha et så likt kjøremiljø som mulig. Husk at du må kopiere `application-local.yaml.template` til `application-local.yaml` og bruke denne profilen ved oppstart.

NB: dersom topic for en app ikke eksisterer enda vil de feile under oppstart. Når api produserer meldinger vil topic lages automatisk. I såfall er det bare å restarte state-appen om den først feilet pga manglende topic


## Datavarehus (DVH)

Ved oppstart vil appen laste inn data om ressurser og orgenheter fra datavarehus. Dette skjer via nom-proxy. 
Du kan kjøre fra dev uten å måtte foreta deg noe mer, men datakvaliteten i prod er bedre, derfor er det anbefalt.

For å få data fra prod må du kjøre mot nom-proxy i prod:

1. Ha naisdevice som kjører grønt. Huk av `onprem-k8s-prod`. Du blir tatt til en naisdevice side hvor du blir spurt om hvorfor du trenger tilgang. Skriv kort at du trenger tilgang fordi du er utvikler for NOM og trenger laste data fra Unit4/Agresso.
2. Kjør scriptet (i project root) for å sette opp secrets med flag for å få prod credentials for api-appen. `python setup_local_azure_secrets.py --apps=nom-api --env=prod`
3. I `application-local` oppdaterer du:
```yaml
NOM_PROXY_BASEURL: https://nom-proxy.intern.nav.no
NOM_PROXY_API_SCOPE: api://prod-fss.nom.nom-proxy/.default 
```

## Publisere last fra datavarehus til event-source

Api vil som standard kun publisere meldinger fra datavarehus last som en kronjobb som kjører 06:00 hver dag. Ved lokal kjøring ønsker du å trigge denne manuelt.
Det kan du gjøre via swagger config:

1. Gå til [swagger](http://localhost:8081/swagger-ui/index.html?urls.primaryName=External%20API%20v1) og klikk `Authorize`.
2. [Skaff token herfra](https://org-token-tool.intern.dev.nav.no/). Sett scope til `dev-gcp.nom.nom-api`, kopier token og paste det inn i swagger.
3. Under `Admin for å manuelt trigge dvh Last`, kjør de endepunktene du trenger.