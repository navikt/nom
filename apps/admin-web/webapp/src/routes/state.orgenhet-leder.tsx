import { Alert, CopyButton, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link as TanStackLink } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import {getOrgenhetLederStatesOptions, searchSchema} from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";

export const Route = createFileRoute("/state/orgenhet-leder")({
  validateSearch: searchSchema.parse,
  component: () => {
    return (
      <>
        <GenericSearchBar route={Route} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgenhetLederPagePage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function OrgenhetLederPagePage() {
  const search = Route.useSearch();

  const orgenhetsLeaders = useSuspenseQuery({
    ...getOrgenhetLederStatesOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const { data, sort, handleSort } = useTableSort(orgenhetsLeaders, "navident");

  return (
    <Table
      onSortChange={(sortKey) => handleSort(sortKey)}
      size="small"
      sort={sort}
      zebraStripes
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col">Fid</Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Navident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhetId" sortable>
            OrgEnhetId
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="startdato" sortable>
            Startdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
            Sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sistEndretAv" sortable>
            SistEndretAv
          </Table.ColumnHeader>
          <Table.HeaderCell />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {data.map((orgenhetleder) => (
          <Table.Row key={orgenhetleder.fid}>
            <Table.DataCell>
              <CopyButton copyText={orgenhetleder.fid} text={orgenhetleder.fid} />
            </Table.DataCell>
            <Table.HeaderCell scope="row">
              <NomLink path={`ressurs/${orgenhetleder.navident}`}>
                {orgenhetleder.navident}
              </NomLink>
            </Table.HeaderCell>
            <Table.DataCell>{orgenhetleder.orgEnhetId}</Table.DataCell>
            <Table.DataCell>{orgenhetleder.startdato}</Table.DataCell>
            <Table.DataCell>{orgenhetleder.sluttdato}</Table.DataCell>
            <Table.DataCell>{orgenhetleder.sistEndretAv}</Table.DataCell>
            <Table.DataCell>
              <TanStackLink
                search={{ filterText: orgenhetleder.fid, fromOffset: 0 }}
                to="/sinks/ressurs"
              >
                Se Events
              </TanStackLink>
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
