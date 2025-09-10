import { Button, Heading, Loader, Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import type { SetStateAction } from "react";
import { useState } from "react";

import { getListAvDuplikateEnheter } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { AntallOgExcelEksportKnapp } from "~/sanitychecks/components/AntallOgExcelEksportKnapp.tsx";
import type { OrgEnhetState } from "~/types/nom-admin/nom-admin-models.ts";

export function DuplikateEnheter() {
  const [page, setPage] = useState(1);
  const [fetchData, setFetchData] = useState(false);
  const handlePageChange = (value: SetStateAction<number>) => {
    window.scrollTo(0, 0);
    setPage(value);
  };
  const rowsPerPage = 100;
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<OrgEnhetState[], Error>({
    queryKey: ["API", "DUPLIKATEENHETER"],
    queryFn: async (): Promise<OrgEnhetState[]> => getListAvDuplikateEnheter(),
    enabled: fetchData,
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "id");

  const sortedData = data.slice((page - 1) * rowsPerPage, page * rowsPerPage);

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
          Listen er tom eller ikke hentet
        </Heading>
        <div
          style={{
            justifyContent: "center",
            display: "flex",
            paddingTop: "50px",
          }}
        >
          <Button onClick={() => setFetchData(true)}>Hent data</Button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <AntallOgExcelEksportKnapp
        antall={sortedData.length}
        exportName="duplikateEnheter"
        exportType="orgenhet"
      />
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="fid" sortable>
            FID
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="id" sortable>
            Orgenhet Id
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navn" sortable>
            Orgenhets Navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom" sortable>
            Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom" sortable>
            Gyldig TOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="uniqueAgressoId" sortable>
            Unik Agresso Id
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {sortedData.map((orgEnhet) => (
            <Table.Row key={orgEnhet.id} shadeOnHover={false}>
              <Table.DataCell>{orgEnhet.fid}</Table.DataCell>
              <Table.DataCell>{orgEnhet.id}</Table.DataCell>
              <Table.DataCell>{orgEnhet.navn}</Table.DataCell>
              <Table.DataCell>{orgEnhet.gyldigFom}</Table.DataCell>
              <Table.DataCell>{orgEnhet.gyldigTom}</Table.DataCell>
              <Table.DataCell>{orgEnhet.uniqueAgressoId}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
      <Pagination
        count={Math.ceil(data.length / rowsPerPage)}
        onPageChange={handlePageChange}
        page={page}
        size="medium"
        style={{ marginTop: "24px", justifyContent: "center", display: "flex" }}
      />
    </div>
  );
}
