import { useQuery } from "@tanstack/react-query";

import { getAvvisteSfOutboxRader } from "~/api/queries.ts";
import { SfOutboxTabell } from "~/sanitychecks/SfIntegrasjon/SfOutboxTabell.tsx";
import type { SfOutboxResponse } from "~/types/nom-sf-integrasjon/nom-sf-integrasjon-models.ts";

export function SfOutboxAvvist() {
  const {
    data = [],
    isLoading,
    isError,
  } = useQuery<SfOutboxResponse[], Error>({
    queryKey: ["API", "SF_OUTBOX_AVVIST"],
    queryFn: async (): Promise<SfOutboxResponse[]> => getAvvisteSfOutboxRader(),
  });

  return (
    <SfOutboxTabell
      beskrivelse="SF avviste raden eller svarte tomt, eller nom-sf-integrasjon kunne ikke lese orgenheten. Senderen prøver ikke disse radene på nytt."
      isError={isError}
      isLoading={isLoading}
      rader={data}
    />
  );
}
