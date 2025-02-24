import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRessursMedFlerEnnEnDOL } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
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
    queryFn: async (): Promise<RessursMedFlereDOL[]> =>
      getRessursMedFlerEnnEnDOL(),
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
          <Table.ColumnHeader scope="col" sortKey="orgtilknytninger" sortable>
            Overlappende DOL orgtilknytninger
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {dataWithKey.map((dol) => (
            <Table.Row key={dol.key} shadeOnHover={false}>
              <Table.DataCell>
                <NomLink path={`ressurs/${dol.navident}`}>
                  {dol.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{dol.otErMedOverlapp}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}
