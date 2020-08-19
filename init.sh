#!/bin/bash

setenv () {
  if [ -f "$2" ]; then
      export $1="$(cat $2)"
  fi
}

# skjermede-personer
setenv AAD_CLIENT_ID      /var/run/secrets/nais.io/azuread/client_id
setenv AAD_CLIENT_SECRET  /var/run/secrets/nais.io/azuread/client_id