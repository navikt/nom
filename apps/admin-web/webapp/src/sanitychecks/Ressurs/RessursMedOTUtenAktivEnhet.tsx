import { Button, Heading, Loader, Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as TanStackLink } from "@tanstack/react-router";
import type { SetStateAction } from "react";
import { useState } from "react";

import { getRessursMedAktivOrgTilknytningHvorEnhetErInaktiv } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import type { RessursMedInaktivEnhet } from "~/types/nom-admin/nom-admin-models.ts";

export function RessursMedOTUtenAktivEnhet({
  inkluderEksterne,
  inkluderHelfo,
}: {
  inkluderEksterne: boolean;
  inkluderHelfo: boolean;
}) {
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
  } = useQuery<RessursMedInaktivEnhet[], Error>({
    queryKey: [
      "API",
      "RESSURSMEDINAKTIVENHET",
      inkluderEksterne,
      inkluderHelfo,
    ],
    queryFn: async (): Promise<RessursMedInaktivEnhet[]> =>
      getRessursMedAktivOrgTilknytningHvorEnhetErInaktiv(
        inkluderEksterne,
        inkluderHelfo,
      ),
    enabled: fetchData || inkluderEksterne || inkluderHelfo,
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
          <Table.ColumnHeader scope="col" sortKey="ressursFid" sortable>
            Ressurs FID
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
            Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhetId" sortable>
            OrgEnhetId
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhetNavn" sortable>
            OrgEnhetNavn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="Unik Agresso ID" sortable>
            Unik Agresso ID
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {sortedData.map((ressurs) => (
            <Table.Row key={null} shadeOnHover={false}>
              <Table.DataCell>
                <NomLink path={`ressurs/${ressurs.navident}`}>
                  {ressurs.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>
                <TanStackLink
                  search={{ filterText: ressurs.ressursFid, fromOffset: 0 }}
                  to="/sinks/ressurs"
                >
                  {ressurs.ressursFid}
                </TanStackLink>
              </Table.DataCell>
              <Table.DataCell>{ressurs.sektor}</Table.DataCell>
              <Table.DataCell>
                <NomLink path={`org/${ressurs.orgEnhetId}`}>
                  {ressurs.orgEnhetId}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{ressurs.orgEnhetNavn}</Table.DataCell>
              <Table.DataCell>{ressurs.uniqueAgressoId}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
      <Pagination
        count={Math.ceil(data.length / rowsPerPage)}
        onPageChange={handlePageChange}
        page={page}
        size="medium"
        style={{
          marginTop: "24px",
          justifyContent: "center",
          display: "flex",
        }}
      />
    </div>
  );
}
