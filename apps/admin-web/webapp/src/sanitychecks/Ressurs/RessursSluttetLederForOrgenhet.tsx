import { Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRessurserSomHarSluttetMedLederRolleForOrgEnhet } from "~/api/queries.ts";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import type {
  EnhetSanityCheckResponse,
  RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse,
} from "~/types/nom-admin/nom-admin-models.ts";

export function RessursSluttetLederForOrgenhet() {
  const {
    data: ressurser = [],
    isLoading,
    isError,
  } = useQuery<
    RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse[],
    Error
  >({
    queryKey: ["API", "RESSURS_SLUTTET_LEDER_FOR_ORGENHET"],
    queryFn: async (): Promise<
      RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse[]
    > => getRessurserSomHarSluttetMedLederRolleForOrgEnhet(),
  });

  const { data, sort, handleSort } = useTableSort(ressurser ?? [], "navident");

  if (isLoading) {
    return <LoadingTable />;
  }

  if (ressurser.length === 0 && !isError) {
    return <EmptyTable />;
  }

  return (
    <div>
      <div style={{ paddingBottom: "20px" }}>
        <strong>Totalt antall ressurser: {ressurser?.length}</strong>
      </div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Navident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="visningsnavn" sortable>
            Visningsnavn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
            Sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Orgenheter</Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {data.map((ressurs) => (
            <Table.Row key={ressurs.navident} shadeOnHover={false}>
              <Table.DataCell>
                <NomLink path={`ressurs/${ressurs.navident}`}>
                  {ressurs.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{ressurs.visningsnavn}</Table.DataCell>
              <Table.DataCell>{ressurs.sluttdato}</Table.DataCell>
              <Table.DataCell>
                {ressurs.enheter?.map((orgEnhet) => (
                  <RessursOrgenehetComposite
                    enhet={orgEnhet}
                    key={ressurs.navident + orgEnhet.id}
                  />
                ))}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}

function RessursOrgenehetComposite({
  enhet,
}: {
  enhet: EnhetSanityCheckResponse;
}) {
  return (
    <div>
      <NomLink path={`org/${enhet.id}`}>{enhet.id}</NomLink>
      {enhet.navn} ({enhet.agressoId})
    </div>
  );
}
