import { Tooltip } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRessursOptions } from "~/api/apiQueries.ts";
import { CACHE_FOR } from "~/api/queries.ts";
import { formatVisningsnavn } from "~/utils";

export function NavidentNameLookup({ navident }: { navident: string }) {
  const hentRessursQuery = useQuery({
    ...getRessursOptions(navident),
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
