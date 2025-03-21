import { Button, HStack, Table, UNSAFE_Combobox } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import type { SubmitHandler } from "react-hook-form";
import { Controller, useForm } from "react-hook-form";

import { getRemedyCompareDiffPaaSluttdato } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type { RemedyCompareResult } from "~/types/nom-admin/nom-admin-models.ts";
import { Sektor } from "~/types/nom-api/nom-api-models.ts";

type Inputs = { sektorFilters: string[] };

export function RemedyCompareDiffPaaSluttdato() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<RemedyCompareResult[], Error>({
    queryKey: ["API", "REMEDY_COMPARE_DIFF_PAA_SLUTTDATO"],
    queryFn: async (): Promise<RemedyCompareResult[]> =>
      getRemedyCompareDiffPaaSluttdato(),
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "navident");
  const dataWithKey = data.map((compareResponse) => ({
    compareResponse,
    key: crypto.randomUUID(),
  }));

  const [filteredData, setFilteredData] = useState(dataWithKey);

  const onValidSubmit: SubmitHandler<Inputs> = (data) => {
    setFilteredData(
      dataWithKey.filter(({ compareResponse }) =>
        compareResponse.ressursState?.sektor
          ? !data.sektorFilters.includes(compareResponse.ressursState.sektor)
          : true,
      ),
    );
  };
  const { handleSubmit, control } = useForm<Inputs>({
    defaultValues: { sektorFilters: [] },
  });

  if (isLoading) {
    return <LoadingTable />;
  }

  if (data.length === 0 && !isError) {
    return <EmptyTable />;
  }

  const csv = () => {
    const headers = [
      "Nav ident",
      "NOM sluttdato",
      "Remedy sluttdato",
      "NOM sektor",
      "Remedy aktiv",
      "Remedy sektor",
      "Mastret i NOM",
    ];
    const rows = filteredData.map(({ compareResponse }) => {
      const { ressursState, importedRemedyUser, mastretINom } = compareResponse;
      return [
        ressursState?.navident,
        ressursState?.sluttdato,
        importedRemedyUser?.sluttDato,
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

  return (
    <div>
      <HStack as="form" gap="8" onSubmit={handleSubmit(onValidSubmit)}>
        <div style={{ paddingBottom: "20px" }}>
          Antall: {filteredData.length}/{data.length}
        </div>
        <Controller
          control={control}
          name="sektorFilters"
          render={({ field }) => (
            <UNSAFE_Combobox
              id="nom-sektor-filter"
              isMultiSelect
              label="Ekskluder NOM sektor"
              onToggleSelected={(option, isSelected) => {
                if (isSelected) {
                  field.onChange([...field.value, option]);
                } else {
                  field.onChange(field.value.filter((v) => v !== option));
                }
              }}
              options={Object.values(Sektor)}
              style={{ width: "150px" }}
            />
          )}
        />
        <div>
          <Button type="submit">Oppdater</Button>
        </div>
      </HStack>
      <a download="remedy_compare_diff_paa_sluttdato.csv" href={csv()}>
        Last ned CSV
      </a>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Nav ident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomSlutt" sortable>
            NOM sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="remedySlutt" sortable>
            Remedy sluttdato
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
          {filteredData.map((withKey) => {
            const { mastretINom, ressursState, importedRemedyUser } =
              withKey.compareResponse;
            return (
              <Table.Row key={withKey.key} shadeOnHover={false}>
                <Table.DataCell>
                  <NomLink path={`ressurs/${ressursState?.navident}`}>
                    {ressursState?.navident}
                  </NomLink>
                </Table.DataCell>
                <Table.DataCell>{ressursState?.sluttdato}</Table.DataCell>
                <Table.DataCell>{importedRemedyUser?.sluttDato}</Table.DataCell>
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
