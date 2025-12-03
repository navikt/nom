# Nom GraphQL

Microservice som implementerer en graphql tjeneste som følger NOM-API sitt graphql skjema.

## Diverse

Konsument av denne maven-modulen bør importere `NomGraphQlAutoConfiguration`, og lage en controller som implementerer `NomGraphQlControllerInterface`, og importere `spring-graphql-starter`.

## Lokalkjøring

Kjør `NomGqlApplication.java`

Ved lokalkjøring, så trengs det en JSON fil som inneholder state objekter. Filsti kommer som prompt i en dialog, eller kan settes
som første argument i run-configuration.

### Generering av hierarki JSON fil

Kjør `InitHierarkiByBuilder` i `test/.../hierarchygeneration/` , som lager en JSON fil i riktig format i tmp mappa. Eksakt filnavn
printes i konsoll.


## Gjenstående

- Få med `@PreAuthorize` ? Denne skal være av for lokalkjøring, men på i API
- Flytte testene fra API ned i denne modulen?
- Integrere spring-security direkte?