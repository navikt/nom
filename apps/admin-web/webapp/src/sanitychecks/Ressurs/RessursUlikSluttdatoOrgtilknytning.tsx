import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRessursOrgtilknytningUlikSluttdato } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { RessursOrgtilknytningUlikSluttdato } from "~/types/nom-admin/nom-admin-models.ts";

export function RessursUlikSluttdatoOrgtilknytning() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<RessursOrgtilknytningUlikSluttdato[], Error>({
    queryKey: ["API", "ULIKSLUTTDATORESSURSORGTILKNYTNING"],
    queryFn: async (): Promise<RessursOrgtilknytningUlikSluttdato[]> =>
      getRessursOrgtilknytningUlikSluttdato(),
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "navident");

  if (isLoading) {
    return <LoadingTable />;
  }

  if (data.length === 0 && !isError) {
    return <EmptyTable />;
  }

  return (
    <div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Nav ident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhet" sortable>
            Orgenhet
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
            Ressurs sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom" sortable>
            Orgtilknytning gyldig TOM
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {data.map((ulikDato) => (
            <Table.Row
              key={
                ulikDato.navident +
                ulikDato.orgEnhetId +
                ulikDato.ressursSluttdato +
                ulikDato.orgtilknytningSluttdato
              }
              shadeOnHover={false}
            >
              <Table.DataCell>
                <NomLink path={`ressurs/${ulikDato.navident}`}>
                  {ulikDato.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{ulikDato.orgEnhetId}</Table.DataCell>
              <Table.DataCell>{ulikDato.ressursSluttdato}</Table.DataCell>
              <Table.DataCell>
                {ulikDato.orgtilknytningSluttdato}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}
