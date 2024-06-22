import { Alert, type SortState, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import sortBy from "lodash/sortBy";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getOrgtilknytningStateOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { NavidentNameLookup } from "~/components/NavidentNameLookup.tsx";

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

  const [sort, setSort] = useState<SortState>({
    orderBy: "navn",
    direction: "ascending",
  });

  const handleSort = (sortKey: string | undefined) => {
    if (sortKey) {
      setSort({
        orderBy: sortKey,
        direction:
          sort && sortKey === sort.orderBy && sort.direction === "ascending"
            ? "descending"
            : "ascending",
      });
    } else {
      setSort({ orderBy: "id", direction: "none" });
    }
  };

  const sortedOrgtilknytninger = sort
    ? sortOrgtilknytninger({ orgtilknytninger, sort })
    : orgtilknytninger;

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
        {sortedOrgtilknytninger.map((orgtilknytning) => {
          return (
            <Table.Row key={orgtilknytning.fid}>
              <Table.HeaderCell scope="row">
                <NavidentNameLookup navident={orgtilknytning.navident} />
              </Table.HeaderCell>
              <Table.DataCell>{orgtilknytning.orgEnhetId}</Table.DataCell>
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

function sortOrgtilknytninger<T>({
  orgtilknytninger,
  sort,
}: {
  orgtilknytninger: T[];
  sort: SortState;
}) {
  const { orderBy, direction } = sort;

  const sortedMembersAscending = sortBy(orgtilknytninger, orderBy);
  const reversed = direction === "descending";

  return reversed ? sortedMembersAscending.reverse() : sortedMembersAscending;
}
