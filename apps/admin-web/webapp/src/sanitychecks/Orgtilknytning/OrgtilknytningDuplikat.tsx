import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getDuplikateOrgtilknytninger } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { DuplikatOrgtilknytning } from "~/types/nom-admin/nom-admin-models.ts";

export function OrgtilknytningDuplikat() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<DuplikatOrgtilknytning[], Error>({
    queryKey: ["API", "DUPLIKATEORGTILKNYTNINGER"],
    queryFn: async (): Promise<DuplikatOrgtilknytning[]> =>
      getDuplikateOrgtilknytninger(),
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
      <div style={{ marginBottom: "20px" }}>
        <b>Antall duplikate orgtilknytninger: {data.length}</b>
      </div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="fid1" sortable>
            FID 1
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="fid2" sortable>
            FID 2
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Nav ident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhet" sortable>
            Orgenhet
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom" sortable>
            Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
            Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="tilknytningstype" sortable>
            Tilknytningstype
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="DOL" sortable>
            Daglig oppfølging
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {data.map((duplikateOTer) => (
            <Table.Row
              key={duplikateOTer.fid1 + duplikateOTer.fid2}
              shadeOnHover={false}
            >
              <Table.DataCell>{duplikateOTer.fid1}</Table.DataCell>
              <Table.DataCell>{duplikateOTer.fid2}</Table.DataCell>
              <Table.DataCell>
                <NomLink path={`ressurs/${duplikateOTer.navident}`}>
                  {duplikateOTer.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>
                <NomLink path={`org/${duplikateOTer.orgEnhetId}`}>
                  {duplikateOTer.orgEnhetId}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{duplikateOTer.gyldigFom}</Table.DataCell>
              <Table.DataCell>{duplikateOTer.sektor}</Table.DataCell>
              <Table.DataCell>
                {duplikateOTer.orgtilknytningType}
              </Table.DataCell>
              <Table.DataCell>
                {duplikateOTer.erDagligOppfolging}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}
