import { Alert, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getDeadLetterEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { formatDisplayDatetime } from "~/utils.ts";

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
    <>
      <Table>
        <Table.Header>
          <Table.ColumnHeader scope="col">Fid</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Created at</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Header</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Event</Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {deadLetterEvents.map((event, index) => {
            return (
              <Table.Row key={index}>
                <Table.HeaderCell scope="row">{event.fid}</Table.HeaderCell>
                <Table.DataCell>
                  {formatDisplayDatetime(event.timestamp)}
                </Table.DataCell>
                <Table.DataCell>
                  <JsonModal data={event.headers || {}} text="Header" />
                </Table.DataCell>
                <Table.DataCell>
                  <JsonModal data={event.event} text="Event" />
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </>
  );
}
