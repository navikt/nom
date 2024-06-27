import { Alert, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getRessursEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { EventDateRange } from "~/components/EventDateRange.tsx";
import { EventOrigination } from "~/components/EventOrigination.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { formatDisplayDatetime } from "~/utils.ts";

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
    <div>
      <EventDateRange events={ressursEvents} />
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
          {ressursEvents.map((ressursEvent, index) => {
            return (
              <Table.Row key={index}>
                <Table.HeaderCell scope="row">
                  <Link
                    search={{ filterText: ressursEvent.fid }}
                    to="/state/ressurs"
                  >
                    {ressursEvent.fid}
                  </Link>
                </Table.HeaderCell>
                <Table.DataCell>
                  <JsonModal data={ressursEvent} text={ressursEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(
                    ressursEvent.metadata.createdTimestamp,
                  )}
                </Table.DataCell>
                <Table.DataCell>
                  <EventOrigination metadata={ressursEvent.metadata} />
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
