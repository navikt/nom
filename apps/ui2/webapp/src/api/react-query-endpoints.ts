/* eslint-disable unicorn/no-await-expression-member*/
import type { AxiosResponse } from "axios";
import axios from "axios";

const CACHE_FOR = {
  aMinute: 1000 * 60,
  anHour: 1000 * 60 * 60,
  aDay: 1000 * 60 * 60 * 24,
};

import type {
  AvvikleRessursRequest,
  EndreOrgtilknytningDatoRequest,
  GetPersonRequest,
  LeggTilOrgtilknytningRequest,
  OrgEnhet,
  OrgtilknytningState,
  Person,
  Ressurs,
  RessursLeder,
  UpdateFysiskeBrukerstederRequest,
} from "~/types/api-models";

export const ressurserGraphqlKeys = {
  all: ["ALLE_RESSURSER_GRAPHQL"] as const,
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
        "/nom-api/graphql",
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
    (await axios.get<Ressurs>(`/nom-api/app/ressurser/byNavident/${navident}`))
      .data,
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
      await axios.post<GetPersonRequest, AxiosResponse<Person>>(
        "/nom-api/app/person",
        { personident },
      )
    ).data,
};

export const getTeamcatMemberships = {
  queryKey: (navident: string) => teamcatMembershipsKeys.navident(navident),
  queryFn: async (navident: string) =>
    (await axios.get(`/teamcat/member/membership/${navident}`)).data,
};

export const getRessursEvents = {
  queryFn: async ({ pageParam }: { pageParam: number }) =>
    (
      await axios.get("/nom-api/app/ressurser/read-topic", {
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
      await axios.get("/nom-api/app/orgenheter/read-topic", {
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
    `/nom-api/app/ressurser/${navident}/navn`,
    updateNavnForRessursRequest,
  );
  await hopeForKafkaStateToUpdate();
};

export const updateOrgtilknytningDato = async (
  fid: string,
  endreOrgtilknytningDatoRequest: EndreOrgtilknytningDatoRequest,
) => {
  await axios.post<EndreOrgtilknytningDatoRequest, AxiosResponse<unknown>>(
    `/nom-api/app/orgtilknytninger/${fid}/dato`,
    endreOrgtilknytningDatoRequest,
  );
  await hopeForKafkaStateToUpdate();
};

export const leggTilOrgtilknytning = async (
  leggTilOrgtilknytningRequest: LeggTilOrgtilknytningRequest,
) => {
  await axios.post<LeggTilOrgtilknytningRequest, AxiosResponse<unknown>>(
    `/nom-api/app/orgtilknytninger`,
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
      `/nom-api/app/ressurser/${fid}/avvikle`,
      avvikleRessursRequest,
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

export const getFysiskeBrukersteder = {
  gcTime: CACHE_FOR.anHour,
  queryKey: ["FYSISKE_BRUKERSTEDER"],
  queryFn: async () =>
    (
      await axios.get<FysiskBrukersted[]>(
        "/nom-remedy-integrasjon/api/fysiskebrukersteder",
      )
    ).data,
};

export const updateFysiskeBrukersteder = async (
  fid: string,
  updateFysiskeBrukerstederRequest: UpdateFysiskeBrukerstederRequest,
) => {
  await axios.post<unknown, AxiosResponse<unknown>>(
    `/nom-api/app/ressurser/${fid}/fysiskebrukersteder`,
    updateFysiskeBrukerstederRequest,
  );
  await hopeForKafkaStateToUpdate();
};

async function hopeForKafkaStateToUpdate() {
  await new Promise((r) => setTimeout(r, 1000));
}

type RessurserWithMetadataGraphqlResponseWrapper = {
  data: {
    ressurserWithMetadata: RessursWithMetadata[];
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
