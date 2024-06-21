import { Link as AkselLink } from "@navikt/ds-react";
import { createFileRoute, Link } from "@tanstack/react-router";
export const Route = createFileRoute("/")({
  component: () => {
    return (
      <div className="flex items-center gap-2 flex-col">
        <AkselLink as={Link} to="/sinks">
          Kafka meldinger
        </AkselLink>
        <AkselLink as={Link} to="/state">
          State
        </AkselLink>
      </div>
    );
  },
});
