#!/bin/sh
CTX=dev-gcp
NS=org
SECRET_NAME=`kubectl get secrets --sort-by='{metadata.creationTimestamp}' --context=${CTX} --namespace=${NS}|grep ^azure-nom-unit4|grep -v token|tail -1|cut -d ' ' -f1`
SECRET=`kubectl get secret $SECRET_NAME -o json --context=${CTX} --namespace=${NS}`
CLIENT_ID=`echo $SECRET |jq '.data.AZURE_APP_CLIENT_ID' -r|base64 -d`
TENANT_ID=`echo $SECRET |jq '.data.AZURE_APP_TENANT_ID' -r|base64 -d`
JWK=`echo $SECRET |jq '.data.AZURE_APP_JWK' -r|base64 -d`
echo
echo 'AZURE_APP_TENANT_ID: '$TENANT_ID
echo 'AZURE_APP_CLIENT_ID: '$CLIENT_ID
echo 'AZURE_APP_JWK: '\'$JWK\'