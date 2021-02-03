export function createRessursQuery(id: string | null): string {
  const whereClause = id ? `where: {navIdent: "${id}"}` : "where: null";
  return `query {
          ressurs(${whereClause}) {
              navIdent
              personIdent
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
              koblinger {
                  ressurs {
                      navIdent
                      personIdent
                  }
              }
          }
      }`;
}
