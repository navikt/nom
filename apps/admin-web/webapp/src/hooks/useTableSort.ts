import type { SortState } from "@navikt/ds-react";
import sortBy from "lodash/sortBy";
import { useState } from "react";

import type { KafkaEvent } from "~/types.ts";

export function useTableSort<T>(
  tableData: T[],
  defaultOrderBy: string,
  direction?: "ascending" | "descending" | "none",
) {
  const [sort, setSort] = useState<SortState>({
    orderBy: defaultOrderBy,
    direction: direction ?? "ascending",
  });
  const handleSort = (sortKey: string | undefined) => {
    if (sortKey) {
      setSort({
        orderBy: sortKey,
        direction:
          sort && sortKey === sort.orderBy && sort.direction === "descending"
            ? "ascending"
            : "descending",
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

  let sortedData = [...tableData];

  if (orderBy === "dato" && isKafkaEventArray(tableData)) {
    sortedData = sortedData.sort((a, b) => {
      const kafkaA = a as KafkaEvent;
      const kafkaB = b as KafkaEvent;
      const dateA = kafkaA.metadata?.createdTimestamp
        ? new Date(kafkaA.metadata.createdTimestamp).getTime()
        : 0;
      const dateB = kafkaB.metadata?.createdTimestamp
        ? new Date(kafkaB.metadata.createdTimestamp).getTime()
        : 0;
      return dateA - dateB;
    });
  } else {
    // Default sorting for other fields
    sortedData = sortBy(tableData, orderBy);
  }

  const reversed = direction === "descending";

  return reversed ? sortedData.reverse() : sortedData;
}

function isKafkaEventArray(data: any[]): data is KafkaEvent[] {
  return (
    data.length === 0 ||
    (data[0] !== undefined &&
      "metadata" in data[0] &&
      data[0].metadata !== undefined)
  );
}
