import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getArbeidsomraadeInfo } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { AntallOgExcelEksportKnapp } from "~/sanitychecks/components/AntallOgExcelEksportKnapp.tsx";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type {
  LoevnodeMedFeilAntallArbOmr,
  OrgEnhetArbeidsomraadeInfo,
} from "~/types/nom-admin/nom-admin-models.ts";

export function Arbeidsomraade() {
  const {
    data: arbOmraadeInfo,
    isLoading,
    isError,
  } = useQuery<OrgEnhetArbeidsomraadeInfo, Error>({
    queryKey: ["API", "ORGENHET_ARBEIDSOMRAADEINFO"],
    queryFn: async (): Promise<OrgEnhetArbeidsomraadeInfo> =>
      getArbeidsomraadeInfo(),
  });

  const arbOmraaderMedFeil: LoevnodeMedFeilAntallArbOmr[] =
    arbOmraadeInfo?.loevnoderMedFeil ?? [];

  const { data, sort, handleSort } = useTableSort(
    arbOmraaderMedFeil ?? [],
    "orgenhetNavn",
  );
  const dataWithKey = data.map((lovnode) => ({
    ...lovnode,
    key: crypto.randomUUID(),
  }));

  if (isLoading) {
    return <LoadingTable />;
  }

  if (arbOmraaderMedFeil.length === 0 && !isError) {
    return <EmptyTable />;
  }

  return (
    <div>
      <AntallOgExcelEksportKnapp
        antall={arbOmraadeInfo?.loevnoderMedFeil.length}
        exportName="validerArbeidsomraade"
        exportType="orgenhet"
      />
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col" sortKey="orgenhetId" sortable>
              Id
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="orgenhetNavn" sortable>
              Navn
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="antallArbOmr" sortable>
              Antall arbeidsområder
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {dataWithKey.map((arbOmr) => (
            <Table.Row key={arbOmr.key} shadeOnHover={false}>
              <Table.DataCell>
                <NomLink path={`org/${arbOmr.orgenhetId}`}>
                  {arbOmr.orgenhetId}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{arbOmr.orgenhetNavn}</Table.DataCell>
              <Table.DataCell>{arbOmr.antallArbOmr}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}
