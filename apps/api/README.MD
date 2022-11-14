## Lokal kjøring

Hent aad-properties fra dev-gcp og legg disse inn i application-local.yaml
Kjør ApiStarter med VM options

    -Dspring.profiles.active=local

### Hente AAD properties

Kan hentes med dette shell scriptet. Krever jq installert (brew install jq)

    #!/bin/sh
    CTX=dev-gcp
    NS=nom
    SECRET_NAME=`kubectl get secrets --sort-by='{metadata.creationTimestamp}' --context=${CTX} --namespace=${NS}|grep ^azure-nom-api|grep -v token|tail -1|cut -d ' ' -f1`
    SECRET=`kubectl get secret $SECRET_NAME -o json --context=${CTX} --namespace=${NS}`
    CLIENT_ID=`echo $SECRET |jq '.data.AZURE_APP_CLIENT_ID' -r|base64 -d`
    TENANT_ID=`echo $SECRET |jq '.data.AZURE_APP_TENANT_ID' -r|base64 -d`
    JWK=`echo $SECRET |jq '.data.AZURE_APP_JWK' -r|base64 -d`

    echo 'AZURE_APP_TENANT_ID: '$TENANT_ID
    echo 'AZURE_APP_CLIENT_ID: '$CLIENT_ID
    echo 'AZURE_APP_JWK: '\'$JWK\'

### Kjøre med Fiddler Web Proxy for WebClient

Sett opp Fiddler med 'capture HTTPS traffic' og 'Ignore server certificate errors (unsafe)'
Under Connections, sett opp port 8866 (f.eks.), og 'Act as system proxy on startup'

I application-local må proxy.host og port settes. F.eks.

    proxy:
      host: 127.0.0.1
      port: 8866

Til slutt, kjør ApiStarter med proxy profil i tillegg

    -Dspring.profiles.active=local,proxy

