# NAV ident komponent

## Lokal kjøring

Hent aad-properties fra dev-gcp og legg disse inn i application-local.yaml
Kjør NavIdent med VM options

    -Dspring.profiles.active=local

### Hente AAD properties

Kan hentes med dette shell scriptet. Krever jq installert (brew install jq)

    #!/bin/sh
    CTX=dev-gcp
    NS=org
    SECRET_NAME=`kubectl get secrets --sort-by='{metadata.creationTimestamp}' --context=${CTX} --namespace=${NS}|grep ^azure-nav-ident|grep -v token|tail -1|cut -d ' ' -f1`
    SECRET=`kubectl get secret $SECRET_NAME -o json --context=${CTX} --namespace=${NS}`
    CLIENT_ID=`echo $SECRET |jq '.data.AZURE_APP_CLIENT_ID' -r|base64 -d`
    TENANT_ID=`echo $SECRET |jq '.data.AZURE_APP_TENANT_ID' -r|base64 -d`
    JWK=`echo $SECRET |jq '.data.AZURE_APP_JWK' -r|base64 -d`
    echo
    echo 'AZURE_APP_TENANT_ID: '$TENANT_ID
    echo 'AZURE_APP_CLIENT_ID: '$CLIENT_ID
    echo 'AZURE_APP_JWK: '\'$JWK\'

### Curl eksempel for å teste

Her er eksempel Curl som kan brukes. Denne personid'en finnes også i PDL test, så den skal treffe der.
Husk å sett inn JWT token som kan lages på IDA. ida.intern.nav.no -> AAD On-Behalf-Of Token for navIdent Client Id (AZURE_APP_CLIENT_ID)

    curl -i --location --request POST 'http://localhost:8081/navident' \
    -H 'Content-Type: application/json' \
    -H 'Nav-Consumer-Id: Test' \
    -H 'Nav-Call-Id: Test_id_2' \
    -H 'Authorization: Bearer <JWT-PLACEHOLDER>' \
    --data-raw '{
        "personid": "71106949829",
        "identType": "STANDARD",
        "identPrefix": "A"
    }'