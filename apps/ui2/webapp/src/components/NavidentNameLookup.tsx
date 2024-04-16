import { Tooltip } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React from "react";

import { CACHE_FOR, getRessurs } from "~/api/react-query-endpoints";
import { formatVisningsnavn } from "~/utils";

export function NavidentNameLookup({ navident }: { navident: string }) {
  const hentRessursQuery = useQuery({
    queryKey: getRessurs.queryKey(navident),
    queryFn: () => getRessurs.queryFn(navident),
    staleTime: CACHE_FOR.aDay,
    gcTime: CACHE_FOR.aDay,
    select: (result) => formatVisningsnavn(result.visningsnavn),
  });

  const name = hentRessursQuery.data;

  if (!name) {
    return <span>{navident}</span>;
  }

  return (
    <Tooltip content={navident}>
      <span aria-label={`${name} med navident ${navident}`}>{name}</span>
    </Tooltip>
  );
}
