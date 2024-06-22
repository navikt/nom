import { Alert, Link, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link as TanStackLink } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getOrgenhetStatesOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";

export const Route = createFileRoute("/state/orgenhet")({
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgenhetPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function OrgenhetPage() {
  const search = Route.useSearch();

  const orgenheter = useSuspenseQuery({
    ...getOrgenhetStatesOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const { data, sort, handleSort } = useTableSort(orgenheter, "id");

  return (
    <Table
      onSortChange={(sortKey) => handleSort(sortKey)}
      size="small"
      sort={sort}
      zebraStripes
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col" sortKey="id" sortable>
            Id
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhetsType" sortable>
            OrgEnhetsType
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navn" sortable>
            Navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Gyldig fra</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Gyldig til</Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="agressoId" sortable>
            agressoId
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgNiv" sortable>
            Orgniv
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="agressoId" sortable>
            remedyEnhetId
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomNivaa" sortable>
            nomNivaa
          </Table.ColumnHeader>
          <Table.ColumnHeader
            scope="col"
            sortKey="agressoOrgenhetType"
            sortable
          >
            agressoOrgenhetType
          </Table.ColumnHeader>
          <Table.HeaderCell />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {data.map((orgEnhet) => {
          return (
            <Table.Row key={orgEnhet.fid}>
              <Table.HeaderCell scope="row">
                <Link
                  href={`http://nom.nav.no/org/${orgEnhet.id}`}
                  target="_blank"
                >
                  {orgEnhet.id}
                </Link>
              </Table.HeaderCell>
              <Table.DataCell />
              <Table.DataCell>{orgEnhet.navn}</Table.DataCell>
              <Table.DataCell>{orgEnhet.gyldigFom}</Table.DataCell>
              <Table.DataCell>{orgEnhet.gyldigTom}</Table.DataCell>
              <Table.DataCell>{orgEnhet.agressoId}</Table.DataCell>
              <Table.DataCell>{orgEnhet.agressoOrgNivaa}</Table.DataCell>
              <Table.DataCell>{orgEnhet.agressoId}</Table.DataCell>
              <Table.DataCell>{orgEnhet.nomNivaa}</Table.DataCell>
              <Table.DataCell>{orgEnhet.agressoOrgenhetType}</Table.DataCell>
              <Table.DataCell>
                <TanStackLink
                  search={{ filterText: orgEnhet.fid, fromOffset: 0 }}
                  to="/sinks/orgenhet"
                >
                  Se Events
                </TanStackLink>
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
