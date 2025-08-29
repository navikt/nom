import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getDuplikateRessursHendelser } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { RessursHendelseKafkaDTO } from "~/types/nom-admin/nom-admin-models.ts";
import { formatVisningsnavn } from "~/utils.ts";

export function DuplikateRessursHendelser() {
  const {
    data: duplikater = [],
    isLoading,
    isError,
  } = useQuery<RessursHendelseKafkaDTO[], Error>({
    queryKey: ["API", "DUPLIKATE_RESSURS_HENDELSER"],
    queryFn: async (): Promise<RessursHendelseKafkaDTO[]> =>
      getDuplikateRessursHendelser(),
  });

  const { data, sort, handleSort } = useTableSort(duplikater ?? [], "navident");

  if (isLoading) {
    return <LoadingTable />;
  }

  if (duplikater.length === 0 && !isError) {
    return <EmptyTable />;
  }

  return (
    <div>
      <div style={{ marginBottom: "20px" }}>
        <b>Antall duplikate ressurs hendelser: {duplikater.length}</b>
      </div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Navident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Fid</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Visningsnavn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
            Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="startdato" sortable>
            Startdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
            Sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">OT orgEnhet</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">OT gyldig FOM</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">OT gyldig TOM</Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {data.map((ressursHendelse) => (
            <Table.Row key={ressursHendelse.navident} shadeOnHover={false}>
              <Table.DataCell>
                <NomLink path={`ressurs/${ressursHendelse.navident}`}>
                  {ressursHendelse.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{ressursHendelse.fid}</Table.DataCell>
              <Table.DataCell>
                {formatVisningsnavn(ressursHendelse.visningsnavn)}
              </Table.DataCell>
              <Table.DataCell>{ressursHendelse.sektor}</Table.DataCell>
              <Table.DataCell>{ressursHendelse.startdato}</Table.DataCell>
              <Table.DataCell>{ressursHendelse.sluttdato}</Table.DataCell>
              <Table.DataCell>
                {ressursHendelse.orgtilknytning.orgEnhetId}
              </Table.DataCell>
              <Table.DataCell>
                {ressursHendelse.orgtilknytning.gyldigFom}
              </Table.DataCell>
              <Table.DataCell>
                {ressursHendelse.orgtilknytning.gyldigTom}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}
