import { Button, Heading, Loader, Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import type { SetStateAction } from "react";
import { useState } from "react";

import { getListAvOTMedGyldighetUtoverEnhetensGyldighet } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import type { OrgtilknytningMedOrgEnhet } from "~/types/nom-admin/nom-admin-models.ts";

export function OrgtilknytningMedOrgEnhetTabell() {
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
  } = useQuery<OrgtilknytningMedOrgEnhet[], Error>({
    queryKey: ["API", "DUPLIKATEENHETER"],
    queryFn: async (): Promise<OrgtilknytningMedOrgEnhet[]> =>
      getListAvOTMedGyldighetUtoverEnhetensGyldighet(),
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
            NAV-ident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="uniqueAgressoId" sortable>
            Unik AgressoId
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhetId" sortable>
            OrgEnhetId
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigFom" sortable>
            OT Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom" sortable>
            OT Gyldig TOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhetGyldigFom" sortable>
            OrgEnhet Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhetGyldigTom" sortable>
            OrgEnhet Gyldig TOM
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {sortedData.map((orgtilknytning) => (
            <Table.Row key={orgtilknytning.navident} shadeOnHover={false}>
              <Table.DataCell>{orgtilknytning.navident}</Table.DataCell>
              <Table.DataCell>{orgtilknytning.uniqueAgressoId}</Table.DataCell>
              <Table.DataCell>{orgtilknytning.orgEnhetId}</Table.DataCell>
              <Table.DataCell>{orgtilknytning.gyldigFom}</Table.DataCell>
              <Table.DataCell>{orgtilknytning.gyldigTom}</Table.DataCell>
              <Table.DataCell>
                {orgtilknytning.orgEnhetGyldigFom}
              </Table.DataCell>
              <Table.DataCell>
                {orgtilknytning.orgEnhetGyldigTom}
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
