import { css } from "@emotion/react";
import { PencilIcon, PersonIcon } from "@navikt/aksel-icons";
import {
  Button,
  Heading,
  HelpText,
  HStack,
  Link,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink, useNavigate } from "@tanstack/react-router";
import capitalize from "lodash/capitalize";
import uniqBy from "lodash/uniqBy";
import React from "react";

import {
  getLedereForRessurs,
  harLederTilgangForNavidentOptions,
} from "~/api/react-query-endpoints";
import {
  useGetSession,
  useHasAdminAccess,
  useHasWriteAccess,
} from "~/api/useGetSession";
import { LabelWithValue } from "~/components/LabelWithValue";
import { UserImage } from "~/components/UserImage";
import { useFysiskBrukersted } from "~/hooks/useFysiskBrukersted";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import type { Ressurs } from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import { formatDisplayDate, formatTlf, formatVisningsnavn } from "~/utils";

// Det er kanskje litt unødvendig å ha en wrapper komponent rundt hver detalj (navident, sluttdato, etc)
// men det kommer stadig endringer i reglene for logikken rundt når noe skal vises og ikke.
// Og som endrer seg etterhvert som vi støtter flere sektorer.
// Derfor greit å samle alle disse på et sted slik at man f.eks ikke risikere å vise SluttDato i ett bilde,
// men ikke i et annet fordi man glemte ordne logikk begge steder.

export function PersonOpplysninger({ ressurs }: { ressurs: Ressurs }) {
  return (
    <HStack>
      <VStack
        css={css`
          flex: 1;
        `}
      >
        <Heading level="1" size="large">
          {formatVisningsnavn(ressurs.visningsnavn)}
        </Heading>
        <KortFortalt />
        <div
          css={css`
            display: grid;
            grid-template-columns: max-content 1fr;
            gap: var(--a-spacing-2) var(--a-spacing-4);
          `}
        >
          <Navident ressurs={ressurs} />
          <SektorView ressurs={ressurs} />
          <StartDato ressurs={ressurs} />
          <SluttDato ressurs={ressurs} />
          <Epost ressurs={ressurs} />
          <DagligLeder ressurs={ressurs} />
          {ressurs.primaryFysiskBrukersted && (
            <FysiskBrukersted ressurs={ressurs} />
          )}
          <TelefonNummere ressurs={ressurs} />
        </div>
      </VStack>
      <VStack align="end" justify="space-between">
        <UserImage
          includeInstructionsWhenNotFound
          navident={ressurs.navident}
          navn={formatVisningsnavn(ressurs.visningsnavn)}
          size="126px"
        />
        <EditButton ressurs={ressurs} />
      </VStack>
    </HStack>
  );
}

function EditButton({ ressurs }: { ressurs: Ressurs }) {
  const navigate = useNavigate();

  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const hasWriteAccessForRessurs =
    useQuery(harLederTilgangForNavidentOptions(ressurs.navident)).data ?? false;

  const hasWriteAccessForSelvbetjening =
    useGetSession().data?.navident === ressurs.navident;

  const ressursCanBeEdited = ressurs.sektor !== Sektor.NAV_STATLIG;

  if (
    hasAdminAccess ||
    (hasWriteAccess && hasWriteAccessForRessurs) ||
    hasWriteAccessForSelvbetjening
  ) {
    return (
      <HStack align="center" gap="2">
        {!ressursCanBeEdited && (
          <HelpText title="Hvorfor er dette utilgjengelig?">
            Statlige ansatte kan ikke opprettes eller endres i NOM på dette
            tidspunktet. Disse kan endres i tråd med gjeldende rutiner i
            Identrutina og Unit4.
          </HelpText>
        )}

        <Button
          data-cy="endre-personopplysninger-button"
          disabled={!ressursCanBeEdited}
          icon={<PencilIcon aria-hidden />}
          onClick={() =>
            navigate({
              search: { editPersonopplysninger: true },
              params: (previous) => previous,
            })
          }
          size="small"
          variant="secondary"
        >
          Endre personopplysninger
        </Button>
      </HStack>
    );
  }

  return;
}

export function KortFortalt() {
  return (
    <Heading
      css={css`
        margin-top: var(--a-spacing-8);
        margin-bottom: var(--a-spacing-6);
      `}
      level="2"
      size="small"
    >
      <PersonIcon />
      <span>Kort fortalt</span>
    </Heading>
  );
}

export function Navident({ ressurs }: { ressurs: Ressurs }) {
  return <LabelWithValue label="NAV-ident" value={ressurs.navident} />;
}

export function StartDato({ ressurs }: { ressurs: Ressurs }) {
  return (
    <LabelWithValue
      helpText="Første arbeidsdag medarbeideren jobbet for NAV. Datoen kan være fra tidligere arbeidsforhold i NAV. (Historiske startdatoer fra noen år tilbake kan avvike noe fra den faktiske første arbeidsdagen. Datoen vil da vanligvis representere dagen NAV-identen ble opprettet.) "
      label="Startdato"
      value={formatDisplayDate(ressurs.startdato)}
    />
  );
}

export function SluttDato({ ressurs }: { ressurs: Ressurs }) {
  const hasWriteAccessForRessurs =
    useQuery(harLederTilgangForNavidentOptions(ressurs.navident)).data ?? false;

  if (
    ressurs.sluttdato &&
    (hasWriteAccessForRessurs || ressurs.sektor === "EKSTERN")
  ) {
    return (
      <LabelWithValue
        helpText="Medarbeiderens siste arbeidsdag i NAV."
        label="Sluttdato"
        value={formatDisplayDate(ressurs.sluttdato)}
      />
    );
  }
  return;
}

export function SektorView({ ressurs }: { ressurs: Ressurs }) {
  return (
    <LabelWithValue label="Sektor" value={SektorNameMapping[ressurs.sektor]} />
  );
}

export function Epost({ ressurs }: { ressurs: Ressurs }) {
  return <LabelWithValue label="E-post" value={ressurs.epost} />;
}

export function FysiskBrukersted({ ressurs }: { ressurs: Ressurs }) {
  const fysiskBrukersted = useFysiskBrukersted(ressurs.primaryFysiskBrukersted);
  return (
    <LabelWithValue
      label="Fysisk brukersted"
      value={
        fysiskBrukersted ? capitalize(fysiskBrukersted.brukerstedNavn) : "-"
      }
    />
  );
}

export function TelefonNummere({ ressurs }: { ressurs: Ressurs }) {
  const telefonnummere = ressurs.primaryTelefonnummer
    ? [{ nummer: ressurs.primaryTelefonnummer }, ...ressurs.telefoner]
    : ressurs.telefoner;

  const uniqueTelefonnummere = uniqBy(telefonnummere, "nummer");

  return (
    <LabelWithValue
      label="Telefon"
      value={uniqueTelefonnummere
        .map(({ nummer }) => formatTlf(nummer))
        .join(", ")}
    />
  );
}

export function DagligLeder({ ressurs }: { ressurs: Ressurs }) {
  const dagligLeder = useQuery({
    queryKey: getLedereForRessurs.queryKey(ressurs.navident),
    queryFn: () => getLedereForRessurs.queryFn(ressurs.navident),
    select: (ledere) => ledere.find((leder) => leder.erDagligOppfolging)?.leder,
  }).data;

  if (!dagligLeder) {
    return;
  }

  return (
    <>
      <span>Leder:</span>
      <div
        css={css`
          display: flex;
          gap: var(--a-spacing-2);
        `}
      >
        <ReactRouterLink
          params={{ id: dagligLeder.navident }}
          search={{}}
          to="/ressurs/$id"
        >
          <Link as="span">{formatVisningsnavn(dagligLeder.visningsnavn)}</Link>
        </ReactRouterLink>

        {dagligLeder && (
          <>
            <span>-</span>
            <span>Daglig oppfølging</span>
            <HelpText title="Hva er daglig leder">
              Denne lederen har det daglige ansvaret for sykefraværsoppfølging,
              godkjenning i MinWinTid, og har tilgang til den ansatte i
              Læringsportalen.
            </HelpText>
          </>
        )}
      </div>
    </>
  );
}
