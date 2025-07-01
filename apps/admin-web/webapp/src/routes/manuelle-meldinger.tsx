import { Heading } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";

import { AvsluttDelegering } from "~/manuelle-meldinger/AvsluttDelegering.tsx";
import { AvvikleOrgenhet } from "~/manuelle-meldinger/AvvikleOrgenhet.tsx";
import { AvvikleRessurs } from "~/manuelle-meldinger/AvvikleRessurs.tsx";
import { EndreDagligOppfolging } from "~/manuelle-meldinger/EndreDagligOppfolging.tsx";
import { EndreOrgtilknytningSluttdato } from "~/manuelle-meldinger/EndreOrgtilknytningSluttdato.tsx";
import { FjernOrgtilknytning } from "~/manuelle-meldinger/FjernOrgtilknytning.tsx";
import { FornyDelegering } from "~/manuelle-meldinger/FornyDelegering.tsx";
import { OppdatereRessursSektor } from "~/manuelle-meldinger/OppdatereRessursSektor.tsx";
import { OppdatereSluttDatoRessurs } from "~/manuelle-meldinger/OppdatereSluttDatoRessurs.tsx";
import { OppdatereStartdato } from "~/manuelle-meldinger/OppdatereStartdato.tsx";
import { OppdaterPersonalia } from "~/manuelle-meldinger/OppdaterPersonalia.tsx";
import { OppdaterPersonidentRessurs } from "~/manuelle-meldinger/OppdaterPersonidentRessurs.tsx";
import { OpprettDelegering } from "~/manuelle-meldinger/OpprettDelegering.tsx";
import { SettNomMaster } from "~/manuelle-meldinger/SettNomMaster.tsx";
import { SjekkRessursStateRemedy } from "~/manuelle-meldinger/SjekkRessursStateRemedy.tsx";
import { SlettAlleErrorStates } from "~/manuelle-meldinger/SlettAlleErrorStates.tsx";
import { SlettEnErrorStateRad } from "~/manuelle-meldinger/SlettEnErrorStateRad.tsx";
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
        <OppdatereStartdato />
        <OppdatereSluttDatoRessurs />
        <OppdatereRessursSektor />
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
        <EndreOrgtilknytningSluttdato />
      </div>

      <Heading level="2" size="medium" spacing>
        Remedy Admin Controller
      </Heading>
      <div className="flex gap-2 flex-wrap mb-4">
        <SjekkRessursStateRemedy />
        <SlettAlleErrorStates />
        <SlettEnErrorStateRad />
      </div>

      <Heading level="2" size="medium" spacing>
        Delegering
      </Heading>
      <div className="flex gap-2 flex-wrap mb-4">
        <FornyDelegering />
        <OpprettDelegering />
        <AvsluttDelegering />
      </div>
    </div>
  );
}
