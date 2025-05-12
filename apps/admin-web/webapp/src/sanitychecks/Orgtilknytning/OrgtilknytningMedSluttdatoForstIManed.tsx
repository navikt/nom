import { Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { type SetStateAction, useState } from "react";

import { getOrgtilknytningMedSluttdatoForstIManed } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { OrgtilknytningStateTabell } from "~/sanitychecks/Orgtilknytning/OrgtilknytningStateTabell.tsx";

export function OrgtilknytningMedSluttdatoForstIManed() {
  const orgtilknytningMedSluttdatoForstIManed = useQuery(
    getOrgtilknytningMedSluttdatoForstIManed(),
  );
  const [page, setPage] = useState(1);
  const handlePageChange = (value: SetStateAction<number>) => {
    window.scrollTo(0, 0);
    setPage(value);
  };
  const rowsPerPage = 50;

  const { data, sort, handleSort } = useTableSort(
    orgtilknytningMedSluttdatoForstIManed.data ?? [],
    "navident",
  );

  const sortedData = data.slice((page - 1) * rowsPerPage, page * rowsPerPage);

  return (
    <>
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
