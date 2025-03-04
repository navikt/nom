import { FilesIcon } from "@navikt/aksel-icons";
import { Button, HGrid, Table, VStack } from "@navikt/ds-react";
import { Link } from "@tanstack/react-router";
import { JsonView } from "react-json-view-lite";

import { EventDateRange } from "~/components/EventDateRange.tsx";
import { EventOrigination } from "~/components/EventOrigination.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { copyToClipboard } from "~/components/quickdialog.tsx";
import type { KafkaEvent } from "~/types.ts";
import { formatDisplayDatetime } from "~/utils.ts";

export function KafkaEventTable(props: {
  kafkaEvents: KafkaEvent[];
  linkPrefix: string;
}) {
  const kafkaEvents = props.kafkaEvents;
  return (
    <div>
      <EventDateRange events={kafkaEvents} />
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.ColumnHeader scope="col">Fid</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Endring</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Dato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Opprinnelse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {kafkaEvents.map((kafkaEvent, index) => {
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
                <Table.DataCell>
                  <JsonModal data={kafkaEvent} text={kafkaEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(kafkaEvent.metadata?.createdTimestamp)}
                </Table.DataCell>
                <Table.DataCell>
                  <EventOrigination metadata={kafkaEvent.metadata} />
                </Table.DataCell>
              </Table.ExpandableRow>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function EmbeddedJsonView(props: { jsonData: object | any[] }) {
  const kafkaEvent = props.jsonData;
  return (
    <HGrid columns={"1fr min-content"} style={{ position: "relative" }}>
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
    </HGrid>
  );
}
