import { css } from "@emotion/react";
import {
  ExclamationmarkTriangleFillIcon,
  FloppydiskIcon,
  InformationSquareFillIcon,
} from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  Button,
  HStack,
  Modal,
  ReadMore,
} from "@navikt/ds-react";
import { useQueries } from "@tanstack/react-query";
import { addDays } from "date-fns";
import sortBy from "lodash/sortBy";
import type { ReactNode } from "react";
import { useEffect } from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { senesteTom } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { OrgEnhet, OrgtilknytningState } from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import {
  dateWithoutTimezone,
  formatDisplayDate,
  isSameDateOrAfter,
  isTodayOrAfter,
  isTodayOrBefore,
  todayWithoutTimezone,
} from "~/utils";

export function EndreOtBekreftelsesmodal({
  onCancel,
  onConfirm,
  orgtilknytninger,
  navn,
  dagensTilgangsenhetId,
  dagensDolOrgEnhet,
  eksisterendeStatligeAgressoIder,
}: {
  onCancel?: () => void;
  onConfirm?: () => void;
  orgtilknytninger: Partial<OrgtilknytningState>[];
  navn: string;
  dagensTilgangsenhetId: string | undefined;
  dagensDolOrgEnhet: OrgEnhet | undefined;
  eksisterendeStatligeAgressoIder: string[];
}) {
  const senesteTomDato = senesteTom(orgtilknytninger);

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
            dateWithoutTimezone(orgtilknytning.gyldigTom),
            todayWithoutTimezone(),
          ))
      );
    }),
    "gyldigFom",
  );

  const nyTilgangsenhetTilknytning = relevanteDolOrgtilknytninger.find(
    (tilknytning) => {
      return (
        orgEnheter.find((orgEnhet) => orgEnhet.id === tilknytning.orgEnhetId)
          ?.tilgangsenhetId !== dagensTilgangsenhetId
      );
    },
  );

  const nyTilgangsenhetOrgEnhet = orgEnheter.find(
    (orgEnhet) => orgEnhet.id === nyTilgangsenhetTilknytning?.orgEnhetId,
  );

  const oppdaterteStatligeOrgEnhetIder = orgtilknytninger
    .filter(
      (tilknytning) =>
        tilknytning.sektor === Sektor.NAV_STATLIG &&
        (!tilknytning.gyldigTom || isTodayOrAfter(tilknytning.gyldigTom)),
    )
    .map((tilknytning) => tilknytning.orgEnhetId);

  const oppdaterteStatligeOrgEnheter = oppdaterteStatligeOrgEnhetIder.map(
    (id) => orgEnheter.find((orgEnhet) => orgEnhet.id === id),
  );

  const oppdaterteStatligeAgressoIder = oppdaterteStatligeOrgEnheter.map(
    (orgEnhet) => orgEnhet?.agressoId ?? "",
  );

  const eksisterendeSet = new Set(oppdaterteStatligeAgressoIder);
  const oppdaterteSet = new Set(eksisterendeStatligeAgressoIder);

  const endringAgressoIder =
    eksisterendeSet.size !== oppdaterteSet.size ||
    [...eksisterendeSet].some((id) => !oppdaterteSet.has(id));

  const isTomDatoTodayOrBefore = senesteTomDato
    ? isTodayOrBefore(dateWithoutTimezone(senesteTomDato))
    : false;

  const isNyTilgangsenhetTilknytningTodayOrBefore =
    nyTilgangsenhetTilknytning?.gyldigFom
      ? isTodayOrBefore(
          dateWithoutTimezone(nyTilgangsenhetTilknytning.gyldigFom),
        )
      : false;

  useEffect(() => {
    if (
      !senesteTomDato &&
      (!nyTilgangsenhetTilknytning || !nyTilgangsenhetOrgEnhet) &&
      !endringAgressoIder &&
      onConfirm
    ) {
      onConfirm();
    }
  }, [
    senesteTomDato,
    nyTilgangsenhetTilknytning,
    nyTilgangsenhetOrgEnhet,
    onConfirm,
    !endringAgressoIder,
  ]);

  if (
    !senesteTomDato &&
    (!nyTilgangsenhetTilknytning || !nyTilgangsenhetOrgEnhet) &&
    !endringAgressoIder &&
    onConfirm
  ) {
    return;
  }

  const tomDatoMessage = senesteTomDato ? (
    isTomDatoTodayOrBefore ? (
      <Alert variant="warning">
        <HStack gap="6">
          <BodyLong>
            Du har nå satt siste sluttdato på medarbeiderens
            organisasjonstilknytning(er) til dagens dato eller tidligere, og det
            er ikke registrert noen ny tilknytning etter denne datoen.
          </BodyLong>
          <BodyLong>
            <b>
              Dette innebærer at fra i dag vil {navn} ikke lenger være
              registrert som medarbeider i Nav, og alle tilganger for
              medarbeideren vil bli fjernet.
            </b>
          </BodyLong>
          <BodyLong>
            Hvis dette ikke er korrekt, må du trykke på <b>Avbryt</b> og legge
            til en ny tilknytning.
          </BodyLong>
          <BodyLong>
            Hvis dette er korrekt, kan du bekrefte endringen ved å trykke på{" "}
            <b>Bekreft endring</b>.
          </BodyLong>
        </HStack>
      </Alert>
    ) : (
      <HStack gap="6">
        <BodyLong>
          Du har nå satt siste sluttdato på medarbeiderens
          organisasjonstilknytning(er) til {formatDisplayDate(senesteTomDato)},
          og det er ikke registrert noen ny tilknytning etter denne datoen.
        </BodyLong>
        <BodyLong>
          <b>
            Dette innebærer at fra og med{" "}
            {formatDisplayDate(addDays(senesteTomDato, 1))} vil {navn} ikke
            lenger være registrert som medarbeider i Nav, og alle tilganger for
            medarbeideren vil bli fjernet.
          </b>
        </BodyLong>
        <BodyLong>
          Hvis dette ikke er korrekt, må det legges inn en ny tilknytning. Du
          kan trykke på <b>Avbryt</b> og gjøre dette nå, eller så kan du eller
          noen andre gjøre det senere, men senest sluttdatoen{" "}
          {formatDisplayDate(senesteTomDato)}.
        </BodyLong>
        <BodyLong>
          Hvis dette er korrekt, kan du bekrefte endringen ved å trykke på{" "}
          <b>Bekreft endring</b>.
        </BodyLong>
      </HStack>
    )
  ) : null;

  const tilgangsenhetMessage =
    nyTilgangsenhetTilknytning &&
    nyTilgangsenhetOrgEnhet &&
    dagensTilgangsenhetId ? (
      isNyTilgangsenhetTilknytningTodayOrBefore ? (
        <Alert variant="warning">
          <HStack gap="6">
            <BodyLong>
              Du registrerer nå at medarbeideren på dagens dato eller tidligere
              bytter organisasjonstilknytning fra{" "}
              <b>
                {dagensDolOrgEnhet
                  ? `${dagensDolOrgEnhet?.navn} (${dagensDolOrgEnhet?.tilgangsenhetId})`
                  : "ukjent enhet"}
              </b>{" "}
              til{" "}
              <b>
                {nyTilgangsenhetOrgEnhet?.navn} (
                {nyTilgangsenhetOrgEnhet?.tilgangsenhetId})
              </b>
              .
            </BodyLong>
            <BodyLong>
              <b>
                Dette innebærer en endring i innstillinger for tilganger
                (4-siffer-enhet). Vær oppmerksom på at dette betyr at fra i dag
                vil alle tilganger for {navn} fjernes. Nye tilganger må tildeles
                i Identrutina og/eller Mine tilganger.
              </b>
            </BodyLong>
            <BodyLong>
              Ta kontakt med identadministrator eller leder hvis du er usikker
              på om noe må gjøres.
            </BodyLong>
          </HStack>
        </Alert>
      ) : (
        <HStack gap="6">
          <BodyLong>
            Du registrerer nå at medarbeideren fra{" "}
            {formatDisplayDate(nyTilgangsenhetTilknytning.gyldigFom)} bytter
            organisasjonstilknytning fra{" "}
            <b>
              {dagensDolOrgEnhet
                ? `${dagensDolOrgEnhet?.navn} (${dagensDolOrgEnhet?.tilgangsenhetId})`
                : "ukjent enhet"}
            </b>{" "}
            til{" "}
            <b>
              {nyTilgangsenhetOrgEnhet?.navn} (
              {nyTilgangsenhetOrgEnhet?.tilgangsenhetId})
            </b>
            .
          </BodyLong>
          <BodyLong>
            <b>
              Dette innebærer en endring i innstillinger for tilganger
              (4-siffer-enhet), slik at fra og med{" "}
              {formatDisplayDate(nyTilgangsenhetTilknytning.gyldigFom)} vil alle
              tilganger for {navn} fjernes. Nye tilganger må tildeles i
              Identrutina og/eller Mine tilganger.
            </b>
          </BodyLong>
          <BodyLong>
            Ta kontakt med identadministrator eller leder hvis du er usikker på
            om noe må gjøres.
          </BodyLong>
        </HStack>
      )
    ) : null;

  const endringAgressoMessage = endringAgressoIder ? (
    <BodyLong>
      Medarbeider får nå en ny organisasjonstilknytning på en ny
      organisasjonsenhet. Denne endringen må også meldes til Nav Økonomitjeneste
      ved hjelp av lønnsskjema i Unit4.
    </BodyLong>
  ) : null;

  return (
    <Modal
      css={css`
        padding: var(--a-spacing-6);
        border-radius: 0;
      `}
      header={{ heading: `Bekreft endringer for ${navn}`, closeButton: false }}
      onClose={onCancel}
      open
      width="548px"
    >
      <Modal.Body>
        <div
          css={css`
            display: grid;
            gap: 1px;
            background-color: var(--a-border-divider);
            padding-top: 1px;
            padding-bottom: 1px;
          `}
        >
          {tilgangsenhetMessage && (
            <ModalReadMore
              content={tilgangsenhetMessage}
              header={
                isNyTilgangsenhetTilknytningTodayOrBefore ||
                !nyTilgangsenhetTilknytning
                  ? "Tilganger fjernes fra i dag"
                  : `Tilganger fjernes fra ${
                      nyTilgangsenhetTilknytning
                        ? formatDisplayDate(
                            nyTilgangsenhetTilknytning.gyldigFom,
                          )
                        : "angitt dato"
                    }`
              }
              icon={
                isNyTilgangsenhetTilknytningTodayOrBefore ||
                !nyTilgangsenhetTilknytning
                  ? "warning"
                  : "info"
              }
            />
          )}

          {tomDatoMessage && (
            <ModalReadMore
              content={tomDatoMessage}
              header={
                isTomDatoTodayOrBefore
                  ? "Fra i dag vil medarbeideren ikke lenger være registrert som ansatt i Nav"
                  : `Fra ${
                      senesteTomDato
                        ? formatDisplayDate(addDays(senesteTomDato, 1))
                        : "den valgte datoen"
                    } vil medarbeideren ikke lenger være registrert som ansatt i Nav`
              }
              icon={isTomDatoTodayOrBefore ? "warning" : "info"}
            />
          )}

          {endringAgressoMessage && (
            <ModalReadMore
              content={endringAgressoMessage}
              header="Må også registreres i Unit4"
              icon="info"
            />
          )}
        </div>
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

function ModalReadMore({
  header,
  icon,
  content,
}: {
  header: string;
  icon: "warning" | "info";
  content: ReactNode;
}) {
  return (
    <ReadMore
      css={css`
        width: 100%;
        background: white;

        .navds-read-more__button {
          width: 100%;
          color: #000;
          display: flex;
          align-items: center;
          background: #fff;
          font-size: var(--a-font-size-heading-small);

          :hover {
            background: var(--a-border-divider);
          }
        }

        span {
          width: 100%;
        }
      `}
      header={
        <div
          css={css`
            display: flex;
            justify-content: space-between;
            align-items: center;
          `}
        >
          <span>{header}</span>

          {icon === "warning" ? (
            <ExclamationmarkTriangleFillIcon
              css={css`
                color: var(--a-icon-warning);
                float: right;
              `}
              fontSize="2rem"
              title="a11y-title"
            />
          ) : (
            <InformationSquareFillIcon
              css={css`
                color: var(--nom-primary);
              `}
              fontSize="2rem"
              title="a11y-title"
            />
          )}
        </div>
      }
    >
      <div
        css={css`
          margin-bottom: var(--a-spacing-3);
        `}
      >
        {content}
      </div>
    </ReadMore>
  );
}
