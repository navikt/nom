import { Alert, BodyShort, Table } from "@navikt/ds-react";

import { EmbeddedJsonView } from "~/components/KafkaEventTable.tsx";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { SfOutboxResponse } from "~/types/nom-sf-integrasjon/nom-sf-integrasjon-models.ts";

export function SfOutboxTabell({
  beskrivelse,
  isError,
  isLoading,
  rader,
}: {
  beskrivelse: string;
  isError: boolean;
  isLoading: boolean;
  rader: SfOutboxResponse[];
}) {
  const { data, sort, handleSort } = useTableSort(rader, "sporingsId");

  if (isLoading) {
    return <LoadingTable />;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Kunne ikke hente outbox-radene fra nom-sf-integrasjon.
      </Alert>
    );
  }

  if (data.length === 0) {
    return <EmptyTable />;
  }

  return (
    <>
      <div style={{ paddingBottom: "20px" }}>
        <BodyShort spacing>{beskrivelse}</BodyShort>
        <b>Antall: {data.length}</b>
      </div>
      <Table
        onSortChange={(sortKey) => handleSort(sortKey)}
        size="small"
        sort={sort}
      >
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.ColumnHeader scope="col" sortKey="sporingsId" sortable>
              Sporings-ID
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="orgEnhetId" sortable>
              Orgenhet
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Endret av</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">
              Endringshendelser
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Feilmelding</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {data.map((rad) => (
            <Table.ExpandableRow
              content={<EmbeddedJsonView jsonData={rad.orgenhet} />}
              key={rad.sporingsId}
            >
              <Table.HeaderCell scope="row">{rad.sporingsId}</Table.HeaderCell>
              <Table.DataCell>
                <NomLink path={`org/${rad.orgEnhetId}`}>
                  {rad.orgEnhetId}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{rad.orgenhet.navn}</Table.DataCell>
              <Table.DataCell>{rad.endretAv?.join(", ")}</Table.DataCell>
              <Table.DataCell>
                {rad.endringshendelser?.join(", ")}
              </Table.DataCell>
              <Table.DataCell>{rad.feilmelding}</Table.DataCell>
            </Table.ExpandableRow>
          ))}
        </Table.Body>
      </Table>
    </>
  );
}
