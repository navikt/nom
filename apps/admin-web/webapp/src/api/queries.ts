/* eslint-disable @typescript-eslint/no-explicit-any */
import { queryOptions } from "@tanstack/react-query";
import ky from "ky";
import queryString from "query-string";
import { z } from "zod";
const API_BASE_URL = "/frackend/admin-api";

export const searchSchema = z.object({
  filterText: z.string().optional(),
  fromOffset: z.string().optional(),
});

type searchType = z.infer<typeof searchSchema>;

export function getRessursEventsOptions(search: searchType) {
  return queryOptions({
    queryKey: ["EVENTS", "RESSURS", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/sinks/ressursEvents?${queryString.stringify(search)}`,
        )
      ).json<any[]>(),
  });
}

export function getOrgenhetEventsOptions(search: searchType) {
  return queryOptions({
    queryKey: ["EVENTS", "ORGENHET", search],
    queryFn: async () =>
      (
        await ky.get(
          `${API_BASE_URL}/sinks/orgenhetEvents?${queryString.stringify(search)}`,
        )
      ).json<any[]>(),
  });
}
