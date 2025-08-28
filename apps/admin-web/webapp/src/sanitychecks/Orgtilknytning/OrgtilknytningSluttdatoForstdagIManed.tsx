import { Pagination, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { type SetStateAction, useState } from "react";

import { getOrgtilknytningSluttdatoForstdagIManed } from "~/api/queries.ts";
import { useTableSort } from "~/hooks/useTableSort.ts";
import { AntallOgExcelEksportKnapp } from "~/sanitychecks/components/AntallOgExcelEksportKnapp.tsx";
import { OrgtilknytningStateTabell } from "~/sanitychecks/Orgtilknytning/OrgtilknytningStateTabell.tsx";

export function OrgtilknytningSluttdatoForstdagIManed() {
  const orgtilknytningSluttdatoForstdagIManed = useQuery(
    getOrgtilknytningSluttdatoForstdagIManed(),
  );
  const [page, setPage] = useState(1);
  const handlePageChange = (value: SetStateAction<number>) => {
    window.scrollTo(0, 0);
    setPage(value);
  };
  const rowsPerPage = 50;

  const { data, sort, handleSort } = useTableSort(
    orgtilknytningSluttdatoForstdagIManed.data ?? [],
    "navident",
  );

  const sortedData = data.slice((page - 1) * rowsPerPage, page * rowsPerPage);

  return (
    <>
      <AntallOgExcelEksportKnapp
        antall={data.length}
        exportName="listTilkoblingerSomharTomDatoForstDagImaneden"
        exportType="orgtilknytning"
      />
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
