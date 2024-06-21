import { queryOptions } from "@tanstack/react-query";
import ky from "ky";

const API_BASE_URL = "/frackend/api";

export function getRessursOptions(navident: string) {
  return queryOptions({
    queryKey: ["RESSURS", navident],
    queryFn: async () =>
      (await ky.get(`${API_BASE_URL}/app/ressurser/byNavident/${navident}`))
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        .json<any>(),
  });
}
