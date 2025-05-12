import { Alert } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getOrgenhetEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { KafkaEventTable } from "~/components/KafkaEventTable.tsx";

export const Route = createFileRoute("/sinks/orgenhet")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getOrgenhetEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => {
    return (
      <>
        <GenericSearchBar route={Route} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgenhetHistoryPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function OrgenhetHistoryPage() {
  const search = Route.useSearch();

  const orgEnhetEventsQuery = useSuspenseQuery({
    ...getOrgenhetEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const orgEnhetEvents = orgEnhetEventsQuery.data;

  return (
    <KafkaEventTable
      kafkaEvents={orgEnhetEvents}
      linkPrefix={"/state/orgenhet"}
    />
  );
}
