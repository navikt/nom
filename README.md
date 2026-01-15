# NAV Organization Master (NOM)

Docs: https://navikt.github.io/nom/
## Oppsett for pre-commit trigger
Sett opp slik at pre-commit trigger kjøres lokalt på din maskin ved commit for å søke etter secrets, credentials og personinfo i endringer som sjekkes inn.

### Installering
Installer pre-commit på maskinen (trenger kun å kjøres én gang) (https://pre-commit.com/#install)
```shell
pip install pre-commit
```
Installer GitLeaks på maskinen (trenger kun å kjøres én gang) (https://github.com/gitleaks/gitleaks)
Eksempel:
```shell
brew install gitleaks
```
#### Installasjon med nix
```shell
nix profile add nixpkgs#pre-commit
nix profile add nixpkgs#gitleaks
```
### Verifiser installering
```shell
pre-commit --version
gitleaks version
```

### Aktivere pre-commit i prosjektet
Installer pre-commit hooks i github-prosjektet (trenger kun å kjøres én gang per prosjekt)
```shell
pre-commit install
```
Nå skal GitLeaks kjøre på alle endringer som forsøkes å commit'es. Finner den noe mistenkelig vil den stoppe commit'en og vise hva som er funnet.

Commit output skal vise noe slikt som dette:
    
    Detect hardcoded secrets using Gitleaks..................................Passed

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

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen [#nom](https://slack.com/app_redirect?team=T5LNAMWNA&channel=nom)

## Kode generert av GitHub Copilot
Dette repoet bruker GitHub Copilot til å generere kode.