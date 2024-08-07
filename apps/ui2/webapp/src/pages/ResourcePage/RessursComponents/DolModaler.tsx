import { Alert, BodyLong, HStack } from "@navikt/ds-react";
import { useQueries } from "@tanstack/react-query";
import sortBy from "lodash/sortBy";
import React from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { senesteTom } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { OrgEnhet, OrgtilknytningState } from "~/types/api-models";
import {
  formatDisplayDate,
  isSameDateOrAfter,
  isTodayOrBefore,
  todayWithoutTimezone,
} from "~/utils";

export function TomDatoModalInnhold({
  orgtilknytninger,
  navn,
}: {
  orgtilknytninger: Partial<OrgtilknytningState>[];
  navn: string;
}) {
  const senesteTomDato = senesteTom(orgtilknytninger);

  if (!senesteTomDato) {
    return null;
  }

  return isTodayOrBefore(new Date(senesteTomDato)) ? (
    <BekreftModalInnholdTomIFortiden navn={navn ?? "medarbeideren"} />
  ) : (
    <BekreftModalInnholdTomIFremtiden
      dato={formatDisplayDate(senesteTomDato)}
      navn={navn ?? "medarbeideren"}
    />
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
          som medarbeider i NAV, og alle tilganger for medarbeideren vil bli
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
            som medarbeider i NAV, og alle tilganger for medarbeideren vil bli
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

export function EndreRemedyEnhetModalInnhold({
  orgtilknytninger,
  navn,
  dagensRemedyEnhetId,
  dagensDolOrgEnhet,
}: {
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

  if (!!nyRemedyTilknytning && !!nyRemedyOrgEnhet) {
    return isTodayOrBefore(
      new Date(nyRemedyTilknytning.gyldigFom as string),
    ) ? (
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
    );
  } else {
    return <div>Er du sikker på at du vil lagre?</div>;
  }
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
