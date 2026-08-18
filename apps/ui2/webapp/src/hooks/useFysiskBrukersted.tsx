import { useQuery } from "@tanstack/react-query";

import { getFysiskeBrukersteder } from "~/api/react-query-endpoints";

export function useFysiskBrukersted(fysiskBrukerStedNr?: string) {
  return useQuery({
    ...getFysiskeBrukersteder,
    select: (fysiskeBrukersteder) =>
      fysiskeBrukersteder.find(({ nr }) => fysiskBrukerStedNr === nr),
    enabled: !!fysiskBrukerStedNr,
  }).data;
}
