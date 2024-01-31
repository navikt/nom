import { css } from "@emotion/react";
import { Button, Table } from "@navikt/ds-react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";

import {
  eventSourceForRessursKeys,
  getRessursEvents,
} from "~/api/react-query-endpoints";
import { JsonModal } from "~/pages/AdminPage/components/JsonModal";
import { formatDisplayDatetime, formatVisningsnavn } from "~/utils";

export function RessursHistoryPage() {
  const ressursEventsQuery = useInfiniteQuery({
    queryKey: eventSourceForRessursKeys.all,
    queryFn: getRessursEvents.queryFn,
    getNextPageParam: (lastPage, allPages, lastPageOffset) =>
      lastPageOffset + 50,
    initialPageParam: 50,
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const ressursEvents = (ressursEventsQuery.data?.pages ?? []).flat();
  const latestEvent = ressursEvents[0];
  const firstEvent = ressursEvents.at(-1);

  return (
    <div
      css={css`
        padding: var(--a-spacing-4);
      `}
    >
      <div>
        Viser {ressursEvents.length} events i perioden{" "}
        {formatDisplayDatetime(firstEvent?.metadata.createdTimestamp)} -{" "}
        {formatDisplayDatetime(latestEvent?.metadata.createdTimestamp)}
      </div>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navident</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Endring</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Dato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Opprinnelse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any -- events can be of any type */}
          {ressursEvents.map((ressursEvent: any, index: number) => {
            return (
              <Table.Row key={index}>
                <Table.DataCell>
                  <Link
                    params={{ id: ressursEvent.additionalData?.navident }}
                    to="/ressurs/$id"
                  >
                    {ressursEvent.additionalData?.navident}
                  </Link>
                </Table.DataCell>
                <Table.DataCell scope="row">
                  {formatVisningsnavn(ressursEvent.data?.visningsnavn)}
                </Table.DataCell>

                <Table.DataCell>
                  <JsonModal data={ressursEvent} text={ressursEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(
                    ressursEvent.metadata?.createdTimestamp,
                  )}
                </Table.DataCell>
                <Table.DataCell>
                  {ressursEvent.metadata.inputMetadata.inputInterfaceMethodName}{" "}
                  via {ressursEvent.metadata.inputMetadata.inputInterfaceName}
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
      <Button
        onClick={() => ressursEventsQuery.fetchNextPage()}
        variant="primary"
      >
        Se mer
      </Button>
    </div>
  );
}
