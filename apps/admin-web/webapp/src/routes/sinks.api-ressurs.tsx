import { FilesIcon } from "@navikt/aksel-icons";
import { Alert, Button, HStack, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getApiRessursEventsOptions, searchSchema } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { EmbeddedJsonView } from "~/components/KafkaEventTable.tsx";
import { NomLink } from "~/components/NomLink.tsx";
import { copyToClipboard } from "~/components/quickdialog.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { formatDisplayDatetime, formatVisningsnavn } from "~/utils.ts";

export const Route = createFileRoute("/sinks/api-ressurs")({
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <ApiRessursPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function ApiRessursPage() {
  const search = Route.useSearch();

  const apiRessurser = useSuspenseQuery({
    ...getApiRessursEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const { data, sort, handleSort } = useTableSort(apiRessurser, "timestamp");

  return (
    <>
      <HStack justify={"end"}>
        <Button
          icon={<FilesIcon />}
          onClick={() => copyToClipboard(data)}
          size={"xsmall"}
          variant={"secondary"}
        >
          JSON[]
        </Button>
      </HStack>
      <Table
        onSortChange={(sortKey) => handleSort(sortKey)}
        size="small"
        sort={sort}
        zebraStripes
      >
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.ColumnHeader scope="col" sortKey="fid" sortable>
              Fid
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="timestamp" sortable>
              Timestamp
            </Table.ColumnHeader>
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
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {data.map((apiRessurs, index) => (
            <Table.ExpandableRow
              content={<EmbeddedJsonView jsonData={apiRessurs} />}
              key={index}
            >
              <Table.HeaderCell scope="row">
                <Link search={{ filterText: apiRessurs.key }}>
                  {apiRessurs.key}
                </Link>
              </Table.HeaderCell>
              <Table.DataCell>
                {formatDisplayDatetime(apiRessurs.timestamp)}
              </Table.DataCell>
              <Table.DataCell>
                <NomLink path={`ressurs/${apiRessurs.navident}`}>
                  {apiRessurs.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>
                <JsonModal
                  data={apiRessurs}
                  text={formatVisningsnavn(apiRessurs.visningsnavn)}
                />
              </Table.DataCell>
              <Table.DataCell>{apiRessurs.sektor}</Table.DataCell>
              <Table.DataCell>
                {formatDisplayDatetime(apiRessurs.startdato)}
              </Table.DataCell>
              <Table.DataCell>
                {formatDisplayDatetime(apiRessurs.sluttdato)}
              </Table.DataCell>
            </Table.ExpandableRow>
          ))}
        </Table.Body>
      </Table>
    </>
  );
}
