# Graphql-by-staterepo-core

Setter opp GraphQL resolvere og `SchemaMapping`er for NOM-API sitt graphql-skjema.

## Diverse

Konsument av denne maven-modulen må:
- sette opp beans for alle nødvendige `StateRepository` klasser, fra `libs/nom-state` maven-modulen
- sette opp bean for en controller som implementerer `NomGraphQlControllerInterface`
- sette opp bean som implementerer `NomGraphQlSupportInterface`
  - Interface tilbyr epost, remedyId, persistering av felt-bruk-statistikk, og flagg om lucene-indeksering må avventes 
- importere dependency `spring-graphql-starter`.

## Gjenstående

- Flytte testene fra API ned i denne modulen?
- Flytte ned kode for oppsett av felt-statistikk?
- Integrere spring-security direkte?