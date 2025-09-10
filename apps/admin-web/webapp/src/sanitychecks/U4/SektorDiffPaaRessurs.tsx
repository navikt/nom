import { Button, Heading, Loader, Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import type { SetStateAction } from "react";
import { useState } from "react";

import { sektorDifferansePaaRessursOptions } from "~/api/apiQueries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import type { U4Diff } from "~/types/nom-api/nom-api-admin-models.ts";

export function SektorDiffPaaRessurs() {
  const [page, setPage] = useState(1);
  const [fetchData, setFetchData] = useState(false);
  const handlePageChange = (value: SetStateAction<number>) => {
    window.scrollTo(0, 0);
    setPage(value);
  };

  const rowsPerPage = 500;
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<U4Diff[], Error>({
    queryKey: ["API", "SEKTORDIFFPAARESSURS"],
    queryFn: async (): Promise<U4Diff[]> => sektorDifferansePaaRessursOptions(),
    enabled: fetchData,
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "navident");

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
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Navident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomSektor" sortable>
            NOM Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="U4SektorPaaRessurs" sortable>
            U4 Sektor på Ressurs
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="U4RessursType" sortable>
            U4 Ressurs type
          </Table.ColumnHeader>
          <Table.ColumnHeader
            scope="col"
            sortKey="u4RessursAnsettelsestypeBesk"
            sortable
          >
            U4 Ressurs ansettelsestype besk.
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {sortedData.map((ident, index) => (
            <Table.Row key={index} shadeOnHover={false}>
              <Table.DataCell>
                <NomLink path={`ressurs/${ident.navident}`}>
                  {ident.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{ident.nomSektor}</Table.DataCell>
              <Table.DataCell>{ident.u4SektorPaaRessurs}</Table.DataCell>
              <Table.DataCell>{ident.u4RessursType}</Table.DataCell>
              <Table.DataCell>
                {ident.u4RessursAnsettelsestypeBesk}
              </Table.DataCell>
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
