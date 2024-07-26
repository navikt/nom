import { Alert, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getApiHendelserEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { formatDisplayDatetime } from "~/utils.ts";

export const Route = createFileRoute("/sinks/api-hendelser")({
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <ApiHendelserPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function ApiHendelserPage() {
  const search = Route.useSearch();

  const apiHendelser = useSuspenseQuery({
    ...getApiHendelserEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const { data, sort, handleSort } = useTableSort(apiHendelser, "timestamp");

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
          <Table.ColumnHeader scope="col" sortKey="timestamp" sortable>
            Timestamp
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="entity" sortable>
            Type
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="hendelse" sortable>
            Hendelse
          </Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {data.map((apiHendelse, index) => (
          <Table.Row key={index}>
            <Table.HeaderCell scope="row">
              <Link search={{ filterText: apiHendelse.id }}>
                {apiHendelse.id}
              </Link>
            </Table.HeaderCell>
            <Table.DataCell>
              {formatDisplayDatetime(apiHendelse.timestamp)}
            </Table.DataCell>
            <Table.DataCell>{apiHendelse.entityType}</Table.DataCell>
            <Table.DataCell>{apiHendelse.hendelseType}</Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
