import { Tooltip } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getOrgEnhetOptions } from "~/api/apiQueries.ts";
import { CACHE_FOR } from "~/api/queries.ts";

export function OrgEnhetLookup({ orgEnhet }: { orgEnhet: string }) {
  const hentOrgEnhetQuery = useQuery({
    ...getOrgEnhetOptions(orgEnhet),
    staleTime: CACHE_FOR.aDay,
    gcTime: CACHE_FOR.aDay,
    select: (result) => result.navn,
  });

  const name = hentOrgEnhetQuery.data;

  if (!name) {
    return <span>{orgEnhet}</span>;
  }

  return (
    <Tooltip content={orgEnhet}>
      <span aria-label={`${name} med orgEnhet ${orgEnhet}`}>{name}</span>
    </Tooltip>
  );
}
