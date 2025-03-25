import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRemedyCompareDiffPaaNavn } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { RemedyCompareResult } from "~/types/nom-admin/nom-admin-models.ts";

export function RemedyCompareDiffPaaNavn() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<RemedyCompareResult[], Error>({
    queryKey: ["API", "REMEDY_COMPARE_DIFF_PAA_NAVN"],
    queryFn: async (): Promise<RemedyCompareResult[]> =>
      getRemedyCompareDiffPaaNavn(),
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "navident");
  const dataWithKey = data.map((compareResponse) => ({
    compareResponse,
    key: crypto.randomUUID(),
  }));

  const csv = () => {
    const headers = [
      "Nav ident",
      "NOM navn",
      "Remedy navn",
      "Epost",
      "NOM startdato",
      "NOM sluttdato",
      "NOM sektor",
      "Remedy aktiv",
      "Remedy sektor",
      "Mastret i NOM",
    ];
    const rows = dataWithKey.map(({ compareResponse }) => {
      const { ressursState, importedRemedyUser, mastretINom, epost } =
        compareResponse;
      return [
        ressursState?.navident,
        ressursState?.visningsnavn.mellomnavn
          ? `${ressursState?.visningsnavn.fornavn} ${ressursState?.visningsnavn.mellomnavn} ${ressursState?.visningsnavn.etternavn}`
          : `${ressursState?.visningsnavn.fornavn} ${ressursState?.visningsnavn.etternavn}`,
        importedRemedyUser?.visningsnavn,
        epost,
        ressursState?.startdato,
        ressursState?.sluttdato,
        ressursState?.sektor,
        importedRemedyUser?.aktiv ? "X" : "",
        importedRemedyUser?.sektor,
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
        (Ekskludert alle hvor NOM sluttdato er før 2024)
      </div>
      <a download="remedy_compare_diff_paa_navn.csv" href={csv()}>
        Last ned CSV
      </a>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Nav ident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomNavn" sortable>
            NOM navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedyNavn" sortable>
            Remedy navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="epost" sortable>
            Epost
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomStart" sortable>
            NOM startdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomSlutt" sortable>
            NOM sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomSektor" sortable>
            NOM sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedyAktiv" sortable>
            Remedy aktiv
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedySektor" sortable>
            Remedy sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="mastret" sortable>
            Mastret i NOM
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {dataWithKey.map((withKey) => {
            const { mastretINom, ressursState, epost, importedRemedyUser } =
              withKey.compareResponse;
            const nomNavn = ressursState?.visningsnavn.mellomnavn
              ? `${ressursState?.visningsnavn.fornavn} ${ressursState?.visningsnavn.mellomnavn} ${ressursState?.visningsnavn.etternavn}`
              : `${ressursState?.visningsnavn.fornavn} ${ressursState?.visningsnavn.etternavn}`;
            return (
              <Table.Row key={withKey.key} shadeOnHover={false}>
                <Table.DataCell>
                  <NomLink path={`ressurs/${ressursState?.navident}`}>
                    {ressursState?.navident}
                  </NomLink>
                </Table.DataCell>
                <Table.DataCell>{nomNavn}</Table.DataCell>
                <Table.DataCell>
                  {importedRemedyUser?.visningsnavn}
                </Table.DataCell>
                <Table.DataCell>{epost}</Table.DataCell>
                <Table.DataCell>{ressursState?.startdato}</Table.DataCell>
                <Table.DataCell>{ressursState?.sluttdato}</Table.DataCell>
                <Table.DataCell>{ressursState?.sektor}</Table.DataCell>
                <Table.DataCell>
                  {importedRemedyUser?.aktiv ? "X" : ""}
                </Table.DataCell>
                <Table.DataCell>{importedRemedyUser?.sektor}</Table.DataCell>
                <Table.DataCell>{mastretINom ? "X" : ""}</Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
