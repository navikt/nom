import { FilesIcon } from "@navikt/aksel-icons";
import { Button, HStack, Spacer } from "@navikt/ds-react";

import { copyToClipboard } from "~/components/quickdialog.tsx";
import type { KafkaEvent } from "~/types.ts";
import { formatDisplayDatetime } from "~/utils.ts";

export function EventDateRange({ events }: { events: KafkaEvent[] }) {
  const latestEvent = events[0];
  const firstEvent = events[events.length - 1];

  return (
    <HStack>
      <div>
        Viser {events.length} events i perioden{" "}
        {formatDisplayDatetime(firstEvent?.metadata.createdTimestamp)} -{" "}
        {formatDisplayDatetime(latestEvent?.metadata.createdTimestamp)}
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
