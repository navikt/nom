import { queryOptions } from "@tanstack/react-query";
import ky from "ky";
import queryString from "query-string";
import { z } from "zod";

import type {
  DelegertAuthorizationFelles,
  KafkaEvent,
  OrganiseringState,
  OrgEnhetState,
} from "~/types.ts";
import type {
  EnheterOgOrganisering,
  HendelseDTO,
  MasteredRessursDTO,
  OrgEnhetOrganisering,
  OrgEnhetStartOgSluttdato,
  OrgEnhetStateDTO,
  RessursOrgtilknytningInfo,
  RessursStateKafkaDTO,
} from "~/types/nom-admin/nom-admin-models.ts";
import type {
  OrgtilknytningState,
  Ressurs,
  RessursState,
} from "~/types/nom-api/nom-api-models.ts";
const API_BASE_URL = "/frackend/admin-api";
const NOM_API_BASE_URL = "/frackend/api";

export const searchSchema = z.object({
  filterText: z.string().optional(),
  // eslint-disable-next-line unicorn/prefer-top-level-await
  fromOffset: z.number().optional().catch(0),
});

export const searchSchemaWithNavident = z.object({
  navident: z.string().optional(),
  // eslint-disable-next-line unicorn/prefer-top-level-await
  fromOffset: z.number().optional().catch(0),
});

export type searchType = z.infer<typeof searchSchema>;

export type searchTypeWithNavident = z.infer<typeof searchSchemaWithNavident>;

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

export function getRessurserSanityCheck() {
  return queryOptions({
    queryKey: ["API", "RESSURSERSANITYCHECK"],
    queryFn: async () =>
      (
        await ky.get(`${API_BASE_URL}/SanityCheck/ressurs/ressurser`, {
          timeout: 120_000, // 120 sek
        })
      ).json<RessursOrgtilknytningInfo>(),
  });
}
