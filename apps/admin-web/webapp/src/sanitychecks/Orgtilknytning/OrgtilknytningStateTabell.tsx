import { Table } from "@navikt/ds-react";
import { Link as TanStackLink } from "@tanstack/react-router";

import { NomLink } from "~/components/NomLink.tsx";
import type { OrgtilknytningState } from "~/types/nom-admin/nom-admin-models.ts";
import { Tilknytningtype } from "~/types/nom-admin/nom-admin-models.ts";

export function OrgtilknytningStateTabell({
  sortedData,
}: {
  sortedData: OrgtilknytningState[];
}) {
  return (
    <>
      <Table.Header>
        <Table.ColumnHeader scope="col">FID</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">NAV ident</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">OrgEnhet ID</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">Gyldig fra</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">Gyldig til</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">
          Er daglig oppfølging
        </Table.ColumnHeader>
        <Table.ColumnHeader scope="col">Sektor</Table.ColumnHeader>
        <Table.ColumnHeader scope="col">Tilknytningtype</Table.ColumnHeader>
      </Table.Header>
      <Table.Body>
        {sortedData.map((orgtilknytning) => (
          <Table.Row key={orgtilknytning.fid}>
            <Table.DataCell>
              Se events{" "}
              <TanStackLink
                search={{ filterText: orgtilknytning.fid, fromOffset: 0 }}
                to="/sinks/ressurs"
              >
                {orgtilknytning.fid}
              </TanStackLink>
            </Table.DataCell>
            <Table.DataCell>
              <NomLink path={`ressurs/${orgtilknytning.navident}`}>
                {orgtilknytning.navident}
              </NomLink>
            </Table.DataCell>
            <Table.DataCell>
              <NomLink path={`org/${orgtilknytning.orgEnhetId}`}>
                {orgtilknytning.orgEnhetId}
              </NomLink>
            </Table.DataCell>
            <Table.DataCell>{orgtilknytning.gyldigFom}</Table.DataCell>
            <Table.DataCell>{orgtilknytning.gyldigTom}</Table.DataCell>
            <Table.DataCell>
              {orgtilknytning.erDagligOppfolging ? "Ja" : "Nei"}
            </Table.DataCell>
            <Table.DataCell>{orgtilknytning.sektor}</Table.DataCell>
            <Table.DataCell>
              {orgtilknytning.orgtilknytningType === null
                ? Tilknytningtype.INGEN.valueOf()
                : orgtilknytning.orgtilknytningType}
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </>
  );
}
