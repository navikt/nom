export function createRessursQuery(id: string | null): string {
  const whereClause = id ? `where: {navIdent: "${id}"}` : "where: null";
  return `query {
          ressurs(${whereClause}) {
              navIdent
              personIdent
              person {
                navn {
                  fornavn
                  mellomnavn
                  etternavn
                }
              }
              koblinger {
                organisasjonsenhet{
                  navn
                  agressoId
                }
              }
          }
      }`;
}

export function createOrganisasjonsenhetQuery(id: string | null): string {
  const whereClause = id ? `where: {agressoId: "${id}"}` : "where: null";
  return `query {
          organisasjonsenhet(${whereClause}) {
              agressoId
              navn
              gyldigFom
              gyldigTom
              organiseringer {
                retning
                organisasjonsenhet{
                  navn
                  agressoId
                }
              }
              leder{
                ressurs{
                    navIdent
                    personIdent
                }
            }
            koblinger {
                  ressurs {
                      navIdent
                      personIdent
                      person {
                        navn {
                          fornavn
                          mellomnavn
                          etternavn
                        }
                      }
                  }
              }
          }
      }`;
}
