import { Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { type SetStateAction, useState } from "react";

import { getOrgtilknytningMedFlereAktivDOL } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { OrgtilknytningStateTabell } from "~/sanitychecks/Orgtilknytning/OrgtilknytningStateTabell.tsx";

export function OrgtilknytningMedFlereDOLSammePeriode() {
  const orgtilknytningMedFlereAktivDOL = useQuery(
    getOrgtilknytningMedFlereAktivDOL(),
  );
  const [page, setPage] = useState(1);
  const handlePageChange = (value: SetStateAction<number>) => {
    window.scrollTo(0, 0);
    setPage(value);
  };
  const rowsPerPage = 50;

  const { data, sort, handleSort } = useTableSort(
    orgtilknytningMedFlereAktivDOL.data ?? [],
    "navident",
  );

  const sortedData = data.slice((page - 1) * rowsPerPage, page * rowsPerPage);

  return (
    <>
      <div style={{ paddingBottom: "20px" }}>
        <b>
          Antall orgtilknytninger med flere aktiv DOL i samme periode:{" "}
          {orgtilknytningMedFlereAktivDOL.data?.length ?? 0}
        </b>
      </div>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <OrgtilknytningStateTabell sortedData={sortedData} />
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
