import { Heading, Loader, Pagination, Table } from "@navikt/ds-react";
import type { UseQueryResult } from "@tanstack/react-query";
import { useState } from "react";

import { useTableSort } from "~/hooks/useTableSort.ts";
import type { OrgEnhetOrganisering } from "~/types/nom-admin/nom-admin-models.ts";

export function OrgEnhetOrganiseringTabell({
  orgEnhetOrganisering,
}: {
  orgEnhetOrganisering: UseQueryResult<OrgEnhetOrganisering[]>;
}) {
  const [page, setPage] = useState(1);
  const rowsPerPage = 100;
  const { data, sort, handleSort } = useTableSort(
    orgEnhetOrganisering.data || [],
    "orgEnhetId",
  );

  const sortedData = data.slice((page - 1) * rowsPerPage, page * rowsPerPage);

  if (orgEnhetOrganisering.isLoading) {
    return (
      <>
        <Heading
          size="medium"
          style={{ display: "grid", justifyContent: "center" }}
        >
          Laster inn tabellen...
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

  if (data.length === 0 && !orgEnhetOrganisering.isError) {
    return (
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
    );
  }

  return (
    <div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="orgEnhetId" sortable>
            Organisasjon Enhet Id
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="agresso" sortable>
            Unik agressoId
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="fom" sortable>
            Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="tom" sortable>
            Gyldig TOM
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {sortedData.map((orgEnhet) => (
            <Table.Row key={orgEnhet.orgEnhetId} shadeOnHover={false}>
              <Table.DataCell>{orgEnhet.orgEnhetId}</Table.DataCell>
              <Table.DataCell>{orgEnhet.uniqueAgressoId}</Table.DataCell>
              <Table.DataCell>{orgEnhet.gyldigFom}</Table.DataCell>
              <Table.DataCell>{orgEnhet.gyldigTom}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
      <Pagination
        count={Math.ceil(data.length / rowsPerPage)}
        onPageChange={setPage}
        page={page}
        size="medium"
        style={{ marginTop: "24px", justifyContent: "center", display: "flex" }}
      />
    </div>
  );
}
