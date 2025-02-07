import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getOverlappendeOrgtilknytninger } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { OverlappendeOrgtilknytning } from "~/types/nom-admin/nom-admin-models.ts";

export function OrgtilknytningOverlapp() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<OverlappendeOrgtilknytning[], Error>({
    queryKey: ["API", "OVERLAPPENDEORGTILKNYTNINGER"],
    queryFn: async (): Promise<OverlappendeOrgtilknytning[]> =>
      getOverlappendeOrgtilknytninger(),
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "fid1");

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
          <Table.ColumnHeader scope="col" sortKey="DOL1" sortable>
            Daglig oppfølging 1
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navident2" sortable>
            Nav ident 2
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
          <Table.ColumnHeader scope="col" sortKey="DOL2" sortable>
            Daglig oppfølging 2
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {data.map((overlappendeOTer) => (
            <Table.Row
              key={overlappendeOTer.fid1 + overlappendeOTer.fid2}
              shadeOnHover={false}
            >
              <Table.DataCell>{overlappendeOTer.navident1}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.orgEnhetId1}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.fid1}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.gyldigFom1}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.gyldigTom1}</Table.DataCell>
              <Table.DataCell>
                {overlappendeOTer.erDagligOppfolging1}
              </Table.DataCell>
              <Table.DataCell>{overlappendeOTer.navident2}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.orgEnhetId2}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.fid2}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.gyldigFom2}</Table.DataCell>
              <Table.DataCell>{overlappendeOTer.gyldigTom2}</Table.DataCell>
              <Table.DataCell>
                {overlappendeOTer.erDagligOppfolging2}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}
