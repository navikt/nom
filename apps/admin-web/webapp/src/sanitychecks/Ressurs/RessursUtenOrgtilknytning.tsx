import { Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { type SetStateAction, useState } from "react";

import { getRessurserUtenOrgtilknytning } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { EmptyTable } from "~/sanitychecks/components/EmptyTable.tsx";
import { LoadingTable } from "~/sanitychecks/components/LoadingTable.tsx";
import { RessursStateTabell } from "~/sanitychecks/Ressurs/RessursStateTabell.tsx";
import type { RessursState } from "~/types/nom-admin/nom-admin-models.ts";

export function RessursUtenOrgtilknytning() {
  const [page, setPage] = useState(1);
  const handlePageChange = (value: SetStateAction<number>) => {
    window.scrollTo(0, 0);
    setPage(value);
  };
  const rowsPerPage = 50;
  const {
    data: ressurser = [],
    isLoading,
    isError,
  } = useQuery<RessursState[], Error>({
    queryKey: ["API", "RESSURS_UTEN_ORGTILKNYTNING"],
    queryFn: async (): Promise<RessursState[]> =>
      getRessurserUtenOrgtilknytning(),
  });

  const { data, sort, handleSort } = useTableSort(ressurser ?? [], "navident");

  const sortedData = data.slice((page - 1) * rowsPerPage, page * rowsPerPage);

  if (isLoading) {
    return <LoadingTable />;
  }

  if (data.length === 0 && !isError) {
    return <EmptyTable />;
  }

  return (
    <>
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
