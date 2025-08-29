import { Heading, Loader, Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import type { SetStateAction } from "react";
import { useState } from "react";

import { getDuplikateRessurserPaaNavident } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { RessursStateTabell } from "~/sanitychecks/Ressurs/RessursStateTabell.tsx";
import type { RessursState } from "~/types/nom-api/nom-api-models.ts";

export function RessursDuplikat() {
  const [page, setPage] = useState(1);
  const handlePageChange = (value: SetStateAction<number>) => {
    window.scrollTo(0, 0);
    setPage(value);
  };
  const rowsPerPage = 50;
  const {
    data: ressursDuplikat = [],
    isLoading,
    isError,
  } = useQuery<RessursState[], Error>({
    queryKey: ["API", "RESSURSDUPLIKAT"],
    queryFn: async (): Promise<RessursState[]> =>
      getDuplikateRessurserPaaNavident(),
  });

  const { data, sort, handleSort } = useTableSort(
    ressursDuplikat ?? [],
    "navident",
  );

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
          Listen er tom
        </Heading>
      </div>
    );
  }

  return (
    <>
      <div style={{ paddingBottom: "20px" }}>
        <strong>Totalt antall ressurser: {ressursDuplikat?.length}</strong>
      </div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <RessursStateTabell sortedData={sortedData} />
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
    </>
  );
}
