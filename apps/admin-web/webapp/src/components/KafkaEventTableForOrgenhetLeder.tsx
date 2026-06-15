import { FilesIcon } from "@navikt/aksel-icons";
import { Button, HGrid, HStack, Spacer, Table, VStack } from "@navikt/ds-react";
import { Link } from "@tanstack/react-router";
import React from "react";
import { JsonView } from "react-json-view-lite";

import { JsonModal } from "~/components/JsonModal.tsx";
import { copyToClipboard } from "~/components/quickdialog.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import type { OrgEnhetLederEventWithNavidentDTO } from "~/types/nom-admin/nom-admin-models.ts";
import { formatDisplayDatetime } from "~/utils.ts";

export function KafkaEventTableForOrgenhetLeder(props: {
  kafkaEvents: OrgEnhetLederEventWithNavidentDTO[];
  linkPrefix: string;
}) {
  const kafkaEvents = props.kafkaEvents;
  const { data, sort, handleSort } = useTableSort(kafkaEvents ?? [], "dato");

  return (
    <div>
      <OrgenhetLederEventDateRange events={kafkaEvents} />
      <Table
        onSortChange={(sortKey) => handleSort(sortKey)}
        size="small"
        sort={sort}
      >
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.ColumnHeader scope="col">Fid</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">NAV-ident</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Endring</Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="dato" sortable>
              Dato
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Opprinnelse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {data.map((kafkaEvent, index) => {
            return (
              <Table.ExpandableRow
                content={<EmbeddedJsonView jsonData={kafkaEvent} />}
                key={index}
              >
                <Table.HeaderCell scope="row">
                  <Link
                    search={{ filterText: kafkaEvent.fid }}
                    to={props.linkPrefix}
                  >
                    {kafkaEvent.fid}
                  </Link>
                </Table.HeaderCell>
                <Table.DataCell>{kafkaEvent.navident}</Table.DataCell>
                <Table.DataCell>
                  <JsonModal
                    data={kafkaEvent}
                    text={
                      kafkaEvent.data?.type
                        ? kafkaEvent.data?.type.split("$")[0]
                        : ""
                    }
                  />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(kafkaEvent.metadata?.createdTimestamp)}
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(kafkaEvent.metadata?.createdTimestamp)}
                </Table.DataCell>
              </Table.ExpandableRow>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}

export function EmbeddedJsonView(props: { jsonData: object | any[] }) {
  const ref = React.useRef<HTMLDivElement | null>(null);
  const [renderStuff, setRenderStuff] = React.useState(false);
  const kafkaEvent = props.jsonData;

  return (
    <HGrid
      columns={"1fr min-content"}
      ref={ref}
      style={{ position: "relative" }}
    >
      {!renderStuff && (
        <Button onClick={() => setRenderStuff(true)} variant={"tertiary"}>
          Vis JSON
        </Button>
      )}
      {renderStuff && (
        <>
          <JsonView data={kafkaEvent} />
          <VStack justify={"start"}>
            <Button
              icon={<FilesIcon />}
              onClick={() => copyToClipboard(kafkaEvent)}
              size={"xsmall"}
              style={{
                position: "absolute",
                right: 0,
                top: 0,
              }}
              variant={"secondary"}
            >
              Kopier JSON
            </Button>
          </VStack>
        </>
      )}
    </HGrid>
  );
}

export function OrgenhetLederEventDateRange({
  events,
}: {
  events: OrgEnhetLederEventWithNavidentDTO[];
}) {
  const latestEvent = events[0];
  const firstEvent = events[events.length - 1];

  return (
    <HStack>
      <div>
        Viser {events.length} events i perioden{" "}
        {formatDisplayDatetime(firstEvent?.metadata?.createdTimestamp)} -{" "}
        {formatDisplayDatetime(latestEvent?.metadata?.createdTimestamp)}
      </div>
      <Spacer />
      <Button
        icon={<FilesIcon />}
        onClick={() => copyToClipboard(events)}
        size={"xsmall"}
        variant={"secondary"}
      >
        Kopier JSON[]
      </Button>
    </HStack>
  );
}
