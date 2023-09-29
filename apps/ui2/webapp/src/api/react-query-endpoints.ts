/* eslint-disable unicorn/no-await-expression-member*/
import axios from "axios";

export const ressurserKeys = {
  all: ["ALLE_RESSURSER"] as const,
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
                            }
                        }
                        ledere {
                            ressurs {
                                navident
                            }
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
    };
  }[];
  ledere: {
    ressurs: {
      navident: string;
    };
  }[];
  visningsnavn: string;
  sektor: string[];
  sluttdato: string;
  startdato: string;
};
