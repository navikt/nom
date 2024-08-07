import { css } from "@emotion/react";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { Alert, BodyLong, Button, HStack, Modal } from "@navikt/ds-react";
import { useQueries } from "@tanstack/react-query";
import sortBy from "lodash/sortBy";
import React, { useEffect } from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import type { OrgEnhet, OrgtilknytningState } from "~/types/api-models";
import {
  formatDisplayDate,
  isSameDateOrAfter,
  isTodayOrBefore,
  todayWithoutTimezone,
} from "~/utils";

export function RemedyEndringModal({
  onCancel,
  onConfirm,
  orgtilknytninger,
  navn,
  dagensRemedyEnhetId,
  dagensDolOrgEnhet,
}: {
  onCancel?: () => void;
  onConfirm?: () => void;
  orgtilknytninger: Partial<OrgtilknytningState>[];
  navn: string;
  dagensRemedyEnhetId: string | undefined;
  dagensDolOrgEnhet: OrgEnhet | undefined;
}) {
  const orgEnhetIdListe = [
    ...new Set(
      orgtilknytninger.flatMap(
        (orgtilknytning) => orgtilknytning.orgEnhetId || [],
      ),
    ),
  ];

  const orgEnheter = useQueries({
    queries: orgEnhetIdListe.map((id) => ({
      queryKey: getOrgEnhet.queryKey(id),
      queryFn: () => getOrgEnhet.queryFn(id),
    })),
    combine: (results) => results.flatMap((r) => r.data ?? []),
  });

  const relevanteDolOrgtilknytninger = sortBy(
    orgtilknytninger.filter((orgtilknytning) => {
      return (
        orgtilknytning.erDagligOppfolging &&
        (!orgtilknytning.gyldigTom ||
          isSameDateOrAfter(
            new Date(orgtilknytning.gyldigTom),
            todayWithoutTimezone(),
          ))
      );
    }),
    "gyldigFom",
  );

  const nyRemedyTilknytning = relevanteDolOrgtilknytninger.find(
    (tilknytning) => {
      return (
        orgEnheter.find((orgEnhet) => orgEnhet.id === tilknytning.orgEnhetId)
          ?.remedyEnhetId !== dagensRemedyEnhetId
      );
    },
  );

  const nyRemedyOrgEnhet = orgEnheter.find(
    (orgEnhet) => orgEnhet.id === nyRemedyTilknytning?.orgEnhetId,
  );

  useEffect(() => {
    if ((!nyRemedyTilknytning || !nyRemedyOrgEnhet) && onConfirm) {
      onConfirm();
    }
  }, [nyRemedyTilknytning, nyRemedyOrgEnhet, onConfirm]);

  if (!nyRemedyTilknytning || !nyRemedyOrgEnhet) {
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
        {isTodayOrBefore(new Date(nyRemedyTilknytning.gyldigFom as string)) ? (
          <BekreftModalInnholdNyRemedyIFortiden
            gammelOrgEnhet={`${dagensDolOrgEnhet?.navn} (${dagensDolOrgEnhet?.remedyEnhetId})`}
            navn={navn ?? "medarbeideren"}
            nyOrgEnhet={`${nyRemedyOrgEnhet?.navn} (${nyRemedyOrgEnhet?.remedyEnhetId})`}
          />
        ) : (
          <BekreftModalInnholdNyRemedyIFremtiden
            dato={formatDisplayDate(nyRemedyTilknytning.gyldigFom)}
            gammelOrgEnhet={`${dagensDolOrgEnhet?.navn} (${dagensDolOrgEnhet?.remedyEnhetId})`}
            navn={navn ?? "medarbeideren"}
            nyOrgEnhet={`${nyRemedyOrgEnhet?.navn} (${nyRemedyOrgEnhet?.remedyEnhetId})`}
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

function BekreftModalInnholdNyRemedyIFremtiden({
  navn,
  dato,
  gammelOrgEnhet,
  nyOrgEnhet,
}: {
  navn: string;
  dato: string;
  gammelOrgEnhet: string;
  nyOrgEnhet: string;
}) {
  return (
    <HStack gap="6">
      <BodyLong>
        Du registrerer nå at medarbeideren fra {dato} bytter
        organisasjonstilknytning fra <b>{gammelOrgEnhet}</b> til{" "}
        <b>{nyOrgEnhet}</b>.
      </BodyLong>
      <BodyLong>
        <b>
          Dette innebærer en endring i innstillinger for tilganger
          (4-siffer-enhet), slik at fra og med {dato} vil alle tilganger for{" "}
          {navn} fjernes. Nye tilganger må tildeles i Identrutina og/eller Mine
          tilganger.
        </b>
      </BodyLong>
      <BodyLong>
        Ta kontakt med identadministrator eller leder hvis du er usikker på om
        noe må gjøres.
      </BodyLong>
    </HStack>
  );
}

function BekreftModalInnholdNyRemedyIFortiden({
  navn,
  gammelOrgEnhet,
  nyOrgEnhet,
}: {
  navn: string;
  gammelOrgEnhet: string;
  nyOrgEnhet: string;
}) {
  return (
    <Alert variant="warning">
      <HStack gap="6">
        <BodyLong>
          Du registrerer nå at medarbeideren på dagens dato eller tidligere
          bytter organisasjonstilknytning fra <b>{gammelOrgEnhet}</b> til{" "}
          <b>{nyOrgEnhet}</b>.
        </BodyLong>
        <BodyLong>
          <b>
            Dette innebærer en endring i innstillinger for tilganger
            (4-siffer-enhet). Vær oppmerksom på at dette betyr at fra i dag vil
            alle tilganger for {navn} fjernes. Nye tilganger må tildeles i
            Identrutina og/eller Mine tilganger.
          </b>
        </BodyLong>
        <BodyLong>
          Ta kontakt med identadministrator eller leder hvis du er usikker på om
          noe må gjøres.
        </BodyLong>
      </HStack>
    </Alert>
  );
}
