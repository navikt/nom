export function createRessursQuery(id: string | null): string {
  const whereClause = id ? `where: {navIdent: "${id}"}` : "where: null";
  return `query {
          ressurs(${whereClause}) {
              navIdent
              personIdent
              koblinger {
                organisasjonsenhet{
                  navn
                  id
                }
              }
          }
      }`;
}

export function createOrganisasjonsenhetQuery(id: string | null): string {
  const whereClause = id ? `where: {id: "${id}"}` : "where: null";
  return `query {
          organisasjonsenhet(${whereClause}) {
              id
              navn
              gyldigFom
              gyldigTom
              organiseringer {
                retning
                organisasjonsenhet{
                  navn
                  id
                }
              }
              koblinger {
                  ressurs {
                      navIdent
                  }
              }
          }
      }`;
}
