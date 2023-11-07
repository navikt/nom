import { css } from "@emotion/react";
import { Button, Table } from "@navikt/ds-react";
import { useInfiniteQuery } from "@tanstack/react-query";

import {
  eventSourceForOrgEnhetKeys,
  getOrgEnhetEvents,
} from "~/api/react-query-endpoints";
import { JsonModal } from "~/pages/AdminPage/components/JsonModal";
import { formatDisplayDatetime } from "~/utils";

export function OrgEnhetHistoryPage() {
  const orgEnhetEventsQuery = useInfiniteQuery({
    queryKey: eventSourceForOrgEnhetKeys.all,
    queryFn: getOrgEnhetEvents.queryFn,
    getNextPageParam: (lastPage, allPages, lastPageOffset) =>
      lastPageOffset + 50,
    initialPageParam: 50,
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const orgEnhetEvents = (orgEnhetEventsQuery.data?.pages ?? []).flat();
  const latestEvent = orgEnhetEvents[0];
  const firstEvent = orgEnhetEvents.at(-1);

  return (
    <div
      css={css`
        padding: var(--a-spacing-4);
      `}
    >
      <div>
        Viser {orgEnhetEvents.length} events i perioden{" "}
        {formatDisplayDatetime(firstEvent?.metadata.createdTimestamp)} -{" "}
        {formatDisplayDatetime(latestEvent?.metadata.createdTimestamp)}
      </div>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Endring</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Dato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Opprinnelse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any -- events can be of any type */}
          {orgEnhetEvents.map((orgEnhetEvent: any, index: number) => {
            return (
              <Table.Row key={index}>
                <Table.DataCell>
                  <JsonModal data={orgEnhetEvent} text={orgEnhetEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(
                    orgEnhetEvent.metadata?.createdTimestamp,
                  )}
                </Table.DataCell>
                <Table.DataCell>
                  {
                    orgEnhetEvent.metadata.inputMetadata
                      .inputInterfaceMethodName
                  }{" "}
                  via {orgEnhetEvent.metadata.inputMetadata.inputInterfaceName}
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
      <Button
        onClick={() => orgEnhetEventsQuery.fetchNextPage()}
        variant="primary"
      >
        Se mer
      </Button>
    </div>
  );
}
