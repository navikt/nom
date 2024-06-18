import { Link as AkselLink } from "@navikt/ds-react";
import { createFileRoute, Link } from "@tanstack/react-router";
export const Route = createFileRoute("/")({
  component: () => {
    return (
      <div className="flex justify-center">
        <AkselLink as={Link} to="/sinks">
          Kafka meldinger
        </AkselLink>
      </div>
    );
  },
});
