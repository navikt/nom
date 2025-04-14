import { css } from "@emotion/react";
import {
  PencilIcon,
  PersonIcon,
  PersonTallShortIcon,
} from "@navikt/aksel-icons";
import {
  Button,
  Heading,
  HelpText,
  HStack,
  Link,
  Tag,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink, useNavigate } from "@tanstack/react-router";
import { subMonths } from "date-fns";
import React from "react";

import {
  getHarParorendeTilgang,
  getLedereForRessurs,
  harLederTilgangForNavidentOptions,
} from "~/api/react-query-endpoints";
import {
  useGetSession,
  useHasAdminAccess,
  useHasSelvbetjeningAccess,
  useHasWriteAccess,
} from "~/api/useGetSession";
import { LabelWithValue } from "~/components/LabelWithValue";
import { UserImage } from "~/components/UserImage";
import { useFysiskBrukersted } from "~/hooks/useFysiskBrukersted";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import type { Ressurs } from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import { TelefonType } from "~/types/api-models";
import {
  capitalizeNameFysiskBrukersted,
  dateWithoutTimezone,
  formatDisplayDate,
  formatTlf,
  formatVisningsnavn,
  isBeforeToday,
  isSameDateOrAfter,
  todayWithoutTimezone,
} from "~/utils";

// Det er kanskje litt unødvendig å ha en wrapper komponent rundt hver detalj (navident, sluttdato, etc)
// men det kommer stadig endringer i reglene for logikken rundt når noe skal vises og ikke.
// Og som endrer seg etterhvert som vi støtter flere sektorer.
// Derfor greit å samle alle disse på et sted slik at man f.eks ikke risikere å vise SluttDato i ett bilde,
// men ikke i et annet fordi man glemte ordne logikk begge steder.

export function PersonOpplysninger({ ressurs }: { ressurs: Ressurs }) {
  const navident = ressurs.navident;

  const kontaktinfoTilgangQuery = useQuery({
    queryKey: getHarParorendeTilgang.queryKey(navident),
    queryFn: () => getHarParorendeTilgang.queryFn(navident),
    enabled: !!navident,
  });

  const hasKontaktinfoAccessForRessurs = kontaktinfoTilgangQuery.isSuccess;

  const hasSelfServiceAccess =
    useGetSession().data?.navident === ressurs.navident;

  const hasAdminAccess = useHasAdminAccess();

  const harSluttet = !!ressurs.sluttdato && isBeforeToday(ressurs.sluttdato);

  const sluttetOverToMndSiden =
    !!ressurs.sluttdato &&
    !isSameDateOrAfter(
      ressurs.sluttdato,
      dateWithoutTimezone(subMonths(todayWithoutTimezone(), 2)),
    );

  return (
    <HStack>
      <VStack
        css={css`
          flex: 1;
        `}
      >
        <div
          css={css`
            display: flex;
            align-items: center;
          `}
        >
          <Heading level="1" size="large">
            {formatVisningsnavn(ressurs.visningsnavn)}
          </Heading>

          {harSluttet && (
            <Tag
              css={css`
                margin-left: var(--a-spacing-4);
                height: fit-content;
              `}
              size="medium"
              variant="warning"
            >
              Sluttet
            </Tag>
          )}
        </div>

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
          {(hasKontaktinfoAccessForRessurs ||
            hasSelfServiceAccess ||
            hasAdminAccess) &&
            !sluttetOverToMndSiden && (
              <>
                <span>Kontaktpersoner:</span>

                <div
                  css={css`
                    display: flex;
                  `}
                >
                  <ReactRouterLink
                    params={{ id: ressurs.navident }}
                    to="/kontaktpersoner/$id"
                  >
                    {/* Egentlig ikke anbefalt å bruke negativ margin, men det er den eneste måten jeg klarte å oppnå designet som var ønsket */}
                    <Button
                      css={css`
                        width: fit-content;
                        margin-left: -12px;
                        margin-top: -4px;
                      `}
                      icon={<PersonTallShortIcon title="a11y-title" />}
                      size="small"
                      type="button"
                      variant="tertiary"
                    >
                      Se kontaktpersoner
                    </Button>
                  </ReactRouterLink>

                  <HelpText title="Hvem kan se denne informasjonen?">
                    Kun synlig for medarbeideren selv, nærmeste leder og leders
                    leder.
                  </HelpText>
                </div>
              </>
            )}
        </div>
      </VStack>

      <VStack align="end" justify="space-between">
        <UserImage
          includeInstructionsWhenNotFound
          navident={ressurs.navident}
          navn={formatVisningsnavn(ressurs.visningsnavn)}
          size="126px"
        />

        <EditPersonopplysningerButton
          harSluttet={harSluttet}
          ressurs={ressurs}
        />
      </VStack>
    </HStack>
  );
}

function EditPersonopplysningerButton({
  ressurs,
  harSluttet,
}: {
  ressurs: Ressurs;
  harSluttet: boolean;
}) {
  const navigate = useNavigate();

  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const hasWriteAccessForRessurs =
    useQuery(harLederTilgangForNavidentOptions(ressurs.navident)).data ?? false;

  const hasSelvbetjeningAccess = useHasSelvbetjeningAccess(ressurs.navident);

  if (
    (((hasWriteAccess && hasWriteAccessForRessurs) || hasSelvbetjeningAccess) &&
      !harSluttet) ||
    hasAdminAccess
  ) {
    return (
      <Button
        data-cy="endre-personopplysninger-button"
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
  return <LabelWithValue label="Nav-ident" value={ressurs.navident} />;
}

export function StartDato({ ressurs }: { ressurs: Ressurs }) {
  return (
    <LabelWithValue
      helpText="Første arbeidsdag medarbeideren jobbet for Nav. Datoen kan være fra tidligere arbeidsforhold i Nav.
      (Historiske startdatoer fra noen år tilbake kan avvike noe fra den faktiske første arbeidsdagen. Datoen vil
      da vanligvis representere dagen Nav-identen ble opprettet.)"
      label="Startdato"
      value={formatDisplayDate(ressurs.startdato)}
    />
  );
}

export function SluttDato({ ressurs }: { ressurs: Ressurs }) {
  const hasAdminAccess = useHasAdminAccess();
  const hasWriteAccess = useHasWriteAccess();

  const hasWriteAccessForRessurs =
    useQuery(harLederTilgangForNavidentOptions(ressurs.navident)).data ?? false;

  const hasSelvbetjeningAccess = useHasSelvbetjeningAccess(ressurs.navident);

  const harSluttet = !!ressurs.sluttdato && isBeforeToday(ressurs.sluttdato);

  if (
    ressurs.sluttdato &&
    ((hasWriteAccess && hasWriteAccessForRessurs) ||
      hasSelvbetjeningAccess ||
      hasAdminAccess ||
      ressurs.sektor === "EKSTERN" ||
      harSluttet)
  ) {
    return (
      <LabelWithValue
        helpText="Medarbeiderens siste arbeidsdag i Nav"
        label="Sluttdato"
        value={formatDisplayDate(ressurs.sluttdato)}
      />
    );
  }

  return;
}

export function SektorView({ ressurs }: { ressurs: Ressurs }) {
  return (
    <LabelWithValue
      helpText="Medarbeiders sektor hentes fra sektoren som er registrert på organisasjonstilknytningen.
      Dersom sektor er feil, må det opprettes en ny organisasjonstilknytning med korrekt sektor."
      label="Sektor"
      value={SektorNameMapping[ressurs.sektor]}
    />
  );
}

export function Epost({ ressurs }: { ressurs: Ressurs }) {
  return <LabelWithValue label="E-post" value={ressurs.epost?.toLowerCase()} />;
}

export function FysiskBrukersted({ ressurs }: { ressurs: Ressurs }) {
  const fysiskBrukersted = useFysiskBrukersted(ressurs.primaryFysiskBrukersted);
  return (
    <LabelWithValue
      label="Fysisk brukersted"
      value={
        fysiskBrukersted
          ? capitalizeNameFysiskBrukersted(fysiskBrukersted.brukerstedNavn)
          : "-"
      }
    />
  );
}

export function TelefonNummere({
  ressurs,
  redigering,
}: {
  ressurs: Ressurs;
  redigering?: boolean;
}) {
  const formaterOgFjernDuplikater = (numre: string[]) =>
    Array.from(new Set(numre.map((nummer) => formatTlf(nummer))));

  const kontorTelefoner = formaterOgFjernDuplikater(
    ressurs.telefoner
      .filter(
        (telefonnummer) =>
          telefonnummer.type === TelefonType.NAV_KONTOR_TELEFON,
      )
      .map((telefonnummer) => telefonnummer.nummer),
  );

  const tjenesteTelefoner = formaterOgFjernDuplikater(
    ressurs.telefoner
      .filter(
        (telefonnummer) =>
          telefonnummer.type === TelefonType.NAV_TJENESTE_TELEFON,
      )
      .map((telefonnummer) => telefonnummer.nummer),
  );

  const privatTelefon = ressurs.primaryTelefonnummer
    ? formatTlf(ressurs.primaryTelefonnummer)
    : undefined;

  return (
    <>
      <LabelWithValue
        label="Kontortelefon"
        value={kontorTelefoner.join(", ")}
      />

      <LabelWithValue
        label="Tjenestetelefon"
        value={tjenesteTelefoner.join(", ")}
      />

      {!redigering && (
        <LabelWithValue label="Telefonnummer" value={privatTelefon} />
      )}
    </>
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
            <HelpText title="Hva er daglig leder?">
              {(() => {
                switch (ressurs.sektor) {
                  case Sektor.NAV_STATLIG: {
                    return (
                      "Denne lederen har det daglige ansvaret for sykefraværsoppfølging, " +
                      "godkjenning av timeføring, og har tilgang til medarbeideren i Kompasset."
                    );
                  }
                  case Sektor.NAV_KOMMUNAL: {
                    return (
                      "Denne lederen har det daglige ansvaret for medarbeideren, " +
                      "og har tilgang til medarbeideren i Kompasset."
                    );
                  }
                  case Sektor.EKSTERN: {
                    return "Denne lederen har det daglige ansvaret for den eksterne medarbeideren.";
                  }
                }
              })()}
            </HelpText>
          </>
        )}
      </div>
    </>
  );
}
