import { Alert } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import {getOrgenhetLederEventsOptions, getOrgenhetLederStatesOptions, searchSchema} from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import {KafkaEventTableForOrgenhetLeder} from "~/components/KafkaEventTableForOrgenhetLeder.tsx";

export const Route = createFileRoute("/sinks/orgenhet-leder")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getOrgenhetLederEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => {
    return (
      <>
        <GenericSearchBar route={Route} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgenhetLederPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function OrgenhetLederPage() {
  const search = Route.useSearch();

  const orgEnhetLederEventsQuery = useSuspenseQuery({
    ...getOrgenhetLederEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const orgEnhetLederEvents = orgEnhetLederEventsQuery.data;

  const orgenhetsLeaderStatesQuery = useSuspenseQuery({
    ...getOrgenhetLederStatesOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const orgenhetsLeaderStates = orgenhetsLeaderStatesQuery.data;

  return (
    <KafkaEventTableForOrgenhetLeder
      kafkaEvents={orgEnhetLederEvents}
      linkPrefix={"/state/orgenhet-leder"}
      orgenhetsLeaderStates={orgenhetsLeaderStates}
    />
  );
}
