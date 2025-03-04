import { Alert } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getRessursEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { KafkaEventTable } from "~/components/KafkaEventTable.tsx";

export const Route = createFileRoute("/sinks/ressurs")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getRessursEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <RessursHistoryPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function RessursHistoryPage() {
  const search = Route.useSearch();

  const ressursEventsQuery = useSuspenseQuery({
    ...getRessursEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const ressursEvents = ressursEventsQuery.data ?? [];

  return (
    <KafkaEventTable
      kafkaEvents={ressursEvents}
      linkPrefix={"/state/ressurs"}
    />
  );
}
