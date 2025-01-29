import { css } from "@emotion/react";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { Alert, BodyLong, Button, HStack, Modal } from "@navikt/ds-react";
import React, { useEffect } from "react";

import { senesteTom } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { OrgtilknytningState } from "~/types/api-models";
import { formatDisplayDate, isTodayOrBefore } from "~/utils";

export function TomDatoModal({
  onCancel,
  onConfirm,
  orgtilknytninger,
  navn,
}: {
  onCancel?: () => void;
  onConfirm?: () => void;
  orgtilknytninger: Partial<OrgtilknytningState>[];
  navn: string;
}) {
  const senesteTomDato = senesteTom(orgtilknytninger);

  useEffect(() => {
    if (!senesteTomDato && onConfirm) {
      onConfirm();
    }
  }, [senesteTomDato, onConfirm]);

  if (!senesteTomDato) {
    return;
  }

  return (
    <Modal
      css={css`
        padding: var(--a-spacing-6);
        border-radius: 0;
      `}
      header={{ heading: "Bekreft endringer", closeButton: false }}
      onClose={onCancel}
      open
      width="548px"
    >
      <Modal.Body>
        {isTodayOrBefore(new Date(senesteTomDato)) ? (
          <BekreftModalInnholdTomIFortiden navn={navn ?? "medarbeideren"} />
        ) : (
          <BekreftModalInnholdTomIFremtiden
            dato={formatDisplayDate(senesteTomDato)}
            navn={navn ?? "medarbeideren"}
          />
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button
          icon={<FloppydiskIcon aria-hidden />}
          onClick={onConfirm}
          size="small"
        >
          Bekreft endring
        </Button>
        <Button onClick={onCancel} size="small" variant="secondary">
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

function BekreftModalInnholdTomIFremtiden({
  navn,
  dato,
}: {
  navn: string;
  dato: string;
}) {
  return (
    <HStack gap="6">
      <BodyLong>
        Du har nå satt siste sluttdato på medarbeiderens
        organisasjonstilknytning(er) til {dato}, og det er ikke registrert noen
        ny tilknytning etter denne datoen.
      </BodyLong>
      <BodyLong>
        <b>
          Dette innebærer at etter {dato} vil {navn} ikke lenger være registrert
          som medarbeider i Nav, og alle tilganger for medarbeideren vil bli
          fjernet.
        </b>
      </BodyLong>
      <BodyLong>
        Hvis dette ikke er korrekt, må det legges inn en ny tilknytning. Du kan
        trykke på <b>Avbryt</b> og gjøre dette nå, eller så kan du eller noen
        andre gjøre det senere, men senest sluttdatoen {dato}.
      </BodyLong>
      <BodyLong>
        Hvis dette er korrekt, kan du bekrefte endringen ved å trykke på{" "}
        <b>Bekreft endring</b>.
      </BodyLong>
    </HStack>
  );
}

function BekreftModalInnholdTomIFortiden({ navn }: { navn: string }) {
  return (
    <Alert variant="warning">
      <HStack gap="6">
        <BodyLong>
          Du har nå satt siste sluttdato på medarbeiderens
          organisasjonstilknytning(er) til dagens dato eller tidligere, og det
          er ikke registrert noen ny tilknytning etter denne datoen.
        </BodyLong>
        <BodyLong>
          <b>
            Dette innebærer at fra i dag vil {navn} ikke lenger være registrert
            som medarbeider i Nav, og alle tilganger for medarbeideren vil bli
            fjernet.
          </b>
        </BodyLong>
        <BodyLong>
          Hvis dette ikke er korrekt, må du trykke på <b>Avbryt</b> og legge til
          en ny tilknytning.
        </BodyLong>
        <BodyLong>
          Hvis dette er korrekt, kan du bekrefte endringen ved å trykke på{" "}
          <b>Bekreft endring</b>.
        </BodyLong>
      </HStack>
    </Alert>
  );
}
