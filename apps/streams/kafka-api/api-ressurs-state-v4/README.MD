# Hvordan Resette State Streamen

Hvis State Streamen har parkert på en 'poison pill', og eneste mulighet til å korrigere dette er endring i
selve state stream topologien, eller å måtte gå ett steg tilbake i topologi-prosesseringen så må denne oppskriften følges.

## Installer Kafka CLI tools lokalt

Finnes her https://docs.confluent.io/kafka/operations-tools/kafka-tools.html

For å bruke de, må man hente temp creds fra nais https://cli.nais.io/nais_kafka_credentials.html
Hent med 

```shell
nais kafka credentials --environment dev --team org --ttl 1d --output java
```

Den lager en config fil - som i toppen angir USERNAME man evt må bruke på `nais kafka grant-access`, og hvilken bootstrap server som skal brukes
Deretter kan man kjøre kafka cli ved å angi denne config filen med `--command-config` flagget

## Finn korrekte offset for input stream(ene)

Hvis State Streamen skal resettes så ønsker vi ikke å produsere State meldingene fra start på nytt igjen.
Vi ønsker kun å produsere nye state meldinger med endringer fra og med offseten til dit streamen har kommet ved feil.
Dette er forskjellig i prod og dev, så vær oppmerksom på dette.  

Consumer Group navnet er det samme som `spring.kafka.streams.application-id` i `application.yaml`.
Hvis denne baserer seg på nais environment propertyen `${KAFKA_STREAMS_APPLICATION_ID}` så er denne det
samme som Nais applikasjonsnavnet (inkludert namespace foran) med `_stream_` lagt til på slutten

I denne appen er `application-id/consumer-group-id`: `org.nom-kafka-api-ressurs-state-v4_stream_-4`

Offset kan finnes med:
```shell
kafka-consumer-groups --group org.nom-kafka-api-ressurs-state-v4_stream_-4 --describe --bootstrap-server nav-dev-kafka-nav-dev.aivencloud.com:26484 --command-config /var/folders/bw/yhpg1_4s1xqg9jplky8k_9740000gn/T/nais-kafka-4076457192/kafka.properties
```
Legg disse i `application.yaml` for streams applikasjonen og redeploy

NB! Merk at appen kan ved restart under feil avansere offset, hvis det er downstream i topologien at poison-pill feilen trigges.
Dette gjør at siste offset hentet over, ikke nødvendigvis er korrekt, og kan gjøre at noen state meldinger vil hoppes over.
Da må man heller finne offset for første faktiske 'poison-pill' event, og benytte dennes offset.

## For å resette Streamen  

Deploy stream applikasjonen med nødvendige korreksjoner av topologi/kode for å kunne håndtere 'poison pill'-en.
Og - aller viktigst - korrekte offset satt som definert over.
Streamen vil feile på oppstart når det har vært endringer som krever at state streamen resettes. 

NB! Husk på hvilken k8s context du er i (dev/prod)

## Resette streams appen via Kafka CLI (funker ikke helt per i dag)

Får å resette med `kafka-streams-application-reset` må man grante accsess på input topicene (cli må resette consumer group offset).
Det kan gjøres med 
```shell
nais kafka grant-access tmp-kafka-topic-319976 nom.ressurs-eventsource -a readwrite -e dev-gcp -t org
nais kafka grant-access tmp-kafka-topic-319976 nom.orgtilknytning-eventsource -a readwrite -e dev-gcp -t org
```

I tillegg må man få grantet access til Streams appen i nais (slik at interne streams topics kan slettes)
!! Dette fungerer ikke per i dag https://nav-it.slack.com/archives/C73B9LC86/p1779950640577449
Bruk heller metode i kapitlet under

For å resette streamen må applikasjonen stoppes. Dette kan gjøres med å sette replica til 0.
```shell
kubectl scale deploy nom-kafka-api-ressurs-state --replicas=0
```

Deretter kan man resette streamen. (https://docs.confluent.io/platform/current/streams/developer-guide/app-reset-tool.html) <br> 
Dette gjøres med:
```shell
kafka-streams-application-reset --application-id org.nom-kafka-api-ressurs-state-v4_stream_-4 --input-topics org.nom.ressurs-eventsource,org.nom.orgtilknytning-eventsource --bootstrap-server nav-dev-kafka-nav-dev.aivencloud.com:26484 --command-config /var/folders/bw/yhpg1_4s1xqg9jplky8k_9740000gn/T/nais-kafka-4076457192/kafka.properties
```
(Husk alle input topics)

Så kan man starte applikasjonen igjen med:
```shell
kubectl scale deploy nom-kafka-api-ressurs-state --replicas=1
```

## Resette streams appen via versjonering av streams application-id

Lager man en ny 'streams application id' så vil dette være en helt ny stream, med nye consumer grupper og nye interne topics

Har nettopp gjort dette i dev med å sette:
```yaml
....
--- # DEV config
spring:
    config:
        activate:
            on-profile: dev && !prod

    kafka:
        streams:
            application-id: ${KAFKA_STREAMS_APPLICATION_ID}-5 # Versjonert app id for å få nye consumer grupper
```
