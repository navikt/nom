import { Alert, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getOrganiseringStateOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";

export const Route = createFileRoute("/state/organisering")({
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrganiseringPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function OrganiseringPage() {
  const search = Route.useSearch();
  const organiseringer = useSuspenseQuery({
    ...getOrganiseringStateOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const { data, sort, handleSort } = useTableSort(organiseringer, "id");

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
            ID
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navn" sortable>
            Navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="idOver" sortable>
            ID Over
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navnOver" sortable>
            Navn over
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom" sortable>
            Gyldig fra
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom" sortable>
            Gyldig til
          </Table.ColumnHeader>
          <Table.HeaderCell />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {data.map((organisering) => {
          return (
            <Table.Row key={organisering.fid}>
              <Table.HeaderCell scope="row">
                <Link
                  search={{ filterText: organisering.id }}
                  to="/state/orgenhet"
                >
                  {organisering.id}
                </Link>
              </Table.HeaderCell>
              <Table.DataCell>{organisering.navn}</Table.DataCell>
              <Table.DataCell>
                <Link
                  search={{ filterText: organisering.idOver }}
                  to="/state/orgenhet"
                >
                  {organisering.idOver}
                </Link>
              </Table.DataCell>
              <Table.DataCell>{organisering.navnOver}</Table.DataCell>
              <Table.DataCell>{organisering.gyldigFom}</Table.DataCell>
              <Table.DataCell>{organisering.gyldigTom}</Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
