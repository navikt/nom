import { Alert, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getOrgtilknytningEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { EventDateRange } from "~/components/EventDateRange.tsx";
import { EventOrigination } from "~/components/EventOrigination.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { formatDisplayDatetime } from "~/utils.ts";

export const Route = createFileRoute("/sinks/orgtilknytning")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getOrgtilknytningEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgtilknytningHistoryPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function OrgtilknytningHistoryPage() {
  const search = Route.useSearch();

  const orgtilknytningEventsQuery = useSuspenseQuery({
    ...getOrgtilknytningEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const orgtilknytningEvents = orgtilknytningEventsQuery.data;

  return (
    <div>
      <EventDateRange events={orgtilknytningEvents} />
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
          {orgtilknytningEvents.map((orgtilknytningEvent, index) => {
            return (
              <Table.Row key={index}>
                <Table.HeaderCell scope="row">
                  <Link
                    search={{ filterText: orgtilknytningEvent.fid }}
                    to="/state/orgtilknytning"
                  >
                    {orgtilknytningEvent.fid}
                  </Link>
                </Table.HeaderCell>
                <Table.DataCell>
                  <JsonModal data={orgtilknytningEvent} text={orgtilknytningEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(
                    orgtilknytningEvent.metadata?.createdTimestamp,
                  )}
                </Table.DataCell>
                <Table.DataCell>
                  <EventOrigination metadata={orgtilknytningEvent.metadata} />
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
