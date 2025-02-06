import { Heading, Loader, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";

import { getRessursOrgtilknytningUlikSluttdato } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import type { RessursOrgtilknytningUlikSluttdato } from "~/types/nom-admin/nom-admin-models.ts";

export function UlikSluttdatoRessursOrgtilknytning() {
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<RessursOrgtilknytningUlikSluttdato[], Error>({
    queryKey: ["API", "ULIKSLUTTDATORESSURSORGTILKNYTNING"],
    queryFn: async (): Promise<RessursOrgtilknytningUlikSluttdato[]> =>
      getRessursOrgtilknytningUlikSluttdato(),
  });

  const { data, sort, handleSort } = useTableSort(dataset ?? [], "navident");

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
          Listen er tom
        </Heading>
      </div>
    );
  }

  return (
    <div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Nav ident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgEnhet" sortable>
            Orgenhet
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
            Ressurs sluttdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="gyldigTom" sortable>
            Orgtilknytning gyldig TOM
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {data.map((ulikDato) => (
            <Table.Row
              key={
                ulikDato.navident +
                ulikDato.orgEnhetId +
                ulikDato.ressursSluttdato +
                ulikDato.orgtilknytningSluttdato
              }
              shadeOnHover={false}
            >
              <Table.DataCell>{ulikDato.navident}</Table.DataCell>
              <Table.DataCell>{ulikDato.orgEnhetId}</Table.DataCell>
              <Table.DataCell>{ulikDato.ressursSluttdato}</Table.DataCell>
              <Table.DataCell>
                {ulikDato.orgtilknytningSluttdato}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </div>
  );
}
