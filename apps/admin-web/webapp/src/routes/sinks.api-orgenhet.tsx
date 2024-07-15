import { Alert, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getApiOrgenhetEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { formatDisplayDatetime } from "~/utils.ts";

export const Route = createFileRoute("/sinks/api-orgenhet")({
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <ApiOrgenhetPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function ApiOrgenhetPage() {
  const search = Route.useSearch();

  const apiOrgenheter = useSuspenseQuery({
    ...getApiOrgenhetEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const { data, sort, handleSort } = useTableSort(apiOrgenheter, "timestamp");

  return (
    <Table
      onSortChange={(sortKey) => handleSort(sortKey)}
      size="small"
      sort={sort}
      zebraStripes
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col" sortKey="fid" sortable>
            Fid
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="timestamp" sortable>
            Timestamp
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="id" sortable>
            Orgenhet NOM ID
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navn" sortable>
            Navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom" sortable>
            Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom" sortable>
            Gyldig TOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomNivaa" sortable>
            NOM Nivå
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgenhetType" sortable>
            Orgenhet Type
          </Table.ColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {data.map((apiOrgenhet, index) => (
          <Table.Row key={index}>
            <Table.HeaderCell scope="row">
              <Link search={{ filterText: apiOrgenhet.key }}>
                {apiOrgenhet.key}
              </Link>
            </Table.HeaderCell>
            <Table.DataCell>
              {formatDisplayDatetime(apiOrgenhet.timestamp)}
            </Table.DataCell>
            <Table.DataCell>
              <NomLink path={`ressurs/${apiOrgenhet.id}`}>
                {apiOrgenhet.id}
              </NomLink>
            </Table.DataCell>
            <Table.DataCell>
              <JsonModal data={apiOrgenhet} text={apiOrgenhet.navn} />
            </Table.DataCell>
            <Table.DataCell>
              {formatDisplayDatetime(apiOrgenhet.gyldigFom)}
            </Table.DataCell>
            <Table.DataCell>
              {formatDisplayDatetime(apiOrgenhet.gyldigTom)}
            </Table.DataCell>
            <Table.DataCell>{apiOrgenhet.nomNivaa}</Table.DataCell>
            <Table.DataCell>{apiOrgenhet.orgEnhetsType}</Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
