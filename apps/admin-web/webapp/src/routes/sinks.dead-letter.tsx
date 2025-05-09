import { Alert } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getDeadLetterEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { KafkaEventTable } from "~/components/KafkaEventTable.tsx";

export const Route = createFileRoute("/sinks/dead-letter")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getDeadLetterEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => {
    return (
      <>
        <GenericSearchBar route={Route} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <DeadLetterHistoryPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function DeadLetterHistoryPage() {
  const search = Route.useSearch();

  const deadLetterEventsQuery = useSuspenseQuery({
    ...getDeadLetterEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const deadLetterEvents = deadLetterEventsQuery.data;

  return (
    <KafkaEventTable
      kafkaEvents={deadLetterEvents}
      linkPrefix={"/state/dead-letter"}
    />
  );
}
