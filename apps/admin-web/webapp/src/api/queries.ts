import { queryOptions } from "@tanstack/react-query";
import ky from "ky";
import queryString from "query-string";
import { z } from "zod";

import type { KafkaEvent } from "~/types.ts";
const API_BASE_URL = "/frackend/admin-api";

export const searchSchema = z.object({
  filterText: z.string().optional(),
  // eslint-disable-next-line unicorn/prefer-top-level-await
  fromOffset: z.number().optional().catch(0),
});

type searchType = z.infer<typeof searchSchema>;

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
      ).json<KafkaEvent[]>(),
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
