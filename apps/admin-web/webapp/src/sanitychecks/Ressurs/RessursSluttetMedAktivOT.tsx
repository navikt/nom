import { Button, Heading, Loader, Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { type SetStateAction, useState } from "react";

import { getRessursMedAktivOrgTilknytningHvorEnhetErInaktivMedUnderenhet } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { RessursMedEnhetTabell } from "~/sanitychecks/Ressurs/RessursMedEnhetTabell.tsx";
import type { RessursMedEnhet } from "~/types/nom-admin/nom-admin-models.ts";

export function RessursSluttetMedAktivOT() {
  const [page, setPage] = useState(1);
  const [fetchData, setFetchData] = useState(false);
  const handlePageChange = (value: SetStateAction<number>) => {
    window.scrollTo(0, 0);
    setPage(value);
  };
  const rowsPerPage = 100;
  const {
    data: dataset = [],
    isLoading,
    isError,
  } = useQuery<RessursMedEnhet[], Error>({
    queryKey: ["API", "RESSURSLUTTETMEDAKTIVOT"],
    queryFn: async (): Promise<RessursMedEnhet[]> =>
      getRessursMedAktivOrgTilknytningHvorEnhetErInaktivMedUnderenhet(),
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
        <RessursMedEnhetTabell sortedData={sortedData} />
      </Table>
      <Pagination
        count={Math.ceil(data.length / rowsPerPage)}
        onPageChange={handlePageChange}
        page={page}
        size="medium"
        style={{
          marginTop: "24px",
          justifyContent: "center",
          display: "flex",
        }}
      />
    </div>
  );
}
