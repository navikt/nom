import { Alert, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getOrgtilknytningStateOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { NavidentNameLookup } from "~/components/NavidentNameLookup.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";

export const Route = createFileRoute("/state/orgtilknytning")({
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgtilknytningPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function OrgtilknytningPage() {
  const search = Route.useSearch();
  const orgtilknytninger = useSuspenseQuery({
    ...getOrgtilknytningStateOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const { data, sort, handleSort } = useTableSort(orgtilknytninger, "navident");

  return (
    <Table
      onSortChange={(sortKey) => handleSort(sortKey)}
      size="small"
      sort={sort}
      zebraStripes
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Navident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhetId" sortable>
            Orgenhet ID
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="erDagligOppfolging" sortable>
            Er daglig oppfølging
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom" sortable>
            Gyldig fra
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom" sortable>
            Gyldig til
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgtilknytningType" sortable>
            Orgtilknytning Type
          </Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {data.map((orgtilknytning) => {
          return (
            <Table.Row key={orgtilknytning.fid}>
              <Table.HeaderCell scope="row">
                <Link
                  search={{ filterText: orgtilknytning.navident }}
                  to="/state/ressurs"
                >
                  <NavidentNameLookup navident={orgtilknytning.navident} />
                </Link>
              </Table.HeaderCell>
              <Table.DataCell>
                <Link
                  search={{ filterText: orgtilknytning.orgEnhetId }}
                  to="/state/orgenhet"
                >
                  {orgtilknytning.orgEnhetId}
                </Link>
              </Table.DataCell>
              <Table.DataCell>
                {orgtilknytning.erDagligOppfolging.toString()}
              </Table.DataCell>
              <Table.DataCell>{orgtilknytning.gyldigFom}</Table.DataCell>
              <Table.DataCell>{orgtilknytning.gyldigTom}</Table.DataCell>
              <Table.DataCell>
                {orgtilknytning.orgtilknytningType}
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
