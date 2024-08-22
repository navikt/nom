import { Heading } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";

import { AvvikleOrgenhet } from "~/manuelle-meldinger/AvvikleOrgenhet.tsx";
import { AvvikleRessurs } from "~/manuelle-meldinger/AvvikleRessurs.tsx";
import { EndreDagligOppfolging } from "~/manuelle-meldinger/EndreDagligOppfolging.tsx";
import { FjernOrgtilknytning } from "~/manuelle-meldinger/FjernOrgtilknytning.tsx";
import { OppdaterPersonalia } from "~/manuelle-meldinger/OppdaterPersonalia.tsx";
import { OppdaterPersonidentRessurs } from "~/manuelle-meldinger/OppdaterPersonidentRessurs.tsx";
import { SettNomMaster } from "~/manuelle-meldinger/SettNomMaster.tsx";
import { SlettRessursMelding } from "~/manuelle-meldinger/SlettRessursMelding.tsx";

export const Route = createFileRoute("/manuelle-meldinger")({
  component: ManuelleMeldingerPage,
});

function ManuelleMeldingerPage() {
  return (
    <div>
      <Heading level="2" size="medium" spacing>
        Ressurs
      </Heading>
      <div className="flex gap-2 flex-wrap mb-4">
        <SlettRessursMelding />
        <OppdaterPersonidentRessurs />
        <OppdaterPersonalia />
        <SettNomMaster />
        <AvvikleRessurs />
      </div>
      <Heading level="2" size="medium" spacing>
        Orgenhet
      </Heading>
      <div className="flex gap-2 flex-wrap mb-4">
        <AvvikleOrgenhet />
        <EndreDagligOppfolging />
      </div>
      <Heading level="2" size="medium" spacing>
        Orgtilknytning
      </Heading>
      <div className="flex gap-2 flex-wrap mb-4">
        <FjernOrgtilknytning />
      </div>
    </div>
  );
}
