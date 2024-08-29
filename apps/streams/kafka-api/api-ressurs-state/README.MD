# Hvordan Resette State Streamen

Hvis State Streamen har parkert på en 'poison pill', og eneste mulighet til å korrigere dette er endring i
selve state stream topologien, eller å måtte gå ett steg tilbake i topologi-prosesseringen så må denne oppskriften følges.

## Pass på at org-kafka-tools er deployet med gyldig referanse til Aiven Secret

Kjør denne workflowen https://github.com/navikt/org-kafka-tools/actions/workflows/deploy-kafka-cli.yml

Med Aiven secret referanse. Tilsvarende f.eks. `aiven-nom-research-kafka-api-ressurs-state-9d146b7d-2024-34` <br>
Kan finnes med
```shell
kubectl get secret -n org | grep nom-kafka-api-ressurs-state
```
NB! Er forskjellige secrets i PROD og DEV


## Finn korrekte offset for input stream(ene)

Hvis State Streamen skal resettes så ønsker vi ikke å produsere State meldingene fra start på nytt igjen.
Vi ønsker kun å produsere nye state meldinger med endringer fra og med offseten til dit streamen har kommet ved feil.
Dette er forskjellig i prod og dev, så vær oppmerksom på dette.  

Consumer Group navnet er det samme som `spring.kafka.streams.application-id` i `application.yaml`.
Hvis denne baserer seg på nais environment propertyen `${KAFKA_STREAMS_APPLICATION_ID}` så er denne det
samme som Nais applikasjonsnavnet (inkludert namespace foran) med `_stream_` lagt til på slutten

I denne appen er `application-id/consumer-group-id`: `org.nom-kafka-api-ressurs-state_stream_-2`

Offset kan finnes med:
```shell
kubectl exec -i deploy/kafka-cli -- kafka-consumer-groups --group org.nom-kafka-api-ressurs-state_stream_-2 --describe
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

For å resette streamen må applikasjonen stoppes. Dette kan gjøres med å sette replica til 0.
```shell
kubectl scale deploy nom-kafka-api-ressurs-state --replicas=0
```

Deretter kan man resette streamen. (https://docs.confluent.io/platform/current/streams/developer-guide/app-reset-tool.html) <br> 
Dette gjøres med:
```shell
kubectl exec -i deploy/kafka-cli -- kafka-streams-application-reset --application-id org.nom-kafka-api-ressurs-state_stream_-2 --input-topics org.nom.ressurs-eventsource,org.nom.orgtilknytning-eventsource
```
(Husk alle input topics)

Så kan man starte applikasjonen igjen med:
```shell
kubectl scale deploy nom-kafka-api-ressurs-state --replicas=1
```