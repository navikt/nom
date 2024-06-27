import { Alert, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getTilgangEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { EventDateRange } from "~/components/EventDateRange.tsx";
import { EventOrigination } from "~/components/EventOrigination.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { NavidentNameLookup } from "~/components/NavidentNameLookup.tsx";
import { formatDisplayDatetime } from "~/utils.ts";

export const Route = createFileRoute("/sinks/tilgang")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getTilgangEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <TilgangHistoryPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function TilgangHistoryPage() {
  const search = Route.useSearch();

  const tilgangEventsQuery = useSuspenseQuery({
    ...getTilgangEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const tilgangEvents = tilgangEventsQuery.data ?? [];

  return (
    <div>
      <EventDateRange events={tilgangEvents} />

      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Fra</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Til</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Endring</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Dato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Opprinnelse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {tilgangEvents.map((tilgangEvent, index) => {
            return (
              <Table.Row key={index}>
                <Table.DataCell>
                  <Link
                    search={{ filterText: tilgangEvent.data.av }}
                    to="/state/ressurs"
                  >
                    <NavidentNameLookup navident={tilgangEvent.data.av} />
                  </Link>
                </Table.DataCell>
                <Table.DataCell scope="row">
                  <Link
                    search={{ filterText: tilgangEvent.data.til }}
                    to="/state/ressurs"
                  >
                    <NavidentNameLookup navident={tilgangEvent.data.til} />
                  </Link>
                </Table.DataCell>
                <Table.DataCell>
                  <JsonModal data={tilgangEvent} text={tilgangEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(
                    tilgangEvent.metadata?.createdTimestamp,
                  )}
                </Table.DataCell>
                <Table.DataCell>
                  <EventOrigination metadata={tilgangEvent.metadata} />
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
