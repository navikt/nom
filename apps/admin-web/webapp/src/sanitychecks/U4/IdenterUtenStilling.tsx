import { Button, Heading, Loader, Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import type { SetStateAction } from "react";
import { useState } from "react";

import { identerUtenStillingIU4Options } from "~/api/apiQueries.ts";
import { JsonModal } from "~/components/JsonModal.tsx";
import { NomLink } from "~/components/NomLink.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import type { U4Diff } from "~/types/nom-api/nom-api-admin-models.ts";

export function IdenterUtenStilling() {
  const [page, setPage] = useState(1);
  const [fetchData, setFetchData] = useState(false);
  const handlePageChange = (value: SetStateAction<number>) => {
    window.scrollTo(0, 0);
    setPage(value);
  };

  const rowsPerPage = 500;
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<U4Diff[], Error>({
    queryKey: ["API", "IDENTERUTENSTILLING"],
    queryFn: async (): Promise<U4Diff[]> => identerUtenStillingIU4Options(),
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
          <Table.ColumnHeader scope="col" sortKey="id" sortable>
            ID
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Navident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="DOL">
            DOL
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nom-gyldigFom" sortable>
            NOM Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nom-gyldigTom" sortable>
            NOM Gyldig TOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomOrgEnhetId" sortable>
            NOM OrgEnhet ID
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomOrgEnhetNavn" sortable>
            NOM OrgEnhet Navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomOrgtilknytning">
            NOM Orgtilknytning
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomOtSektor" sortable>
            NOM OT Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomSektor" sortable>
            NOM Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="type" sortable>
            Type
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="U4Dol" sortable>
            U4 DOL
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="U4GyldigFom" sortable>
            U4 Gyldig FOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="U4GyldigTom" sortable>
            U4 Gyldig TOM
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="U4OrgEnhetId" sortable>
            U4 OrgEnhet ID
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="U4OrgEnhetNavn" sortable>
            U4 OrgEnhet Navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="U4Sektor" sortable>
            U4 Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="U4stillingsperiode" sortable>
            U4 Stillingsperiode
          </Table.ColumnHeader>
        </Table.Header>
        <Table.Body>
          {sortedData.map((ident) => (
            <Table.Row key={ident.navident} shadeOnHover={false}>
              <Table.DataCell>{ident.id}</Table.DataCell>
              <Table.DataCell>
                <NomLink path={`ressurs/${ident.navident}`}>
                  {ident.navident}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{ident.nomDol ? "Ja" : "Nei"}</Table.DataCell>
              <Table.DataCell>{ident.nomGyldigFom}</Table.DataCell>
              <Table.DataCell>{ident.nomGyldigTom}</Table.DataCell>
              <Table.DataCell>
                <NomLink path={`org/${ident.nomOrgenhetId}`}>
                  {ident.nomOrgenhetId}
                </NomLink>
              </Table.DataCell>
              <Table.DataCell>{ident.nomOrgenhetNavn}</Table.DataCell>
              <Table.DataCell>
                <JsonModal
                  data={JSON.parse(ident.nomOrgtilknytning as string)}
                  text="NOM OT"
                />
              </Table.DataCell>
              <Table.DataCell>{ident.nomOtSektor}</Table.DataCell>
              <Table.DataCell>{ident.nomSektor}</Table.DataCell>
              <Table.DataCell>{ident.type}</Table.DataCell>
              <Table.DataCell>{ident.u4Dol}</Table.DataCell>
              <Table.DataCell>{ident.u4GyldigFom}</Table.DataCell>
              <Table.DataCell>{ident.u4GyldigTom}</Table.DataCell>
              <Table.DataCell>{ident.u4OrgenhetId}</Table.DataCell>
              <Table.DataCell>{ident.u4OrgenhetNavn}</Table.DataCell>
              <Table.DataCell>{ident.u4Sektor}</Table.DataCell>
              <Table.DataCell>{ident.u4StillingPeriode}</Table.DataCell>
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
