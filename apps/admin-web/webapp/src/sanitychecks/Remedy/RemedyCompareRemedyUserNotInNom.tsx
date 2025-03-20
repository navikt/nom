import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRemedyUsersNotInNom } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { RemedyCompareResult } from "~/types/nom-admin/nom-admin-models.ts";

export function RemedyCompareRemedyUserNotInNom() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<RemedyCompareResult[], Error>({
    queryKey: ["API", "REMEDY_COMPARE_REMEDY_USER_NOT_IN_NOM"],
    queryFn: async (): Promise<RemedyCompareResult[]> =>
      getRemedyUsersNotInNom(),
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "navident");
  const dataWithKey = data.map((compareResponse) => ({
    compareResponse,
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
      <div style={{ paddingBottom: "20px" }}>Antall: {data.length}</div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Nav ident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedyNavn" sortable>
            Remedy navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedyReg" sortable>
            Remedy registrert
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedySlutt" sortable>
            Remedy sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedyAktiv" sortable>
            Remedy aktiv
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedySektor" sortable>
            Remedy sektor
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {dataWithKey.map((withKey) => {
            const { importedRemedyUser } = withKey.compareResponse;
            return (
              <Table.Row key={withKey.key} shadeOnHover={false}>
                <Table.DataCell>
                  <NomLink path={`ressurs/${importedRemedyUser?.navident}`}>
                    {importedRemedyUser?.navident}
                  </NomLink>
                </Table.DataCell>
                <Table.DataCell>
                  {importedRemedyUser?.visningsnavn}
                </Table.DataCell>
                <Table.DataCell>{importedRemedyUser?.regDato}</Table.DataCell>
                <Table.DataCell>{importedRemedyUser?.sluttDato}</Table.DataCell>
                <Table.DataCell>
                  {importedRemedyUser?.aktiv ? "X" : ""}
                </Table.DataCell>
                <Table.DataCell>{importedRemedyUser?.sektor}</Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
