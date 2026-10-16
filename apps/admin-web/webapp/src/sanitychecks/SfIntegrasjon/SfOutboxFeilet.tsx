import { useQuery } from "@tanstack/react-query";

import { getFeiledeSfOutboxRader } from "~/api/queries.ts";
import { SfOutboxTabell } from "~/sanitychecks/SfIntegrasjon/SfOutboxTabell.tsx";
import type { SfOutboxResponse } from "~/types/nom-sf-integrasjon/nom-sf-integrasjon-models.ts";

export function SfOutboxFeilet() {
  const {
    data = [],
    isLoading,
    isError,
  } = useQuery<SfOutboxResponse[], Error>({
    queryKey: ["API", "SF_OUTBOX_FEILET"],
    queryFn: async (): Promise<SfOutboxResponse[]> => getFeiledeSfOutboxRader(),
  });

  return (
    <SfOutboxTabell
      beskrivelse="Sendingen feilet på grunn av nettverk, timeout, 5xx, 408, 429 eller en uventet feil. Senderen prøver disse radene på nytt ved neste kjøring, så rader som blir liggende feiler gjentatte ganger."
      isError={isError}
      isLoading={isLoading}
      rader={data}
    />
  );
}
