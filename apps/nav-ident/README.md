# NAV ident komponent

## Lokal kjøring

[Hent aad-properties fra dev-gcp](../../README.md#hente-aad-properties)

Kjør NavIdent med VM options

    -Dspring.profiles.active=local

### Curl eksempel for å teste

Her er eksempel Curl som kan brukes. Denne personid'en finnes også i PDL test, så den skal treffe der.
Husk å sett inn JWT token som kan lages på IDA. ida.intern.nav.no -> AAD On-Behalf-Of Token for navIdent Client Id (AZURE_APP_CLIENT_ID)

    curl -i --location --request POST 'http://localhost:8081/navident/create' \
    -H 'Content-Type: application/json' \
    -H 'Nav-Consumer-Id: Test' \
    -H 'Nav-Call-Id: Test_id_2' \
    -H 'Authorization: Bearer <JWT-PLACEHOLDER>' \
    --data-raw '{
        "personid": "71106949829",
        "identType": "STANDARD",
        "identPrefix": "A"
    }'