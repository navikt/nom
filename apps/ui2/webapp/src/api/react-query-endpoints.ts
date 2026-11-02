import { queryOptions } from "@tanstack/react-query";
import type { AxiosResponse } from "axios";
import axios from "axios";

import type { MasseflyttingData } from "~/pages/ResourcePage/Masseflytting/Masseflytting";
import type {
  AvvikleOrgEnhetRequest,
  AvvikleRessursRequest,
  EndreOpphengOrgEnhetRequest,
  EndreOpphengOrgEnhetResponse,
  EnvironmentConfigResponse,
  ExcelExportRequest,
  FysiskBrukersted,
  GetPersonRequest,
  GlobalStatistikkResponse,
  KorrigerOrgEnhetNavnRequest,
  KostnadsstedResponse,
  LederTilgangForOrgenhetRequest,
  LederTilgangState,
  MasseFlyttingRequest,
  MinimalRessurs,
  OppdaterOrgtilknytningerForRessursRequest,
  OpprettOrgEnhetLederRequest,
  OpprettOrgEnhetRequest,
  OpprettOrgEnhetResponse,
  OpprettRessursRequest,
  OpprettRessursResponse,
  OrgEnhet,
  OrgEnhetKostnadsstedMappingResponse,
  OrgEnhetLederResponse,
  OrgEnhetMedLedernavn,
  OrgtilknytningState,
  OvrigeEndringerOrgEnhetRequest,
  Person,
  RedigeringsTilgangRessursResponse,
  Ressurs,
  RessurserByFysiskBrukerstedAndTilgangsenhetIdRequest,
  RessurserByFysiskBrukerstedAndTilgangsenhetIdResponse,
  RessursKontaktperson,
  RessursLeder,
  SessionInfoResponse,
  Tilgangsenhet,
  UpdateFysiskBrukerstedForOrgtilknytningRequest,
  UpdateFysiskeBrukerstederRequest,
  UpdatePrimaryFysiskBrukerstedForRessurserRequest,
  UpdatePrimarytelefonnummer,
} from "~/types/api-models";

export const CACHE_FOR = {
  aMinute: 1000 * 60,
  anHour: 1000 * 60 * 60,
  aDay: 1000 * 60 * 60 * 24,
};

const NOM_API_BASE_PATH = "/frackend/nom-api";
const NOM_PAARORENDE_BASE_PATH = "/frackend/paarorende";
const NOM_UNLEASH_BASE_PATH = "/frackend/unleash";

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
    globalThis.location.assign(error.response.headers.location);
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
  orgEnhetListe: (orgEnhetIder: string[]) =>
    [...ressurserKeys.all, "ORGENHET_LISTE", orgEnhetIder] as const,
};

export const orgtilknytningerKeys = {
  all: ["ORGTILKNYTNINGER"] as const,
  navident: (navident: string) =>
    [...orgtilknytningerKeys.all, navident] as const,
  fysiskBrukersted: (fysiskBrukerstedNr: string) =>
    [...orgtilknytningerKeys.all, fysiskBrukerstedNr] as const,
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

export const underliggendeOrgEnheterObjects = {
  all: ["UNDERLIGGENDE_ORGENHETER_OBJECTS"] as const,
  childOrgEnheter: (parent: string) =>
    [...underliggendeOrgEnheterObjects.all, parent] as const,
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

export const kostnadsstedKeys = {
  all: ["KOSTNADSSTED_BY_ORGENHET"] as const,
  orgEnhetId: (orgEnhetId: string) =>
    [...kostnadsstedKeys.all, orgEnhetId] as const,
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

export const getRessurserForFysiskBrukersted = {
  queryKey: (fysiskBrukerstedNr: string) => [
    "RESSURSER_BY_FYSISK_BRUKERSTED",
    ...fysiskBrukerstedNr,
  ],
  queryFn: async (fysiskBrukerstedNr: string) =>
    (
      await axios.get<Ressurs[]>(
        `${NOM_API_BASE_PATH}/app/ressurser/byFysiskbrukersted/${fysiskBrukerstedNr}`,
      )
    ).data,
};

export const getRessurserForFysiskBrukerstedAlle = {
  queryKey: (
    fysiskBrukerstedOgTilgangsenhetId: RessurserByFysiskBrukerstedAndTilgangsenhetIdRequest,
  ) => [
    "RESSURSER_BY_FYSISK_BRUKERSTED_YTTERLIGERE",
    fysiskBrukerstedOgTilgangsenhetId,
  ],
  queryFn: async (
    fysiskBrukerstedOgTilgangsenhetId: RessurserByFysiskBrukerstedAndTilgangsenhetIdRequest,
  ) =>
    (
      await axios.post<RessurserByFysiskBrukerstedAndTilgangsenhetIdResponse[]>(
        `${NOM_API_BASE_PATH}/app/ressurser/byFysiskbrukerstedIAktivOt`,
        fysiskBrukerstedOgTilgangsenhetId,
      )
    ).data,
};

export const downloadExcelRessurserForFysiskBrukerstedAlle = async (
  request: RessurserByFysiskBrukerstedAndTilgangsenhetIdRequest,
  excelDocumentNavn: string,
) => {
  const response = await axios.post(
    `${NOM_API_BASE_PATH}/app/ressurser/byFysiskbrukerstedIAktivOt/export`,
    request,
    {
      responseType: "blob",
    },
  );

  const url = globalThis.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", excelDocumentNavn);
  document.body.append(link);
  link.click();
  link.remove();
};

export const getRessurserByListNavident = {
  queryKey: (navidenter: string[]) => ["RESSURSER_BY_LIST", ...navidenter],
  queryFn: async (navidenter: string[]) =>
    (
      await axios.post<string[], AxiosResponse<Ressurs[]>>(
        `${NOM_API_BASE_PATH}/app/ressurser/byListNavident`,
        navidenter,
      )
    ).data,
};

export const getOrgtilknytningerByNavidentList = {
  queryKey: (navidenter: string[]) => [
    "ORGTILKNYTNINGER_BY_LIST",
    ...navidenter,
  ],
  queryFn: async (navidenter: string[]) =>
    (
      await axios.post<
        string[],
        AxiosResponse<Record<string, OrgtilknytningState[]>>
      >(`${NOM_API_BASE_PATH}/app/orgtilknytninger`, navidenter)
    ).data,
};

export const downloadExcelByListNavident = async (
  request: ExcelExportRequest,
  orgenhetNavn: string,
) => {
  const response = await axios.post(
    `${NOM_API_BASE_PATH}/app/ressurser/byListNavident/export`,
    request,
    {
      responseType: "blob",
    },
  );

  const url = globalThis.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", orgenhetNavn);
  document.body.append(link);
  link.click();
  link.remove();
};

export const downloadExcelKontaktpersonerForOrgenhetByOrgenhetId = async (
  orgEnhetId: string,
  excelDocumentNavn: string,
) => {
  const response = await axios.get(
    `${NOM_API_BASE_PATH}/app/orgenheter/exportKontaktpersonerFor/${orgEnhetId}`,
    {
      responseType: "blob",
    },
  );

  const url = globalThis.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", excelDocumentNavn);
  document.body.append(link);
  link.click();
  link.remove();
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

export const getEnvironmentConfig = {
  queryKey: ["ENVIRONMENTCONFIG"],
  queryFn: async () =>
    (
      await axios.get<EnvironmentConfigResponse>(
        `${NOM_API_BASE_PATH}/app/environmentconfig`,
      )
    ).data,
};

export const getAllOrgEnheter = {
  queryKey: () => orgEnheterKeys.all,
  queryFn: async () =>
    (await axios.get<OrgEnhet[]>(`${NOM_API_BASE_PATH}/app/orgenheter`)).data,
  gcTime: CACHE_FOR.anHour,
  staleTime: CACHE_FOR.aMinute * 5,
};

export const getAlleTilgangsenheter = {
  queryKey: () => ["ALLE_TILGANGSENHETER"],
  queryFn: async () =>
    (await axios.get<Tilgangsenhet[]>(`${NOM_API_BASE_PATH}/app/tilgangsenhet`))
      .data,
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

export const getOrgEnhetList = {
  queryKey: (orgEnhetIder: string[]) => ["ORGENHETER_LIST", ...orgEnhetIder],
  queryFn: async (orgEnhetIder: string[]) =>
    (
      await axios.post<string[], AxiosResponse<OrgEnhet[]>>(
        `${NOM_API_BASE_PATH}/app/orgenheter`,
        orgEnhetIder,
      )
    ).data,
};

export function getParentOgUnderliggende(orgEnhetId: string) {
  return queryOptions({
    queryKey: ["getParentOgUnderliggende", orgEnhetId],
    queryFn: async () =>
      (
        await axios.get<OrgEnhet[]>(
          `${NOM_API_BASE_PATH}/app/orgenheter/parent-og-underliggende/${orgEnhetId}`,
        )
      ).data,
    enabled: !!orgEnhetId,
  });
}

export const getAlleKostnadssteder = {
  queryKey: () => ["ALLE_KOSTNADSSTEDER"],
  queryFn: async () =>
    (
      await axios.get<KostnadsstedResponse[]>(
        `${NOM_API_BASE_PATH}/app/orgenheter/kostnadssted`,
      )
    ).data,
};

export const getKostnadssted = {
  queryKey: (orgEnhetId: string) => kostnadsstedKeys.orgEnhetId(orgEnhetId),
  queryFn: async (orgEnhetId: string) =>
    (
      await axios.get<OrgEnhetKostnadsstedMappingResponse>(
        `${NOM_API_BASE_PATH}/app/orgenheter/kostnadssted/${orgEnhetId}`,
      )
    ).data,
};

export const isOrgEnhetArbeidsomraade = {
  queryKey: (orgEnhetId: string) => ["IS_ARBEIDSOMRAADE", orgEnhetId],
  queryFn: async (orgEnhetId: string) =>
    (
      await axios.get<boolean>(
        `${NOM_API_BASE_PATH}/app/orgenheter/isArbeidsomraade/${orgEnhetId}`,
      )
    ).data,
};

export const isOrgEnhetDriftsenhet = {
  queryKey: (orgEnhetId: string) => ["IS_DRIFTSENHET", orgEnhetId],
  queryFn: async (orgEnhetId: string) =>
    (
      await axios.get<boolean>(
        `${NOM_API_BASE_PATH}/app/orgenheter/isDriftsenhet/${orgEnhetId}`,
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

export const getOrgEnheterLedetAvNavIdent = {
  queryKey: (navident: string) => ["getOrgEnheterLedetAvNavIdent", navident],
  queryFn: async (navident: string) =>
    (
      await axios.get<OrgEnhetLederResponse[]>(
        `${NOM_API_BASE_PATH}/app/orgenhetleder/by-navident/${navident}`,
      )
    ).data as OrgEnhetLederResponse[],
};

export const getOrgEnheterSomKanHaRessurser = {
  gcTime: CACHE_FOR.anHour,
  staleTime: CACHE_FOR.aMinute * 5,
  queryKey: ["ORGENHETER_KAN_HA_RESSURSER"],
  queryFn: async () =>
    (
      await axios.get<OrgEnhetMedLedernavn[]>(
        `${NOM_API_BASE_PATH}/app/orgenheter/kanHaRessurser`,
      )
    ).data,
};

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
  queryKey: (request: GetPersonRequest) => personKeys.id(request.personident),
  queryFn: async (request: GetPersonRequest) =>
    (
      await axios.post<GetPersonRequest, AxiosResponse<Person>>(
        `${NOM_API_BASE_PATH}/app/person`,
        request,
      )
    ).data,
};

export const getTeamcatMemberships = {
  queryKey: (navident: string) => teamcatMembershipsKeys.navident(navident),
  queryFn: async (navident: string) =>
    (await axios.get(`/frackend/teamcat/member/membership/${navident}`)).data,
};

export const getTeamcatSection = {
  queryKey: (id: string) => teamcatMembershipsKeys.navident(id),
  queryFn: async (id: string) =>
    (await axios.get(`/frackend/teamcat/productarea/nomid/${id}`)).data,
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

export function harLederTilgangForOrgenhetOptions(orgenhetId: string) {
  return queryOptions({
    queryKey: ["TILGANG", "ORGENHET", orgenhetId],
    queryFn: async () =>
      (
        await axios.get<boolean>(
          `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/isAuthorizedForOrgenhet/${orgenhetId}`,
        )
      ).data,
    enabled: !!orgenhetId,
  });
}

export const getOrgEnheterRessursKanRedigere = {
  queryKey: (navident?: string, kunToppnoder?: boolean) => [
    "ORGENHETER_RESSURS_KAN_REDIGERE",
    navident,
    kunToppnoder,
  ],
  queryFn: async (navident?: string, kunToppnoder?: boolean) => {
    const response = await axios.get<OrgEnhet[]>(
      `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/getOrgEnheterLederCanEdit`,
      {
        params: {
          ...(!!navident && { navident }),
          ...(!!kunToppnoder && { kuntoppnoder: true }),
        },
      },
    );

    return response.data;
  },
};

export const getLedereForOrgEnhet = {
  queryKey: (orgEnhetIder: string[]) => ["LEDERE_FOR_ORGENHET", orgEnhetIder],
  queryFn: async (orgEnhetIder: string[]) =>
    (
      await axios.post<
        string[],
        AxiosResponse<Record<string, RedigeringsTilgangRessursResponse[]>>
      >(
        `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/getLedereForOrgenheter`,
        orgEnhetIder,
      )
    ).data,
};

export function harLederTilgangForNavidentOptions(navident: string) {
  return queryOptions({
    queryKey: ["TILGANG", "NAVIDENT", navident],
    queryFn: async () =>
      (
        await axios.get<boolean>(
          `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/isAuthorizedForNavident/${navident}`,
        )
      ).data,
    enabled: !!navident,
  });
}

export function getKonaktpersonerForOrgEnhet(orgEnhetId: string) {
  return queryOptions({
    queryKey: ["TILGANG", "NAVIDENT", orgEnhetId],
    queryFn: async () =>
      (
        await axios.get<RessursKontaktperson[]>(
          `${NOM_API_BASE_PATH}/app/orgenheter/kontaktpersoner/${orgEnhetId}`,
        )
      ).data,
    enabled: !!orgEnhetId,
  });
}

export const fornyDelegering = async (
  request: LederTilgangForOrgenhetRequest,
) => {
  await axios.post<LederTilgangForOrgenhetRequest>(
    `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/renewDelegatedAuthorization`,
    request,
  );
};

// Skal antakelig bort, men i tilfelle
// export const updateOrgtilknytningDato = async (
//   fid: string,
//   endreOrgtilknytningDatoRequest: EndreOrgtilknytningDatoRequest,
// ) => {
//   await axios.post<EndreOrgtilknytningDatoRequest, AxiosResponse<unknown>>(
//     `${NOM_API_BASE_PATH}/app/orgtilknytninger/${fid}/dato`,
//     endreOrgtilknytningDatoRequest,
//   );
//   await hopeForKafkaStateToUpdate();
// };
//
// export const leggTilOrgtilknytning = async (
//   leggTilOrgtilknytningRequest: LeggTilOrgtilknytningRequestOld,
// ) => {
//   await axios.post<LeggTilOrgtilknytningRequestOld, AxiosResponse<unknown>>(
//     `${NOM_API_BASE_PATH}/app/orgtilknytninger`,
//     leggTilOrgtilknytningRequest,
//   );
//   await hopeForKafkaStateToUpdate();
// };

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

export const oppdaterOrgtilknytningerForRessurs = async (
  navident: string,
  oppdaterOrgtilknytningerRequest: OppdaterOrgtilknytningerForRessursRequest,
) =>
  (
    await axios.post<
      OppdaterOrgtilknytningerForRessursRequest,
      AxiosResponse<unknown>
    >(
      `${NOM_API_BASE_PATH}/app/orgtilknytninger/forRessurs/${navident}`,
      oppdaterOrgtilknytningerRequest,
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

export const getAlleUnderliggendeOrgEnheter = {
  queryKey: (parentId: string) =>
    underliggendeOrgEnheterObjects.childOrgEnheter(parentId),
  queryFn: async (parentId: string) =>
    (
      await axios.get<OrgEnhet[]>(
        `${NOM_API_BASE_PATH}/app/orgenheter/alle-underliggende/${parentId}`,
      )
    ).data,
};

export const getFysiskeBrukersteder = {
  gcTime: CACHE_FOR.anHour,
  queryKey: ["FYSISKE_BRUKERSTEDER"],
  queryFn: async () =>
    (
      await axios.get<FysiskBrukersted[]>(
        `${NOM_API_BASE_PATH}/app/fysiskeBrukersteder`,
      )
    ).data,
};

export function getTilgangerDelegertTilMegOptions() {
  return queryOptions({
    queryKey: tilgangerKeys.me,
    queryFn: async () =>
      (
        await axios.get<LederTilgangState[]>(
          `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/getLederTilgangerDelegertTilMeg`,
        )
      ).data,
  });
}

export function getTilgangerDelegertForOrgEnhetOptions() {
  return queryOptions({
    queryKey: tilgangerKeys.delegerteByUser(),
    queryFn: async () =>
      (
        await axios.get<LederTilgangState[]>(
          `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/getLederTilgangerDelegertForOrgEnhet`,
        )
      ).data,
  });
}

export function getDelegerbareOrgEnheterForUserOptions(navident: string) {
  return queryOptions({
    queryKey: ["DELEGERBARE_ORGENHETER", navident],
    queryFn: async () =>
      (
        await axios.get<OrgEnhet[]>(
          `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/isAuthorizedToDelegate/${navident}`,
        )
      ).data,
  });
}

export const getRessurserWhichCanBeDelegatedTo = {
  queryKey: ["DELEGERBARE_RESSURSER"],
  queryFn: async () =>
    (
      await axios.get<MinimalRessurs[]>(
        `${NOM_API_BASE_PATH}/app/authorizedRessursRedigering/getRessurserWhichCanBeDelegatedTo`,
      )
    ).data,
};

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

export const updateFysiskBrukerstedForRessurser = async (
  request: UpdatePrimaryFysiskBrukerstedForRessurserRequest,
) => {
  await axios.post<unknown, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/ressurser/updateFysiskBrukerstedForRessurserByNavidentList`,
    request,
  );
  await hopeForKafkaStateToUpdate();
};

export const updateFysiskBrukerstedForOrgtilknytninger = async (
  request: UpdateFysiskBrukerstedForOrgtilknytningRequest,
) => {
  await axios.post<unknown, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/orgtilknytninger/updateFysiskBrukerstedForOrgtilknytningByOrgtilknytningList`,
    request,
  );
  await hopeForKafkaStateToUpdate();
};

export function getAllActiveOrgtilknytningerWithGivenFysiskBrukerstedNr(
  fysiskBrukerstedNr: string,
) {
  return queryOptions({
    queryKey: orgtilknytningerKeys.fysiskBrukersted(fysiskBrukerstedNr),
    queryFn: async () =>
      (
        await axios.get<OrgtilknytningState[]>(
          `${NOM_API_BASE_PATH}/app/orgtilknytninger/getAllActiveOrgtilknytningerWithGivenFysiskBrukerstedNr/${fysiskBrukerstedNr}`,
        )
      ).data,
    staleTime: CACHE_FOR.aMinute * 5,
    gcTime: CACHE_FOR.anHour,
    enabled: !!fysiskBrukerstedNr,
  });
}

export const createRessurs = async (request: OpprettRessursRequest) => {
  return axios.post<unknown, AxiosResponse<OpprettRessursResponse>>(
    `${NOM_API_BASE_PATH}/app/ressurser`,
    request,
  );
};

export const getPeriodiserteLedereOrgEnhet = {
  queryKey: (id: string) => ["LEDERE_ORGENHET", id],
  queryFn: async (id: string): Promise<OrgEnhetLederResponse[]> =>
    (
      await axios.get<OrgEnhetLederResponse[]>(
        `${NOM_API_BASE_PATH}/app/orgenhetleder/byOrgEnhetId/${id}`,
      )
    ).data,
};

export const opprettOrgEnhet = async (request: OpprettOrgEnhetRequest) => {
  return axios.post<unknown, AxiosResponse<OpprettOrgEnhetResponse>>(
    `${NOM_API_BASE_PATH}/app/orgenheter/opprett`,
    request,
  );
};

export const korrigerOrgEnhetNavn = async (
  request: KorrigerOrgEnhetNavnRequest,
) => {
  return axios.patch<unknown, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/orgenheter/korrigerNavn`,
    request,
  );
};

export const opprettOrgEnhetLeder = async (
  request: OpprettOrgEnhetLederRequest,
) => {
  return axios.post<unknown, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/orgenhetleder/opprettLeder`,
    request,
  );
};

export const endreOrgEnhetInfo = async (
  request: OvrigeEndringerOrgEnhetRequest,
) => {
  return axios.patch<unknown, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/orgenheter/endreOvrige`,
    request,
  );
};

export const endreOrgEnhetOverliggende = async (
  request: EndreOpphengOrgEnhetRequest,
) => {
  return axios.post<unknown, AxiosResponse<EndreOpphengOrgEnhetResponse>>(
    `${NOM_API_BASE_PATH}/app/orgenheter/endreOpphengForOrgenhet`,
    request,
  );
};

export const avvikleOrgEnhet = async (
  orgEnhetId: string,
  request: AvvikleOrgEnhetRequest,
) => {
  return axios.patch<unknown, AxiosResponse<unknown>>(
    `${NOM_API_BASE_PATH}/app/orgenheter/avvikle/${orgEnhetId}`,
    request,
  );
};

export const masseflytting = async (request: MasseFlyttingRequest) => {
  return await axios.post<unknown, AxiosResponse<MasseflyttingData>>(
    `${NOM_API_BASE_PATH}/app/orgtilknytninger/masseflytting`,
    request,
  );
};

export const updateTelefonnummer = async (
  navident: string,
  request: UpdatePrimarytelefonnummer,
) => {
  await axios.post<unknown, AxiosResponse<UpdatePrimarytelefonnummer>>(
    `${NOM_API_BASE_PATH}/app/ressurser/${navident}/telefonnummer`,
    request,
  );
  await hopeForKafkaStateToUpdate();
};

export const getGlobalStatistikk = {
  queryKey: () => ["GLOBAL_STATISTIKK"],
  queryFn: async () =>
    (
      await axios.get<GlobalStatistikkResponse>(
        `${NOM_API_BASE_PATH}/app/statistikk`,
      )
    ).data,
};

export const getStatistikkForOrgEnhet = {
  queryKey: (id: string) => ["STATISTIKK_ORGENHET", id],
  queryFn: async (id: string) =>
    (
      await axios.get<GlobalStatistikkResponse>(
        `${NOM_API_BASE_PATH}/app/statistikk/${id}`,
      )
    ).data,
};

export const getParorendeForBruker = {
  queryKey: () => ["paarorende"],
  queryFn: async (): Promise<Kontaktinfo[]> =>
    (
      await axios.get<Kontaktinfo[]>(
        `${NOM_PAARORENDE_BASE_PATH}/api/kontaktinfo`,
      )
    ).data,
};

export const getParorendeForNavident = {
  queryKey: (navident: string) => ["paarorende", navident],
  queryFn: async (navident: string): Promise<Kontaktinfo[]> =>
    (
      await axios.get<Kontaktinfo[]>(
        `${NOM_PAARORENDE_BASE_PATH}/api/kontaktinfo/${navident}`,
      )
    ).data,
};

export const getHarParorendeTilgang = {
  queryKey: (navident: string) => ["paarorendeTilgang", navident],
  queryFn: async (navident: string): Promise<Kontaktinfo[]> =>
    (
      await axios.get<Kontaktinfo[]>(
        `${NOM_PAARORENDE_BASE_PATH}/api/kontaktinfo/tilgang/${navident}`,
      )
    ).data,
};

export const getSpaHtmlWithoutNonce = {
  queryKey: () => ["getSpaHtmlWithoutNonce"],
  queryFn: async (): Promise<string> => {
    const spaHtml = (await axios.get<string>(`/`)).data;
    return spaHtml.replaceAll(/nonce=\S+"/g, "");
  },
};

export const createParorendeForBruker = async (kontakt: Kontaktinfo) =>
  (
    await axios.post<Kontaktinfo, AxiosResponse<unknown>>(
      `${NOM_PAARORENDE_BASE_PATH}/api/kontaktinfo`,
      kontakt,
    )
  ).data;

export const oppdaterParorendeForBruker = async (kontakt: Kontaktinfo) =>
  (
    await axios.put<Kontaktinfo, AxiosResponse<unknown>>(
      `${NOM_PAARORENDE_BASE_PATH}/api/kontaktinfo`,
      kontakt,
    )
  ).data;

export const deleteParorendeForBruker = async (kontaktId: string) =>
  (
    await axios.delete<string, AxiosResponse<unknown>>(
      `${NOM_PAARORENDE_BASE_PATH}/api/kontaktinfo/${kontaktId}`,
    )
  ).data;

type UnleashResponse = {
  isUnleashEnabled: boolean;
};

export const getUnleashEnabled = async (unleashToggleName: string) =>
  (
    await axios.get<string, AxiosResponse<UnleashResponse>>(
      `${NOM_UNLEASH_BASE_PATH}/${unleashToggleName}`,
    )
  ).data;

async function hopeForKafkaStateToUpdate() {
  await new Promise((r) => setTimeout(r, 1000));
}
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

export type Kontaktinfo = {
  kontaktId: string;
  fornavn: string;
  etternavn: string;
  relasjon: string;
  telefon: string;
  prioritet: number;
  gateadresse?: string;
  postnr?: string;
  poststed?: string;
  land?: string;
};
