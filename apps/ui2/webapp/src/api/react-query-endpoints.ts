/* eslint-disable unicorn/no-await-expression-member*/
import axios from "axios";

export const ressurserKeys = {
  all: ["ALLE_RESSURSER"] as const,
};

export const underLiggendeOrgEnheterKeys = {
  all: ["ALLE_UNDERLIGGENDE_ORGENHETER"] as const,
  childOrgEnheter: (parent: string) =>
    [...underLiggendeOrgEnheterKeys.all, parent] as const,
};

export const getAlleRessurser = {
  queryKey: ressurserKeys.all,
  queryFn: async () =>
    (
      await axios.post<GraphqlResponseWrapper>("/nom-api/graphql", {
        operationName: "HentAlleRessurser",
        query: `
            query HentAlleRessurser {
                ressurser(where: {}) {
                    ressurs {
                        navident,
                        orgTilknytning{
                            orgEnhet {
                                id
                                navn
                            }
                            gyldigFom
                            gyldigTom
                        }
                        ledere {
                            ressurs {
                                navident
                                visningsnavn
                            }
                            gyldigFom
                            gyldigTom
                        }
                        visningsnavn
                        sektor
                        sluttdato
                        startdato
                    }
                }
            }
            `,
        variables: {},
      })
    ).data.data.ressurser.map((ressurs) => ressurs.ressurs),
};

export const getUnderliggendeOrgenheter = {
  queryKey: (parentId: string) =>
    underLiggendeOrgEnheterKeys.childOrgEnheter(parentId),
  queryFn: async (parentId: string): Promise<string[]> =>
    (
      await axios.get<string[]>(
        `/nom-api/app/orgenhet/underLiggende/${parentId}`,
      )
    ).data,
};

type GraphqlResponseWrapper = {
  data: {
    ressurser: {
      ressurs: RessursFields;
    }[];
  };
};

export type RessursFields = {
  navident: string;
  orgTilknytning: {
    orgEnhet: {
      id: string;
      navn: string;
    };
    gyldigFom: string;
    gyldigTom: string;
  }[];
  ledere: {
    ressurs: {
      navident: string;
      visningsnavn: string;
    };
    gyldigFom: string;
    gyldigTom: string;
  }[];
  visningsnavn: string;
  sektor: string[];
  startdato: string;
  sluttdato: string;
};
