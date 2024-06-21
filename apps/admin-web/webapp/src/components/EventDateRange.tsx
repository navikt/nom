import type { KafkaEvent } from "~/types.ts";
import { formatDisplayDatetime } from "~/utils.ts";

export function EventDateRange({ events }: { events: KafkaEvent[] }) {
  const latestEvent = events[0];
  const firstEvent = events.at(-1);

  return (
    <div>
      Viser {events.length} events i perioden{" "}
      {formatDisplayDatetime(firstEvent?.metadata.createdTimestamp)} -{" "}
      {formatDisplayDatetime(latestEvent?.metadata.createdTimestamp)}
    </div>
  );
}
