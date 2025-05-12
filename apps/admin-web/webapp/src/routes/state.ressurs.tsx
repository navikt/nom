import { Alert, CopyButton, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link as TanStackLink } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getRessursStatesOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { formatVisningsnavn } from "~/utils.ts";

export const Route = createFileRoute("/state/ressurs")({
  validateSearch: searchSchema.parse,
  component: () => {
    return (
      <>
        <GenericSearchBar route={Route} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <RessursPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function RessursPage() {
  const search = Route.useSearch();

  const ressurser = useSuspenseQuery({
    ...getRessursStatesOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const { data, sort, handleSort } = useTableSort(ressurser, "navident");

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
          <Table.ColumnHeader scope="col" sortKey="visningsnavn" sortable>
            Visningsnavn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
            Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="startdato" sortable>
            Startdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
            Sluttdato
          </Table.ColumnHeader>
          <Table.HeaderCell />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {data.map((ressurs) => (
          <Table.Row key={ressurs.fid}>
            <Table.DataCell>
              <CopyButton copyText={ressurs.fid} text={ressurs.fid} />
            </Table.DataCell>
            <Table.HeaderCell scope="row">
              <NomLink path={`ressurs/${ressurs.navident}`}>
                {ressurs.navident}
              </NomLink>
            </Table.HeaderCell>
            <Table.DataCell>
              {formatVisningsnavn(ressurs.visningsnavn)}
            </Table.DataCell>
            <Table.DataCell>{ressurs.sektor}</Table.DataCell>
            <Table.DataCell>{ressurs.startdato}</Table.DataCell>
            <Table.DataCell>{ressurs.sluttdato}</Table.DataCell>
            <Table.DataCell>
              <TanStackLink
                search={{ filterText: ressurs.fid, fromOffset: 0 }}
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
