import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRessursMedFlereDOL } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { RessursMedFlereDOL } from "~/types/nom-admin/nom-admin-models.ts";

export function RessurserMedOverlappendeDOL() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<RessursMedFlereDOL[], Error>({
    queryKey: ["API", "RESSURSMEDFLEREDOL"],
    queryFn: async (): Promise<RessursMedFlereDOL[]> => getRessursMedFlereDOL(),
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "navident");
  const dataWithKey = data.map((ressursMedDOL) => ({
    ...ressursMedDOL,
    key: crypto.randomUUID(),
  }));

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
          <Table.ColumnHeader scope="col" sortKey="orgEnhetNavn" sortable>
            Orgenhet
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom" sortable>
            Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom" sortable>
            Gyldig TOM
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {dataWithKey.map((dol) => (
            <Table.Row key={dol.key} shadeOnHover={false}>
              <Table.DataCell>{dol.navident}</Table.DataCell>
              <Table.DataCell>{dol.orgEnhetId}</Table.DataCell>
              <Table.DataCell>{dol.orgEnhetNavn}</Table.DataCell>
              <Table.DataCell>{dol.gyldigFom}</Table.DataCell>
              <Table.DataCell>{dol.gyldigTom}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}
