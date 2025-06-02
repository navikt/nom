import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getAllErrorStates } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { ErrorState } from "~/types/nom-remedy-integrasjon/nom-remedy-integrasjon-models.ts";
import { formatDisplayDatetime } from "~/utils.ts";

export function RemedyErrorState() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<ErrorState[], Error>({
    queryKey: ["API", "REMEDY_ERROR_STATE"],
    queryFn: async (): Promise<ErrorState[]> => getAllErrorStates(),
  });

  const { data, sort, handleSort } = useTableSort(dataset, "createdAt");

  if (isLoading) {
    return <LoadingTable />;
  }

  if (data.length === 0 && !isError) {
    return <EmptyTable />;
  }

  return (
    <>
      <div style={{ paddingBottom: "20px" }}>
        <b>Antall: {data.length}</b>
        <br />
      </div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="errorId" sortable>
            ID
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Navident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="errorStateFid" sortable>
            Error State Fid
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="errorStateType" sortable>
            Statetype
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="errorMessage" sortable>
            Feilmelding
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="createdAt" sortable>
            Dato feilmelding
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {data.map((errorState) => (
            <Table.Row key={errorState.errorId}>
              <Table.DataCell>{errorState.errorId}</Table.DataCell>
              <Table.DataCell>
                <NomLink path={`/ressurs/${errorState.navident}`}>
                  {errorState.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{errorState.errorStateFid}</Table.DataCell>
              <Table.DataCell>{errorState.errorStateType}</Table.DataCell>
              <Table.DataCell>{errorState.errorMessage}</Table.DataCell>
              <Table.DataCell>
                {formatDisplayDatetime(errorState.createdAt)}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </>
  );
}
