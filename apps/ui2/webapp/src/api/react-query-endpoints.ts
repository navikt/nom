/* eslint-disable unicorn/no-await-expression-member*/
import type { AxiosResponse } from "axios";
import axios from "axios";

export const CACHE_FOR = {
  aMinute: 1000 * 60,
  anHour: 1000 * 60 * 60,
  aDay: 1000 * 60 * 60 * 24,
};

import { queryOptions } from "@tanstack/react-query";

import type {
  AvvikleRessursRequest,
  EndreOrgtilknytningDatoRequest,
  GetPersonRequest,
  LederTilgang,
  LederTilgangForOrgenhetRequest,
  LeggTilOrgtilknytningRequestOld,
  OpprettRessursRequest,
  OpprettRessursResponse,
  OrgEnhet,
  OrgtilknytningState,
  Person,
  Ressurs,
  RessursLeder,
  SessionInfoResponse,
  UpdateFysiskeBrukerstederRequest,
  UpdatePrimarytelefonnummer,
} from "~/types/api-models";

const NOM_API_BASE_PATH = "/frackend/nom-api";
const NOM_REMEDY_INTEGRATION_API_BASE_PATH = "/frackend/nom-remedy-integrasjon";

/**
 * Intercept errors that are 401.
 * This assumes that Wonderwall is the service giving 401 errors when there is no active session.
 * Requests are in those cases short-circuited by Wonderwall. https://docs.nais.io/security/auth/wonderwall/?h=wonderwa#11-redirect-after-login
 * When Wonderwall respons with 401 it also supplies a location header that will take the user to login page and be redirected to where they were: /oauth2/login?redirect=${currentPath}
 *
 * To make this work there are a few caveats:
 * 1. autologin must be enabled
 * 2. All requests to Frackend must be ignored by autologin
 * 3. Except the route that serves the SPA. In our case this is the fallback "*" route.
 *
 * See this thread for additional context: https://nav-it.slack.com/archives/C5KUST8N6/p1694767530593689
 */
axios.interceptors.response.use(undefined, (error) => {
  if (error.response.status === 401) {
    window.location.assign(error.response.headers.location);
  }
  return Promise.reject(error);
});

/**
 * Anbefalt lesing for react-query key factory pattern: https://tkdodo.eu/blog/effective-react-query-keys
 */
export const ressurserGraphqlKeys = {
  all: ["ALLE_RESSURSER_GRAPHQL"] as const,
};

export const sessionKeys = {
  all: ["SESSION"] as const,
};

export const tilgangerKeys = {
  me: ["TILGANGER"] as const,
  delegerte: (navident: string) => [...tilgangerKeys.me, navident] as const,
  delegerteByUser: () => [...tilgangerKeys.me, "DELEGERT_AV_MEG"] as const,
};

export const ressurserKeys = {
  all: ["RESSURSER"] as const,
  id: (id: string) => [...ressurserKeys.all, id] as const,
  ledereTil: (navident: string) =>
    [...ressurserKeys.all, "LEDERE_Til", navident] as const,
  ressurserForOrgenhet: (orgenhetId: string) =>
    [...ressurserKeys.all, "FOR_ORGENHET", orgenhetId] as const,
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

export const teamcatMembershipsKeys = {
  all: ["TEAMCAT_MEMBERSHIPS"] as const,
  navident: (navident: string) =>
    [...teamcatMembershipsKeys.all, navident] as const,
};

export const eventSourceForRessursKeys = {
  all: ["RESSURS_EVENTS"] as const,
};

export const eventSourceForOrgEnhetKeys = {
  all: ["ORGENHET_EVENTS"] as const,
};

export const getAlleRessurserGraphql = {
  gcTime: CACHE_FOR.anHour,
  staleTime: CACHE_FOR.aMinute * 5,
  queryKey: ressurserGraphqlKeys.all,
  queryFn: async () =>
    (
      await axios.post<RessurserWithMetadataGraphqlResponseWrapper>(
        `${NOM_API_BASE_PATH}/graphql`,
        {
          operationName: "HentAlleRessurser",
          query: `
            query HentAlleRessurser {
                ressurserWithMetadata {
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
                    opprettetAv
                    opprettetDato
                    endretAv
                    endretDato
                    endringEvent
                }
            }
            `,
          variables: {},
        },
      )
    ).data.data.ressurserWithMetadata.map((ressursWithMetadata) => ({
      ...ressursWithMetadata.ressurs,
      opprettetAv: ressursWithMetadata.opprettetAv,
      opprettetDato: ressursWithMetadata.opprettetDato,
      endretAv: ressursWithMetadata.endretAv,
      endretDato: ressursWithMetadata.endretDato,
      endringEvent: ressursWithMetadata.endringEvent,
    })),
};

export const getRessurs = {
  queryKey: (navident: string) => ressurserKeys.id(navident),
  queryFn: async (navident: string) =>
    (
      await axios.get<Ressurs>(
        `${NOM_API_BASE_PATH}/app/ressurser/byNavident/${navident}`,
      )
    ).data,
};

export const getRessurserForOrgenhet = {
  queryKey: (orgenhetId: string) =>
    ressurserKeys.ressurserForOrgenhet(orgenhetId),
  queryFn: async (navidenter: string[]) =>
    await Promise.all(
      navidenter.map((navident) => getRessurs.queryFn(navident)),
    ),
};

export const getLedereForRessurs = {
  queryKey: (navident: string) => ressurserKeys.ledereTil(navident),
  queryFn: async (navident: string) =>
    (
      await axios.get<RessursLeder[]>(
        `${NOM_API_BASE_PATH}/app/ressurser/lederFor/${navident}`,
      )
    ).data,
};

export const getSession = {
  queryKey: sessionKeys.all,
  queryFn: async () =>
    (
      await axios.get<SessionInfoResponse>(
        `${NOM_API_BASE_PATH}/app/sessioninfo`,
      )
    ).data,
};

export const getAllOrgEnheter = {
  queryKey: orgEnheterKeys.all,
  queryFn: async () =>
    (await axios.get<OrgEnhet[]>(`${NOM_API_BASE_PATH}/app/orgenheter`)).data,
  gcTime: CACHE_FOR.anHour,
  staleTime: CACHE_FOR.aMinute * 5,
};

export const getOrgEnhet = {
  queryKey: (orgEnhetId: string) => orgEnheterKeys.id(orgEnhetId),
  queryFn: async (orgEnhetId: string) =>
    (
      await axios.get<OrgEnhet>(
        `${NOM_API_BASE_PATH}/app/orgenheter/${orgEnhetId}`,
      )
    ).data,
};

export function getOrgEnheterLedetAvOptions(navident: string) {
  return queryOptions({
    queryKey: orgEnheterKeys.ledetAv(navident),
    queryFn: async () =>
      (
        await axios.get<OrgEnhet[]>(
          `${NOM_API_BASE_PATH}/app/orgenheter/ledetAv/${navident}`,
        )
      ).data,
  });
}

export const getOrgTilknytninger = {
  queryKey: (navident: string) => orgtilknytningerKeys.navident(navident),
  queryFn: async (navident: string) =>
    (
      await axios.get<OrgtilknytningState[]>(
        `${NOM_API_BASE_PATH}/app/orgtilknytninger?navident=${navident}`,
      )
    ).data,
};

export const getPerson = {
  queryKey: (personident: string) => personKeys.id(personident),
  queryFn: async (personident: string) =>
    (
      await axios.post<GetPersonRequest, AxiosResponse<Person>>(
        `${NOM_API_BASE_PATH}/app/person`,
        { personident },
      )
    ).data,
};

export const getTeamcatMemberships = {
  queryKey: (navident: string) => teamcatMembershipsKeys.navident(navident),
  queryFn: async (navident: string) =>
    (await axios.get(`/frackend/teamcat/member/membership/${navident}`)).data,
};

export const getRessursEvents = {
  queryFn: async ({ pageParam }: { pageParam: number }) =>
    (
      await axios.get(`${NOM_API_BASE_PATH}/app/ressurser/read-topic`, {
        params: {
          partition: 0,
          maxRecords: 50,
          fromOffset: pageParam,
        },
      })
    ).data.reverse(),
};

export const getOrgEnhetEvents = {
  queryFn: async ({ pageParam }: { pageParam: number }) =>
    (
      await axios.get(`${NOM_API_BASE_PATH}/app/orgenheter/read-topic`, {
        params: {
          partition: 0,
          maxRecords: 50,
          fromOffset: pageParam,
        },
      })
    ).data.reverse(),
};

export const updateNavnForRessurs = async (
  navident: string,
  updateNavnForRessursRequest: unknown,
) => {
  await axios.post<unknown, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/ressurser/${navident}/navn`,
    updateNavnForRessursRequest,
  );
  await hopeForKafkaStateToUpdate();
};

export const addDelegering = async (
  request: LederTilgangForOrgenhetRequest,
) => {
  await axios.post<LederTilgangForOrgenhetRequest>(
    `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/addDelegatedAuthorization`,
    request,
  );
};

export const fornyDelegering = async (
  request: LederTilgangForOrgenhetRequest,
) => {
  await axios.post<LederTilgangForOrgenhetRequest>(
    `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/renewDelegatedAuthorization`,
    request,
  );
};

export const updateOrgtilknytningDato = async (
  fid: string,
  endreOrgtilknytningDatoRequest: EndreOrgtilknytningDatoRequest,
) => {
  await axios.post<EndreOrgtilknytningDatoRequest, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/orgtilknytninger/${fid}/dato`,
    endreOrgtilknytningDatoRequest,
  );
  await hopeForKafkaStateToUpdate();
};

export const leggTilOrgtilknytning = async (
  leggTilOrgtilknytningRequest: LeggTilOrgtilknytningRequestOld,
) => {
  await axios.post<LeggTilOrgtilknytningRequestOld, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/orgtilknytninger`,
    leggTilOrgtilknytningRequest,
  );
  await hopeForKafkaStateToUpdate();
};

export const avvikleRessurs = async (
  fid: string,
  avvikleRessursRequest: AvvikleRessursRequest,
) =>
  (
    await axios.post<AvvikleRessursRequest, AxiosResponse<unknown>>(
      `${NOM_API_BASE_PATH}/app/ressurser/${fid}/avvikle`,
      avvikleRessursRequest,
    )
  ).data;

export const getUnderliggendeOrgenheter = {
  queryKey: (parentId: string) =>
    underliggendeOrgEnheterKeys.childOrgEnheter(parentId),
  queryFn: async (parentId: string): Promise<string[]> =>
    (
      await axios.get<string[]>(
        `${NOM_API_BASE_PATH}/app/orgenheter/underliggende/${parentId}`,
      )
    ).data,
};

export const getFysiskeBrukersteder = {
  gcTime: CACHE_FOR.anHour,
  queryKey: ["FYSISKE_BRUKERSTEDER"],
  queryFn: async () =>
    (
      await axios.get<FysiskBrukersted[]>(
        `${NOM_REMEDY_INTEGRATION_API_BASE_PATH}/api/fysiskebrukersteder`,
      )
    ).data,
};

export function getTilgangerDelegertTilMegOptions() {
  return queryOptions({
    queryKey: tilgangerKeys.me,
    queryFn: async () =>
      (
        await axios.get<LederTilgang[]>(
          `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/getLederTilgangerDelegertTilMeg`,
        )
      ).data,
  });
}

export function getTilgangerDelegertByUserOptions() {
  return queryOptions({
    queryKey: tilgangerKeys.delegerteByUser(),
    queryFn: async () =>
      (
        await axios.get<LederTilgang[]>(
          `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/getDelegerteLederTilgangerAvUser`,
        )
      ).data,
  });
}

export const updateFysiskeBrukersteder = async (
  fid: string,
  updateFysiskeBrukerstederRequest: UpdateFysiskeBrukerstederRequest,
) => {
  await axios.post<unknown, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/ressurser/${fid}/fysiskebrukersteder`,
    updateFysiskeBrukerstederRequest,
  );
  await hopeForKafkaStateToUpdate();
};

export const createRessurs = async (request: OpprettRessursRequest) => {
  return axios.post<unknown, AxiosResponse<OpprettRessursResponse>>(
    `${NOM_API_BASE_PATH}/app/ressurser`,
    request,
  );
};

export const updateTelefonnummer = async (
  navident: string,
  request: UpdatePrimarytelefonnummer,
) => {
  return axios.post<unknown, AxiosResponse<UpdatePrimarytelefonnummer>>(
    `${NOM_API_BASE_PATH}/app/ressurser/${navident}/telefonnummer`,
    request,
  );
};

export const searchRessurser = {
  /*
   NOTE 22 Mar 2023 (Johannes Moskvil): This key function is not meant to be used as a function for the query itself.
   Since the query is for a search term, caching it makes little sense.
   It can however be useful to manually put the independent search result in the ressurs cache by its navident.
  */
  getKeyForNavident: (navident: string) => ressurserKeys.id(navident),
  queryFn: async (searchTerm: string) =>
    (
      await axios.post<
        GraphQlResponse<
          "searchRessurs",
          { visningsnavn: string; navident: string }[]
        >
      >(`${NOM_API_BASE_PATH}/graphql`, {
        query: `
        query SokRessurser($searchTerm: String!) {
          searchRessurs(term: $searchTerm, filter: {sektorSelection: IKKE_EKSTERN, statusSelection: AKTIV}){
            visningsnavn, navident
          }
        }`,
        variables: { searchTerm },
      })
    ).data.data.searchRessurs,
};

async function hopeForKafkaStateToUpdate() {
  await new Promise((r) => setTimeout(r, 1000));
}

type RessurserWithMetadataGraphqlResponseWrapper = {
  data: {
    ressurserWithMetadata: RessursWithMetadata[];
  };
};

export type GraphQlResponse<QueryKey extends string, ResponseType> = {
  data: {
    [key in QueryKey]: ResponseType;
  };
};

export type RessursWithMetadata = {
  ressurs: RessursFields;
  opprettetAv?: string;
  opprettetDato?: string;
  endretAv?: string;
  endretDato?: string;
  endringEvent?: string;
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
  opprettetAv?: string;
  opprettetDato?: string;
  endretAv?: string;
  endretDato?: string;
  endringEvent?: string;
};

export type FysiskBrukersted = {
  brukerstedNr: string;
  brukerstedNavn: string;
};
