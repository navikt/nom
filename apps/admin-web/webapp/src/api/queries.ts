import { queryOptions } from "@tanstack/react-query";
import ky from "ky";
import queryString from "query-string";
import { z } from "zod";

import type {
  DelegertAuthorizationFelles,
  KafkaEvent,
  OrganiseringState,
} from "~/types.ts";
import type {
  DuplikatOrgtilknytning,
  EnheterOgOrganisering,
  HendelseDTO,
  ImportedRemedyUser,
  MasteredRessursDTO,
  OrgEnhetOrganisering,
  OrgEnhetStartOgSluttdato,
  OrgEnhetState,
  OrgEnhetStateDTO,
  OrgtilknytningMedOrgEnhet,
  OverlappendeOrgtilknytning,
  RemedyCompareResult,
  RessursMedEnhet,
  RessursMedFlereDOL,
  RessursOrgtilknytningKoblingInfo,
  RessursOrgtilknytningUlikSluttdato,
  RessursStateKafkaDTO,
  RobotRessurs,
  SessionInfoResponse,
} from "~/types/nom-admin/nom-admin-models.ts";
import type {
  OrgtilknytningState,
  Ressurs,
  RessursState,
} from "~/types/nom-api/nom-api-models.ts";
import type { ErrorState } from "~/types/nom-remedy-integrasjon/nom-remedy-integrasjon-models.ts";

const API_BASE_URL = "/frackend/admin-api";
const NOM_API_BASE_URL = "/frackend/api";
export const NOM_REMEDY_INTEGRASJON_BASE_URL = "/frackend/remedy";

export const searchSchema = z.object({
  filterText: z.string().optional(),

  fromOffset: z.number().optional().catch(0),
});

export const searchSchemaWithNavident = z.object({
  navident: z.string().optional(),

  fromOffset: z.number().optional().catch(0),
});

export type searchType = z.infer<typeof searchSchema>;

export const CACHE_FOR = {
  aMinute: 1000 * 60,
  anHour: 1000 * 60 * 60,
  aDay: 1000 * 60 * 60 * 24,
};

export function getRessursEventsOptions(search: searchType) {
  return queryOptions({
    queryKey: ["EVENTS", "RESSURS", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/sinks/ressursEvents?${queryString.stringify(search, { skipNull: true })}`,
          { timeout: 50_000 }, // 50 seconds
        )
      ).json<KafkaEvent[]>(),
  });
}

export function getTilgangEventsOptions(search: searchType) {
  return queryOptions({
    queryKey: ["EVENTS", "TILGANG", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/sinks/tilgangEvents?${queryString.stringify(search, { skipNull: true })}`,
        )
      ).json<KafkaEvent<DelegertAuthorizationFelles>[]>(),
  });
}

export function getOrgenhetEventsOptions(search: searchType) {
  return queryOptions({
    queryKey: ["EVENTS", "ORGENHET", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/sinks/orgenhetEvents?${queryString.stringify(search, { skipNull: true })}`,
        )
      ).json<KafkaEvent[]>(),
  });
}

export function getOrgtilknytningEventsOptions(search: searchType) {
  return queryOptions({
    queryKey: ["EVENTS", "ORGTILKNYTNING", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/sinks/orgtilknytningEvents?${queryString.stringify(search, { skipNull: true })}`,
          { timeout: 120_000 }, // 50 seconds
        )
      ).json<KafkaEvent[]>(),
  });
}

export function getNomMasteredStateOptions(
  navident: string | undefined,
  offset: number | undefined,
) {
  return queryOptions({
    queryKey: ["STATE", "NOM-MASTERED-STATE", navident, offset],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/states/nom-mastered-state?${queryString.stringify({ navident, offset }, { skipNull: true })}`,
        )
      ).json<MasteredRessursDTO[]>(),
  });
}

export function getRessursStatesOptions(search: searchType) {
  return queryOptions({
    queryKey: ["STATE", "RESSURS", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/states/ressurs?${queryString.stringify(search, { skipNull: true })}`,
        )
      ).json<RessursState[]>(),
  });
}

export function getOrgenhetStatesOptions(search: searchType) {
  return queryOptions({
    queryKey: ["STATE", "ORGENHET", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/states/orgenhet?${queryString.stringify(search, { skipNull: true })}`,
        )
      ).json<OrgEnhetState[]>(),
  });
}

export function getOrgtilknytningStateOptions(search: searchType) {
  return queryOptions({
    queryKey: ["STATE", "ORGTILKNYTNING", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/states/orgtilknytning?${queryString.stringify(search, { skipNull: true })}`,
        )
      ).json<OrgtilknytningState[]>(),
  });
}

export function getOrganiseringStateOptions(search: searchType) {
  return queryOptions({
    queryKey: ["STATE", "ORGANISERING", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/states/organisering?${queryString.stringify(search, { skipNull: true })}`,
        )
      ).json<OrganiseringState[]>(),
  });
}

export function getApiRessursEventsOptions(search: searchType) {
  return queryOptions({
    queryKey: ["API", "RESSURS", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/sinks/apiRessursEvents?${queryString.stringify(search, { skipNull: true })}`,
        )
      ).json<RessursStateKafkaDTO[]>(),
  });
}

export function getApiOrgenhetEventsOptions(search: searchType) {
  return queryOptions({
    queryKey: ["API", "ORGENHET", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/sinks/apiOrgenhetEvents?${queryString.stringify(search, { skipNull: true })}`,
        )
      ).json<OrgEnhetStateDTO[]>(),
  });
}

export function getApiHendelserEventsOptions(search: searchType) {
  return queryOptions({
    queryKey: ["API", "HENDELSER", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/sinks/apiHendelserEvents?${queryString.stringify(search, { skipNull: true })}`,
        )
      ).json<HendelseDTO[]>(),
  });
}

export function getRessurs(navident: string | undefined) {
  return queryOptions({
    queryKey: ["API", "RESSURS", navident],
    queryFn: async () =>
      (
        await ky.get(`${NOM_API_BASE_URL}/app/ressurser/byNavident/${navident}`)
      ).json<Ressurs>(),
  });
}

export function getRessurser(navidenter: string[]) {
  return queryOptions({
    queryKey: ["API", "RESSURSER", navidenter],
    queryFn: async () =>
      await ky
        .post(`${NOM_API_BASE_URL}/app/ressurser/byListNavident`, {
          json: navidenter,
        })
        .json<Ressurs[]>(),
  });
}

export function getOrgtilknytninger(navident: string | undefined) {
  return queryOptions({
    queryKey: ["API", "ORGTILKNYTNING", navident],
    queryFn: async () =>
      (
        await ky.get(
          `${NOM_API_BASE_URL}/app/orgtilknytninger?navident=${navident}`,
        )
      ).json<OrgtilknytningState[]>(),
  });
}

export function getOrgtilknytningerForListOfNavidents(navidenter: string[]) {
  return queryOptions({
    queryKey: ["API", "ORGTILKNYTNING", navidenter],
    queryFn: async () => {
      const response = await ky
        .post(`${NOM_API_BASE_URL}/app/orgtilknytninger`, {
          json: navidenter,
        })
        .json<Record<string, OrgtilknytningState[]>>();

      return new Map(Object.entries(response));
    },
  });
}

export function getAlleEnheter() {
  return queryOptions({
    queryKey: ["API", "ORGENHET"],
    queryFn: async () =>
      (
        await ky.get(`${API_BASE_URL}/SanityCheck/orgenhet/enheter`)
      ).json<OrgEnhetStartOgSluttdato>(),
  });
}

export function enheterOgOrganisering() {
  return queryOptions({
    queryKey: ["API", "ORGENHETOGORGANISERING"],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/SanityCheck/orgenhet/enheterOgOrganisering`,
        )
      ).json<EnheterOgOrganisering>(),
  });
}

export function getEnheterUtenOverenheter() {
  return queryOptions({
    queryKey: ["API", "ORGENHETUTENOVERENHET"],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/SanityCheck/orgenhet/listEnheterUtenOverEnheter`,
        )
      ).json<OrgEnhetOrganisering[]>(),
  });
}

export function getBunEnheterUtenEksakt1ArbeidsomradeIHierarkiet() {
  return queryOptions({
    queryKey: ["API", "ORGENHETUTENOVERENHET"],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/SanityCheck/orgenhet/bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier`,
        )
      ).json<OrgEnhetOrganisering[]>(),
  });
}

export function getEnheterUtenOverenheterMedUnderEnheter() {
  return queryOptions({
    queryKey: ["API", "ORGENHETUTENOVERENHETMEDUNDERENHETER"],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/SanityCheck/orgenhet/listEnheterUtenOverEnheterMedUnderEnheter`,
        )
      ).json<OrgEnhetOrganisering[]>(),
  });
}

export async function getRessurserSanityCheck(): Promise<RessursOrgtilknytningKoblingInfo> {
  return await ky
    .get(`${API_BASE_URL}/SanityCheck/ressurs/ressurser`, {
      timeout: 120_000, // 120 sekunder
    })
    .json<RessursOrgtilknytningKoblingInfo>();
}

export async function getRessursMedAktivOrgTilknytningHvorEnhetErInaktiv(
  inkluderEksterne: boolean,
  inkluderHelfo: boolean,
): Promise<RessursMedEnhet[]> {
  return await ky
    .get(
      `${API_BASE_URL}/SanityCheck/ressurs/listRessurserTilIkkeAktivEnhet?inkluderEksterne=${inkluderEksterne}&inkluderHelfo=${inkluderHelfo}`,
      {
        timeout: 120_000, // 120 seconds
      },
    )
    .json<RessursMedEnhet[]>();
}

export async function getRessursMedAktivOrgTilknytningHvorEnhetErInaktivMedUnderenhet(): Promise<
  RessursMedEnhet[]
> {
  return await ky
    .get(
      `${API_BASE_URL}/SanityCheck/ressurs/listTilkoblingerUtoverRessursGyldighetPeriode`,
      {
        timeout: 120_000, // 120 seconds
      },
    )
    .json<RessursMedEnhet[]>();
}

export async function getListAvDuplikateEnheter(): Promise<OrgEnhetState[]> {
  return await ky
    .get(`${API_BASE_URL}/SanityCheck/orgenhet/listDuplikateEnheter`, {
      timeout: 120_000, // 120 seconds
    })
    .json<OrgEnhetState[]>();
}

export async function getListAvOTMedGyldighetUtoverEnhetensGyldighet(): Promise<
  OrgtilknytningMedOrgEnhet[]
> {
  return await ky
    .get(
      `${API_BASE_URL}/SanityCheck/orgtilknytning/listTilkoblingerUtoverOrgEnhetGyldighetPeriode`,
      {
        timeout: 120_000, // 120 seconds
      },
    )
    .json<OrgtilknytningMedOrgEnhet[]>();
}

export function getRessursMedBlankMellomNavn() {
  return queryOptions({
    queryKey: ["API", "RESSURSBLANKMELLOMNAVN"],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/SanityCheck/ressurs/ressursMedBlankMellomNavn`,
        )
      ).json<RessursState[]>(),
  });
}

export async function getDuplikateRessurserPaaNavident(): Promise<
  RessursState[]
> {
  return await ky
    .get(`${API_BASE_URL}/SanityCheck/ressurs/duplikateRessurserPaaNavident`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RessursState[]>();
}

export function getOrgtilknytningMedNegativPeriode() {
  return queryOptions({
    queryKey: ["API", "ORGTILKNYTNINGMEDNEGATIVPERIODE"],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/SanityCheck/orgtilknytning/orgtilknytningNegativPeriode`,
        )
      ).json<OrgtilknytningState[]>(),
  });
}

export function getOrgtilknytningMedFlereAktivDOL() {
  return queryOptions({
    queryKey: ["API", "ORGTILKNYTNINGMEDFLEREAKTIVDOL"],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/SanityCheck/orgtilknytning/orgtilknytningMedFlereAktivDagligOppfolging`,
        )
      ).json<OrgtilknytningState[]>(),
  });
}

export function getOrgtilknytningMedUgyldigSektorTilknytningKombinasjon() {
  return queryOptions({
    queryKey: ["API", "UGYLDIGSEKTORTILKNYTNINGTYPE"],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/SanityCheck/orgtilknytning/orgtilknytningStatesMedUgyldigeSektorTilknytninger`,
        )
      ).json<OrgtilknytningState[]>(),
  });
}

export function getOrgtilknytningMedTilknytningtypeUtenSektor() {
  return queryOptions({
    queryKey: ["API", "OTMEDTILKNYTNINGTYPEUTENSEKTOR"],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/SanityCheck/orgtilknytning/orgtilknytningStatesMedTilknytningtypeUtenSektor`,
        )
      ).json<OrgtilknytningState[]>(),
  });
}

export async function getDuplikateOrgtilknytninger(): Promise<
  DuplikatOrgtilknytning[]
> {
  return await ky
    .get(
      `${API_BASE_URL}/SanityCheck/orgtilknytning/duplikateOrgtilknytninger`,
      {
        timeout: 120_000, // 120 seconds
      },
    )
    .json<DuplikatOrgtilknytning[]>();
}

export async function getOverlappendeOrgtilknytninger(): Promise<
  OverlappendeOrgtilknytning[]
> {
  return await ky
    .get(
      `${API_BASE_URL}/SanityCheck/orgtilknytning/orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture`,
      {
        timeout: 120_000, // 120 seconds
      },
    )
    .json<OverlappendeOrgtilknytning[]>();
}

export async function getRessursOrgtilknytningUlikSluttdato(): Promise<
  RessursOrgtilknytningUlikSluttdato[]
> {
  return await ky
    .get(
      `${API_BASE_URL}/SanityCheck/ressurs/ressursSluttdatoUlikOrgTilknytningSluttdato`,
      {
        timeout: 120_000, // 120 seconds
      },
    )
    .json<RessursOrgtilknytningUlikSluttdato[]>();
}

export async function getRessursMedFlerEnnEnDOL(): Promise<
  RessursMedFlereDOL[]
> {
  return await ky
    .get(`${API_BASE_URL}/SanityCheck/ressurs/ressursMedFlereEnnEnDOL`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RessursMedFlereDOL[]>();
}

export function getImportedRemedyUser(navident: string) {
  return queryOptions({
    queryKey: ["API", "REMEDYUSER", navident],
    queryFn: async () =>
      (
        await ky.get(`${API_BASE_URL}/RemedyCompare/remedyUser`, {
          searchParams: {
            navident: navident,
          },
        })
      ).json<ImportedRemedyUser>(),
  });
}

export async function getRessursNotInRemedy(): Promise<RemedyCompareResult[]> {
  return await ky
    .get(`${API_BASE_URL}/RemedyCompare/ressursNotInRemedy`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RemedyCompareResult[]>();
}

export async function getRemedyUsersNotInNom(): Promise<RemedyCompareResult[]> {
  return await ky
    .get(`${API_BASE_URL}/RemedyCompare/remedyUsersNotInNom`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RemedyCompareResult[]>();
}

export async function getRemedyCompareDiffPaaNavn(): Promise<
  RemedyCompareResult[]
> {
  return await ky
    .get(`${API_BASE_URL}/RemedyCompare/diffPaaNavn`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RemedyCompareResult[]>();
}

export async function getRemedyCompareDiffPaaSektor(): Promise<
  RemedyCompareResult[]
> {
  return await ky
    .get(`${API_BASE_URL}/RemedyCompare/diffPaaSektor`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RemedyCompareResult[]>();
}

export async function getRemedyCompareDiffPaaSluttdato(): Promise<
  RemedyCompareResult[]
> {
  return await ky
    .get(`${API_BASE_URL}/RemedyCompare/diffPaaSluttdato`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RemedyCompareResult[]>();
}

export async function getRemedyCompareDiffPaaUtlopsdato(): Promise<
  RemedyCompareResult[]
> {
  return await ky
    .get(`${API_BASE_URL}/RemedyCompare/diffPaaUtlopsdato`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RemedyCompareResult[]>();
}

export async function getRemedyCompareDiffPaaAktiv(): Promise<
  RemedyCompareResult[]
> {
  return await ky
    .get(`${API_BASE_URL}/RemedyCompare/diffPaaAktiv`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RemedyCompareResult[]>();
}

export function getSessionInfo() {
  return queryOptions({
    queryKey: ["SESSIONINFO"],
    queryFn: async () =>
      (await ky.get(`${API_BASE_URL}/sessioninfo`)).json<SessionInfoResponse>(),
  });
}

export async function getRemedyCompareDiffPaaRemedyEnhet(): Promise<
  RemedyCompareResult[]
> {
  return await ky
    .get(`${API_BASE_URL}/RemedyCompare/diffPaaRemedyEnhet`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RemedyCompareResult[]>();
}

export async function getRessurserUtenOrgtilknytning(): Promise<
  RessursState[]
> {
  return await ky
    .get(`${API_BASE_URL}/SanityCheck/ressurs/ressursUtenOrgtilknytning`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RessursState[]>();
}

export async function getRobotRessurser(): Promise<RobotRessurs[]> {
  return await ky
    .get(`${API_BASE_URL}/SanityCheck/ressurs/robotRessurser`, {
      timeout: 120_000, // 120 seconds
    })
    .json<RobotRessurs[]>();
}

export async function getAllErrorStates(): Promise<ErrorState[]> {
  return await ky
    .get(`${NOM_REMEDY_INTEGRASJON_BASE_URL}/admin/getAllErrorState`, {
      timeout: 60_000,
    })
    .json<ErrorState[]>();
}

export function deleteAllErrorStates() {
  return ky.delete(
    `${NOM_REMEDY_INTEGRASJON_BASE_URL}/admin/deleteAllErrorStates`,
  );
}

export function deleteErrorStateById(fid: string) {
  return ky.delete(
    `${NOM_REMEDY_INTEGRASJON_BASE_URL}/admin/deleteErrorState/${fid}`,
  );
}
