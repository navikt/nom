import { Table } from "@navikt/ds-react";
import { Link as TanStackLink } from "@tanstack/react-router";

import { NomLink } from "~/components/NomLink.tsx";
import type { RessursState } from "~/types/nom-admin/nom-admin-models.ts";
import { formatVisningsnavn } from "~/utils.ts";

export function RessursStateTabell({
  sortedData,
}: {
  sortedData: RessursState[];
}) {
  return (
    <>
      <Table.Header>
        <Table.ColumnHeader scope="col">Navident</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">Visningnavn</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">Sektor</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">Start dato</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">Slutt dato</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">Events</Table.ColumnHeader>
      </Table.Header>
      <Table.Body>
        {sortedData.map((ressurs) => (
          <Table.Row key={ressurs.navident}>
            <Table.DataCell>
              <NomLink path={`ressurs/${ressurs.navident}`}>
                {ressurs.navident}
              </NomLink>
            </Table.DataCell>
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
                Se events
              </TanStackLink>
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </>
  );
}
