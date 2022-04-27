import { agressoUrlToData } from "../common/url_utils";

export function createRessursQuery(id: string | null): string {
  const whereClause = id ? `where: {navIdent: "${id}"}` : "where: null";
  return `query {
          ressurs(${whereClause}) {
            navIdent
            personIdent
            epost
            ressurstype
            sluttdato
            person {
              navn {
                fornavn
                mellomnavn
                etternavn
              }
            }
            telefon {
              nummer 
              beskrivelse 
              type
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
                telefon {
                  nummer 
                  beskrivelse 
                  type
                }
              }
            }
            koblinger {
              organisasjonsenhet{
                navn
                agressoId
                orgNiv
              }
            }
            lederFor {
              organisasjonsenhet{
                navn
                agressoId
                orgNiv
              }
            }
          }
      }`;
}

export function createOrganisasjonsenhetQuery(
  agressoIdUrl: string | undefined
): string {
  function agressoUrlToWhereClause(agrIdUrl: string): any {
    const agressoData = agressoUrlToData(agrIdUrl);
    return `where: {agressoId: "${agressoData.agressoId}", orgNiv: "${agressoData.agressoNiva}", navn: "${agressoData.enhetNavn}"}`;
  }

  const whereClause = agressoIdUrl
    ? `${agressoUrlToWhereClause(agressoIdUrl)}`
    : "where: null";

  return `query {
          organisasjonsenhet(${whereClause}) {
            agressoId
            orgNiv
            navn
            gyldigFom
            gyldigTom
            underenheter: organiseringer(retning: under) {
              organisasjonsenhet{
                navn
                agressoId
                orgNiv
                gyldigFom
                gyldigTom
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
                orgNiv
                organiseringer(retning: over) {
                  retning
                  organisasjonsenhet{
                    navn
                    agressoId
                    orgNiv
                    organiseringer(retning: over) {
                      retning
                      organisasjonsenhet{
                        navn
                        agressoId
                        orgNiv
                        organiseringer(retning: over) {
                          retning
                          organisasjonsenhet{
                            navn
                            agressoId
                            orgNiv
                            organiseringer(retning: over) {
                              retning
                              organisasjonsenhet{
                                navn
                                agressoId
                                orgNiv
                                organiseringer(retning: over) {
                                  retning
                                  organisasjonsenhet{
                                    navn
                                    agressoId
                                    orgNiv
                                    organiseringer(retning: over) {
                                      retning
                                      organisasjonsenhet{
                                        navn
                                        agressoId
                                        orgNiv
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
                telefon {
                  nummer 
                  beskrivelse 
                  type
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
                telefon {
                  nummer 
                  beskrivelse 
                  type
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
