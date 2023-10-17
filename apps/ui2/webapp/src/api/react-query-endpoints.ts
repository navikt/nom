/* eslint-disable unicorn/no-await-expression-member*/
import type { AxiosResponse } from "axios";
import axios from "axios";

import type { Sektor } from "~/api/generated/graphql_generated";

export const ressurserKeys = {
  all: ["ALLE_RESSURSER_GRAPHQL"] as const,
};

export const ressurserFromRestKeys = {
  all: ["ALLE_RESSURSER_REST"] as const,
  id: (id: string) => [id, ...ressurserFromRestKeys.all] as const,
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

export const getRessursByRest = {
  queryKey: (navident: string) => ressurserFromRestKeys.id(navident),
  queryFn: async (navident: string) =>
    (await axios.get<RessursState>(`/nom-api/app/ressurs/${navident}`)).data,
};

export const updateNavnForRessurs = async (
  navident: string,
  updateNavnForRessursRequest: unknown,
) =>
  (
    await axios.post<unknown, AxiosResponse<unknown>>(
      `/nom-api/app/ressurs/${navident}/navn`,
      updateNavnForRessursRequest,
    )
  ).data;

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

export type RessursState = {
  fid: string;
  personident: string;
  navident: string;
  sektor: Sektor;
  visningsnavn: {
    fornavn: string;
    mellomnavn?: string;
    etternavn: string;
  };
  sluttdato?: string;
  startdato?: string;
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
