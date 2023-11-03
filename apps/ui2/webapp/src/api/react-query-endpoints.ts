/* eslint-disable unicorn/no-await-expression-member*/
import type { AxiosResponse } from "axios";
import axios from "axios";

const CACHE_FOR = {
  aMinute: 1000 * 60,
  anHour: 1000 * 60 * 60,
  aDay: 1000 * 60 * 60 * 24,
};

import type {
  OrgEnhet,
  Person,
  Ressurs,
  RessursLeder,
} from "~/types/api-models";
import type { OrgtilknytningState } from "~/types/api-models";

export const ressurserGraphqlKeys = {
  all: ["ALLE_RESSURSER_GRAPHQL"] as const,
};

export const ressurserKeys = {
  all: ["RESSURSER"] as const,
  id: (id: string) => [...ressurserKeys.all, id] as const,
  ledereTil: (navident: string) =>
    [...ressurserKeys.all, "LEDERE_Til", navident] as const,
};

export const orgEnheterKeys = {
  all: ["ORGENHETER"] as const,
  id: (id: string) => [...orgEnheterKeys.all, id] as const,
  ledetAv: (navident: string) =>
    [...ressurserKeys.all, "LEDET_AV", navident] as const,
};

export const orgtilknytningerKeys = {
  all: ["ORGTILKNYTNINGER"] as const,
  navident: (navident: string) =>
    [...orgtilknytningerKeys.all, navident] as const,
};

export const personKeys = {
  all: ["PERSONER"] as const,
  id: (id: string) => [...personKeys.all, id] as const,
};

export const underliggendeOrgEnheterKeys = {
  all: ["UNDERLIGGENDE_ORGENHETER"] as const,
  childOrgEnheter: (parent: string) =>
    [...underliggendeOrgEnheterKeys.all, parent] as const,
};

export const teamcatMemberships = {
  all: ["TEAMCAT_MEMBERSHIPS"] as const,
  navident: (navident: string) =>
    [...teamcatMemberships.all, navident] as const,
};

export const getAlleRessurserGraphql = {
  gcTime: CACHE_FOR.anHour,
  staleTime: CACHE_FOR.aMinute * 5,
  queryKey: ressurserGraphqlKeys.all,
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

export const getRessurs = {
  queryKey: (navident: string) => ressurserKeys.id(navident),
  queryFn: async (navident: string) =>
    (await axios.get<Ressurs>(`/nom-api/app/ressurser/byNavident/${navident}`))
      .data,
};

export const getLedereForRessurs = {
  queryKey: (navident: string) => ressurserKeys.ledereTil(navident),
  queryFn: async (navident: string) =>
    (
      await axios.get<RessursLeder[]>(
        `/nom-api/app/ressurser/lederFor/${navident}`,
      )
    ).data,
};

export const getAllOrgEnheter = {
  queryKey: orgEnheterKeys.all,
  queryFn: async () =>
    (await axios.get<OrgEnhet[]>("/nom-api/app/orgenheter")).data,
  gcTime: CACHE_FOR.anHour,
  staleTime: CACHE_FOR.aMinute * 5,
};

export const getOrgEnhet = {
  queryKey: (orgEnhetId: string) => orgEnheterKeys.id(orgEnhetId),
  queryFn: async (orgEnhetId: string) =>
    (await axios.get<OrgEnhet>(`/nom-api/app/orgenheter/${orgEnhetId}`)).data,
};

export const getOrgEnheterLedetAv = {
  queryKey: (navident: string) => orgEnheterKeys.ledetAv(navident),
  queryFn: async (navident: string) =>
    (await axios.get<OrgEnhet[]>(`/nom-api/app/orgenheter/ledetAv/${navident}`))
      .data,
};

export const getOrgTilknytninger = {
  queryKey: (navident: string) => orgtilknytningerKeys.navident(navident),
  queryFn: async (navident: string) =>
    (
      await axios.get<OrgtilknytningState[]>(
        `/nom-api/app/orgtilknytninger?navident=${navident}`,
      )
    ).data,
};

export const getPerson = {
  queryKey: (personident: string) => personKeys.id(personident),
  queryFn: async (personident: string) =>
    (
      await axios.get<Person>("/nom-api/app/person", {
        headers: { personident },
      })
    ).data,
};

export const getTeamcatMemberships = {
  queryKey: (navident: string) => teamcatMemberships.navident(navident),
  queryFn: async (navident: string) =>
    (await axios.get(`/teamcat/member/membership/${navident}`)).data,
};

export const updateNavnForRessurs = async (
  navident: string,
  updateNavnForRessursRequest: unknown,
) =>
  (
    await axios.post<unknown, AxiosResponse<unknown>>(
      `/nom-api/app/ressurser/${navident}/navn`,
      updateNavnForRessursRequest,
    )
  ).data;

export const getUnderliggendeOrgenheter = {
  queryKey: (parentId: string) =>
    underliggendeOrgEnheterKeys.childOrgEnheter(parentId),
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
