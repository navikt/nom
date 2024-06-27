import { Alert, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getOrgenhetEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { EventDateRange } from "~/components/EventDateRange.tsx";
import { EventOrigination } from "~/components/EventOrigination.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { formatDisplayDatetime } from "~/utils.ts";

export const Route = createFileRoute("/sinks/orgenhet")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getOrgenhetEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
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
    <div>
      <EventDateRange events={orgEnhetEvents} />
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Fid</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Endring</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Dato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Opprinnelse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {orgEnhetEvents.map((orgEnhetEvent, index) => {
            return (
              <Table.Row key={index}>
                <Table.HeaderCell scope="row">
                  <Link
                    search={{ filterText: orgEnhetEvent.fid }}
                    to="/state/orgenhet"
                  >
                    {orgEnhetEvent.fid}
                  </Link>
                </Table.HeaderCell>
                <Table.DataCell>
                  <JsonModal data={orgEnhetEvent} text={orgEnhetEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(
                    orgEnhetEvent.metadata?.createdTimestamp,
                  )}
                </Table.DataCell>
                <Table.DataCell>
                  <EventOrigination metadata={orgEnhetEvent.metadata} />
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
