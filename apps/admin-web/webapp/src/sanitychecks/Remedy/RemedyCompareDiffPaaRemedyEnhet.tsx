import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRemedyCompareDiffPaaRemedyEnhet } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { RemedyCompareResult } from "~/types/nom-admin/nom-admin-models.ts";

export function RemedyCompareDiffPaaRemedyEnhet() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<RemedyCompareResult[], Error>({
    queryKey: ["API", "REMEDY_COMPARE_DIFF_PAA_REMEDY_ENHET"],
    queryFn: async (): Promise<RemedyCompareResult[]> =>
      getRemedyCompareDiffPaaRemedyEnhet(),
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "navident");
  const dataWithKey = data.map((compareResponse) => ({
    compareResponse,
    key: crypto.randomUUID(),
  }));

  const csv = () => {
    const headers = [
      "Nav ident",
      "NOM remedy enhet",
      "Remedy 4-siffer enhet",
      "NOM sektor",
      "Remedy sektor",
      "NOM startdato",
      "NOM sluttdato",
      "Remedy aktiv",
      "Mastret i NOM",
    ];
    const rows = dataWithKey.map(({ compareResponse }) => {
      const { ressursState, importedRemedyUser, nomRemedyEnhet, mastretINom } =
        compareResponse;
      return [
        ressursState?.navident,
        nomRemedyEnhet,
        importedRemedyUser?.enhetsId,
        ressursState?.sektor,
        importedRemedyUser?.sektor,
        ressursState?.startdato,
        ressursState?.sluttdato,
        importedRemedyUser?.aktiv ? "X" : "",
        mastretINom ? "X" : "",
      ];
    });

    const csvContent =
      "data:text/csv;charset=utf-8," +
      [headers.join(","), ...rows.map((row) => row.join(","))].join("\n");

    return encodeURI(csvContent);
  };

  if (isLoading) {
    return <LoadingTable />;
  }

  if (data.length === 0 && !isError) {
    return <EmptyTable />;
  }

  return (
    <div>
      <div style={{ paddingBottom: "20px" }}>
        <b>Antall: {data.length}</b>
        (Inkluderer alle fra NOM)
      </div>
      <a download="compare_diff_pa_sektor.csv" href={csv()}>
        Last ned CSV
      </a>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Nav ident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomRemedyEnhet" sortable>
            NOM remedy enhet
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedyRemedyEnhet" sortable>
            Remedy 4-siffer enhet
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomSektor" sortable>
            NOM sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedySektor" sortable>
            Remedy sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomStart" sortable>
            NOM startdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomSlutt" sortable>
            NOM sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedyAktiv" sortable>
            Remedy aktiv
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="mastret" sortable>
            Mastret i NOM
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {dataWithKey.map((withKey) => {
            const {
              mastretINom,
              ressursState,
              nomRemedyEnhet,
              importedRemedyUser,
            } = withKey.compareResponse;
            return (
              <Table.Row key={withKey.key} shadeOnHover={false}>
                <Table.DataCell>
                  <NomLink path={`ressurs/${ressursState?.navident}`}>
                    {ressursState?.navident}
                  </NomLink>
                </Table.DataCell>
                <Table.DataCell>{nomRemedyEnhet}</Table.DataCell>
                <Table.DataCell>{importedRemedyUser?.enhetsId}</Table.DataCell>
                <Table.DataCell>{ressursState?.sektor}</Table.DataCell>
                <Table.DataCell>{importedRemedyUser?.sektor}</Table.DataCell>
                <Table.DataCell>{ressursState?.startdato}</Table.DataCell>
                <Table.DataCell>{ressursState?.sluttdato}</Table.DataCell>
                <Table.DataCell>
                  {importedRemedyUser?.aktiv ? "X" : ""}
                </Table.DataCell>
                <Table.DataCell>{mastretINom ? "X" : ""}</Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
