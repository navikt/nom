# NAV Organization Master (NOM)

Docs: https://navikt.github.io/nom/

## Hente AAD properties

For å hente AAD properties så gjør følgende fra terminal i github-prosjektets rot.

Hente eksterne python avhengigheter (trenger kun å kjøres én gang):
```shell
pip3 install -r requirements.txt
```
Logg på kubectl og kjør python-script som oppretter/oppdaterer application-local.yaml filer og .env filer med oppdaterte AAD properties:
```shell
gcloud auth login
python3 setup_local_azure_secrets.py
```

Alternativt kan secrets hentes med dette shell scriptet. Fyll inn riktig verdi for NS og APP og kjør direkte i terminal. Forutsetter at du er logget inn i kubernetes. Krever jq installert (brew install jq)

    #!/bin/sh
    CTX=dev-gcp
    NS={nom|org}
    APP={kubernetes_app_name}
    SECRET_NAME=`kubectl get secrets --sort-by='{metadata.creationTimestamp}' --context=${CTX} --namespace=${NS}|grep ^azure-${APP}|grep -v token|tail -1|cut -d ' ' -f1`
    SECRET=`kubectl get secret $SECRET_NAME -o json --context=${CTX} --namespace=${NS}`
    CLIENT_ID=`echo $SECRET |jq '.data.AZURE_APP_CLIENT_ID' -r|base64 -d`
    TENANT_ID=`echo $SECRET |jq '.data.AZURE_APP_TENANT_ID' -r|base64 -d`
    JWK=`echo $SECRET |jq '.data.AZURE_APP_JWK' -r|base64 -d`

    echo 'AZURE_APP_TENANT_ID: '$TENANT_ID
    echo 'AZURE_APP_CLIENT_ID: '$CLIENT_ID
    echo 'AZURE_APP_JWK: '\'$JWK\'

## Sette opp lokale kafka topics for nom entiteter

For å sette opp lokale kafka topics for nom entiteter (orgenhet, ressurs, orgtilknytning) så gjør følgende.

Kjør lokalt miljø:
```shell
docker-compose up -d
```
Deretter kjør følgende script:
```shell
python setup_local_kafka_topics.py
```

Skulle du ønske å resette (slette og opprette på nytt) noen av de lokale topicene kan du kjøre
```shell
python setup_local_kafka_topics.py --recreate
```

I tillegg kan du filtrere hvilke entiteter du ønsker å opprette eller resette 
```shell
python setup_local_kafka_topics.py --entites=orgenhet,ressurs --recreate
```