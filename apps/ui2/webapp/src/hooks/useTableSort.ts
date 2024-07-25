import type { SortState } from "@navikt/ds-react";
import sortBy from "lodash/sortBy";
import { useState } from "react";

export function useTableSort<T>(tableData: T[], defaultOrderBy: string) {
  const [sort, setSort] = useState<SortState>({
    orderBy: defaultOrderBy,
    direction: "ascending",
  });
  const handleSort = (sortKey: string | undefined) => {
    if (sortKey) {
      setSort({
        orderBy: sortKey,
        direction:
          sort && sortKey === sort.orderBy && sort.direction === "ascending"
            ? "descending"
            : "ascending",
      });
    } else {
      setSort({ orderBy: defaultOrderBy, direction: "none" });
    }
  };

  return { data: sortTableData({ tableData, sort }), handleSort, sort };
}

export function sortTableData<T>({
  tableData,
  sort,
}: {
  tableData: T[];
  sort: SortState;
}) {
  const { orderBy, direction } = sort;

  const sortedMembersAscending = sortBy(tableData, orderBy);
  const reversed = direction === "descending";

  return reversed ? sortedMembersAscending.reverse() : sortedMembersAscending;
}
