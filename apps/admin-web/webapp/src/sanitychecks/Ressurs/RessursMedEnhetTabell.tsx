import { Table } from "@navikt/ds-react";
import { Link as TanStackLink } from "@tanstack/react-router";

import { NomLink } from "~/components/NomLink.tsx";
import type { RessursMedEnhet } from "~/types/nom-admin/nom-admin-models.ts";

export function RessursMedEnhetTabell({
  sortedData,
}: {
  sortedData: RessursMedEnhet[];
}) {
  return (
    <>
      <Table.Header>
        <Table.ColumnHeader scope="col" sortKey="navident" sortable>
          Navident
        </Table.ColumnHeader>
        <Table.ColumnHeader scope="col" sortKey="ressursFid" sortable>
          Ressurs FID
        </Table.ColumnHeader>
        <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
          Sektor
        </Table.ColumnHeader>
        <Table.ColumnHeader scope="col" sortKey="orgEnhetId" sortable>
          OrgEnhetId
        </Table.ColumnHeader>
        <Table.ColumnHeader scope="col" sortKey="orgEnhetNavn" sortable>
          OrgEnhetNavn
        </Table.ColumnHeader>
        <Table.ColumnHeader scope="col" sortKey="Unik Agresso ID" sortable>
          Unik Agresso ID
        </Table.ColumnHeader>
      </Table.Header>
      <Table.Body>
        {sortedData.map((ressurs) => (
          <Table.Row key={null} shadeOnHover={false}>
            <Table.DataCell>
              <NomLink path={`ressurs/${ressurs.navident}`}>
                {ressurs.navident}
              </NomLink>
            </Table.DataCell>
            <Table.DataCell>
              <TanStackLink
                search={{ filterText: ressurs.ressursFid, fromOffset: 0 }}
                to="/sinks/ressurs"
              >
                {ressurs.ressursFid}
              </TanStackLink>
            </Table.DataCell>
            <Table.DataCell>{ressurs.sektor}</Table.DataCell>
            <Table.DataCell>
              <NomLink path={`org/${ressurs.orgEnhetId}`}>
                {ressurs.orgEnhetId}
              </NomLink>
            </Table.DataCell>
            <Table.DataCell>{ressurs.orgEnhetNavn}</Table.DataCell>
            <Table.DataCell>{ressurs.uniqueAgressoId}</Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </>
  );
}
