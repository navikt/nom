import { css } from "@emotion/react";
import { CheckmarkCircleFillIcon, ChevronRightIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyShort,
  HStack,
  Label,
  Loader,
  VStack,
} from "@navikt/ds-react";
import { Link } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";

import { getRessurs } from "~/api/react-query-endpoints";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import opprettRessursIconRoundedBackground from "~/resources/opprettRessursIconRoundedBakground.svg";
import type { Ressurs } from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import { formatVisningsnavn, isInDevelopment } from "~/utils";

export function SummaryStep() {
  const { navident } = useSearch({ from: "/ressurs/create/$step" });

  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(navident as string),
    queryFn: () => getRessurs.queryFn(navident as string),
    enabled: !!navident,
  });

  if (ressursQuery.isLoading) {
    return (
      <Loader
        css={css`
          align-self: center;
        `}
        size="2xlarge"
        title={`Søker etter ${navident}`}
      />
    );
  }

  const ressurs = ressursQuery.data;

  if (ressurs) {
    return <Receipt ressurs={ressurs} />;
  }

  if (ressursQuery.error) {
    return <Alert variant="error">{ressursQuery.error.message}</Alert>;
  }

  return <></>;
}

function Receipt({ ressurs }: { ressurs: Ressurs }) {
  const teamcatBaseUrl = isInDevelopment()
    ? "https://teamkatalog.ekstern.dev.nav.no"
    : "https://teamkatalog.nav.no";

  const sharepointLenke: string =
    "https://navno.sharepoint.com/sites/intranett-organisasjon/SitePages/Organisasjonsmaster-(NOM).aspx";

  const identskjemaLenke: string =
    "https://navno.sharepoint.com/:b:/r/sites/intranett-hr/Delte%20dokumenter/HR-skjemaer/Identbekreftelses%20skjema.pdf?csf=1&web=1&e=E4RvBs";

  const lonnsskjemaLenke =
    "https://agresso.adeo.no/agresso/ContentContainer.aspx?type=topgen&menu_id=002820&activityStepId=1-1&attribute_id=S108&client=HR";

  // Inntil vi vet bedre hvem som trenger å se denne teksten (f.eks antagelser fra hierarkiet) så skrur vi den av for nå. For majoriteten vil den heller være forvirrende
  const showTeamcatInstructions = false;

  return (
    <ContainerWithHighlightedTop
      icon={opprettRessursIconRoundedBackground}
      title={
        <HStack gap="4">
          <CheckmarkCircleFillIcon
            css={css`
              color: var(--a-surface-success-hover);
              font-size: var(--a-font-size-heading-xlarge);
            `}
          />
          Medarbeideren er nå registrert i NOM
        </HStack>
      }
    >
      <div
        css={css`
          margin: auto;
        `}
      >
        <HStack
          css={css`
            display: grid;
          `}
          gap="2"
        >
          <BodyShort>
            Medarbeideren er nå registrert i NOM og NAV-ident{" "}
            <b>{ressurs.navident}</b> er opprettet for{" "}
            <b>{formatVisningsnavn(ressurs.visningsnavn)}</b>.
          </BodyShort>
          <BodyShort>
            For å fullføre opprettelsen må leder eller identadministrator også
            gi tilganger i Identrutinen i Remedy.
          </BodyShort>
          <BodyShort>
            E-postadresse opprettes automatisk og blir synlig på medarbeiderens
            profilside når den er klar.
          </BodyShort>
          <BodyShort>
            Alle nye medarbeidere må signere identskjema.{" "}
            <Link href={identskjemaLenke}>
              Gå til skjemaet for å laste det ned
            </Link>
            .
          </BodyShort>
          {showTeamcatInstructions && (
            <BodyShort>
              Du kan også legge inn teamtilhørighet i{" "}
              <Link href={teamcatBaseUrl} target="_blank">
                Teamkatalogen
              </Link>
              , men det kan ta opp til to virkedager før personen vil bli
              tilgjengelig der. Personen selv eller andre kontaktpersoner kan
              også legge inn informasjon i Teamkatalogen.
            </BodyShort>
          )}

          {ressurs.sektor === Sektor.NAV_KOMMUNAL && (
            <Alert
              css={css`
                margin-top: var(--a-spacing-4);
                max-width: fit-content;
                justify-self: center;
              `}
              variant="info"
            >
              <BodyShort>
                Kommunalt ansatte skal i utgangspunktet ikke lenger registreres
                i Unit4. Registrering i Unit4 er kun nødvendig for:
                <ul>
                  <li>Kommunale ledere</li>
                  <li>
                    Kommunalt ansatte med behov for tilganger via Unit4, skal ha
                    dekket utlegg eller skal være stedfortredere
                  </li>
                </ul>
                <Link href={sharepointLenke}>
                  På denne siden på Navet kan du ser mer detaljert informasjon
                  om hvilke typer kommunalt ansatte som skal registreres både i
                  NOM og i Unit4.
                </Link>
              </BodyShort>
            </Alert>
          )}

          {ressurs.sektor === Sektor.NAV_STATLIG && (
            <Alert
              css={css`
                margin-top: var(--a-spacing-4);
                max-width: fit-content;
                justify-self: center;
              `}
              variant="info"
            >
              <VStack gap="2">
                <BodyShort>
                  Alle statlig ansatte må også registreres i Unit4 for å få
                  utbetalt lønn.
                </BodyShort>
                <BodyShort>
                  Statlig ansatte registreres i Unit4 ved hjelp av lønnsskjema.
                  Det aktuelle lønnsskjemaet{" "}
                  <Link href={lonnsskjemaLenke}>finner du her</Link>.
                </BodyShort>
              </VStack>
            </Alert>
          )}
        </HStack>
      </div>

      <PanelLink
        href="https://it-hjelpa.adeo.no/arsys/home"
        label="Gi tilganger i Identrutina (må vente opptil 1 minutt)"
      />
      {/*TODO: disse burde ikke være hrefs, men bruker Routeren sine lenker slik at det ikke gjøres en sidenavigasjon og hele siden må lastes på nytt.*/}
      <PanelLink
        href={`/ressurs/${ressurs.navident}`}
        label="Vis medarbeiderens profil"
      />
      <PanelLink href="/" label="Gå tilbake til forsiden" />
    </ContainerWithHighlightedTop>
  );
}

function PanelLink({ label, href }: { label: string; href: string }) {
  return (
    <Link
      css={css`
        display: flex;
        width: 70%;
        margin: auto;
        padding: var(--a-spacing-4);
        border: 1px solid var(--a-border-default);
        text-decoration: none;
        font-size: var(--a-font-size-heading-medium);
        color: var(--a-text-default);
        justify-content: space-between;

        :hover,
        :active {
          color: var(--a-text-action-hover);
          border: 1px solid var(--a-border-action-hover);
          text-decoration: underline;
          box-shadow: var(--a-shadow-small);
          background: unset;
        }
      `}
      href={href}
    >
      <Label
        css={css`
          cursor: pointer;
          font-size: inherit;
          line-height: inherit;
        `}
      >
        {label}
      </Label>
      <ChevronRightIcon />
    </Link>
  );
}
