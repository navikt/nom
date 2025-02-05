import { Heading, Loader, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getDuplikateOrgtilknytninger } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
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
    return (
      <>
        <Heading
          align="center"
          size="medium"
          style={{ display: "grid", justifyContent: "center" }}
        >
          Laster inn tabellen...
          <br />
          Dette kan ta opptil et minutt...
        </Heading>
        <div
          style={{
            display: "grid",
            justifyContent: "center",
            paddingTop: "30px",
          }}
        >
          <Loader size="3xlarge" variant="neutral" />
        </div>
      </>
    );
  }

  if (data.length === 0 && !isError) {
    return (
      <div>
        <Heading
          size="xlarge"
          style={{
            justifyContent: "center",
            display: "flex",
            paddingTop: "30xp",
          }}
        >
          Listen er tom
        </Heading>
      </div>
    );
  }

  return (
    <div>
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
              <Table.DataCell>{duplikateOTer.navident}</Table.DataCell>
              <Table.DataCell>{duplikateOTer.orgEnhetId}</Table.DataCell>
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
