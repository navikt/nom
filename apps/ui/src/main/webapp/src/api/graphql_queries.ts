export function createRessursQuery(id: string | null): string {
  const whereClause = id ? `where: {navIdent: "${id}"}` : "where: null";
  return `query {
          ressurs(${whereClause}) {
            navIdent
            personIdent
            epost
            person {
              navn {
                fornavn
                mellomnavn
                etternavn
              }
            }
            ledere {
              ressurs{
                navIdent
                person {
                  navn {
                    fornavn
                    mellomnavn
                    etternavn
                  }
                }
              }
            }
            koblinger {
              organisasjonsenhet{
                navn
                agressoId
              }
            }
            lederFor {
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
            underenheter: organiseringer(retning: under) {
              organisasjonsenhet{
                navn
                agressoId
                type {
                  kode
                  navn
                }
              }
            }
            overenheter: organiseringer(retning: over) {
              retning
              organisasjonsenhet{
                navn
                agressoId
                organiseringer(retning: over) {
                  retning
                  organisasjonsenhet{
                    navn
                    agressoId
                    organiseringer(retning: over) {
                      retning
                      organisasjonsenhet{
                        navn
                        agressoId
                        organiseringer(retning: over) {
                          retning
                          organisasjonsenhet{
                            navn
                            agressoId
                            organiseringer(retning: over) {
                              retning
                              organisasjonsenhet{
                                navn
                                agressoId
                                organiseringer(retning: over) {
                                  retning
                                  organisasjonsenhet{
                                    navn
                                    agressoId
                                    organiseringer(retning: over) {
                                      retning
                                      organisasjonsenhet{
                                        navn
                                        agressoId
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            leder{
              ressurs{
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
            type {
              kode
              navn
            }
          }
      }`;
}
