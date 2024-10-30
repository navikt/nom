import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as TanStackLink } from "@tanstack/react-router";

import { getRessursMedBlankMellomNavn } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";

export function RessursMedBlankMellomnavn() {
  const ressursMedBlankMellomnavn = useQuery(getRessursMedBlankMellomNavn());

  return (
    <>
      <Table>
        <Table.Header>
          <Table.ColumnHeader scope="col">Navident</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Fornavn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Mellomnavn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Etternavn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Events</Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {ressursMedBlankMellomnavn.data?.map((ressurs) => (
            <Table.Row key={ressurs.navident}>
              <Table.DataCell>
                <NomLink path={`ressurs/${ressurs.navident}`}>
                  {ressurs.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{ressurs.visningsnavn.fornavn}</Table.DataCell>
              <Table.DataCell>{ressurs.visningsnavn.mellomnavn}</Table.DataCell>
              <Table.DataCell>{ressurs.visningsnavn.etternavn}</Table.DataCell>
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
      </Table>
    </>
  );
}
