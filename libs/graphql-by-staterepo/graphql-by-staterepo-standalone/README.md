# Graphql-by-staterepo-standalone

Kjørbar applikasjon som implementerer en graphql tjeneste som følger NOM-API sitt graphql skjema. Basert på `graphql-by-staterepo-core`

## Lokalkjøring

Kjør `NomGqlApplication.java`

Ved lokalkjøring, så trengs det en JSON fil som inneholder state objekter. Filsti kommer som prompt i en dialog, eller kan settes
som første argument i run-configuration.

Deretter kan man bruke graphiql for testing. http://localhost:9038/graphiql?path=/graphql

### Generering av hierarki JSON fil

Kjør `InitHierarkiByBuilder` i `.../hierarchygeneration/` , som lager en JSON fil i riktig format i tmp mappa. Eksakt filnavn
printes i konsoll.