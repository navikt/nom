import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getOverlappendeHistoriskeDolOrgtilknytninger } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { OverlappendeOrgtilknytning } from "~/types/nom-admin/nom-admin-models.ts";

export function OrgtilknytningHistoriskDolOverlapp() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<OverlappendeOrgtilknytning[], Error>({
    queryKey: ["API", "OVERLAPPENDEORGTILKNYTNINGERHISTORISKDOL"],
    queryFn: async (): Promise<OverlappendeOrgtilknytning[]> =>
      getOverlappendeHistoriskeDolOrgtilknytninger(),
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "fid1");
  const dataWithKey = data.map((overlappendeOTer) => ({
    ...overlappendeOTer,
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
      <h1>Antall overlappende historiske DOL: {dataWithKey.length}</h1>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident1" sortable>
            Nav ident 1
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhet1" sortable>
            Orgenhet 1
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="fid1" sortable>
            FID 1
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom1" sortable>
            Gyldig FOM 1
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom1" sortable>
            Gyldig TOM 1
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhet2" sortable>
            Orgenhet 2
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="fid2" sortable>
            FID 2
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom2" sortable>
            Gyldig FOM 2
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom2" sortable>
            Gyldig TOM 2
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {dataWithKey.map((overlappendeOTer) => (
            <Table.Row key={overlappendeOTer.key} shadeOnHover={false}>
              <Table.DataCell>
                <NomLink path={`ressurs/${overlappendeOTer.navident1}`}>
                  {overlappendeOTer.navident1}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>
                <NomLink path={`org/${overlappendeOTer.orgEnhetId1}`}>
                  {overlappendeOTer.orgEnhetId1}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{overlappendeOTer.fid1}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.gyldigFom1}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.gyldigTom1}</Table.DataCell>
              <Table.DataCell>
                <NomLink path={`org/${overlappendeOTer.orgEnhetId2}`}>
                  {overlappendeOTer.orgEnhetId2}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{overlappendeOTer.fid2}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.gyldigFom2}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.gyldigTom2}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}
