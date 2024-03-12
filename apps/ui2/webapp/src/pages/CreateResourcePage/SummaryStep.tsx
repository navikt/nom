import { css } from "@emotion/react";
import { CheckmarkCircleFillIcon, ExternalLinkIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyShort,
  Button,
  Link,
  LinkPanel,
  Loader,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";

import { getRessurs } from "~/api/react-query-endpoints";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";
import type { Ressurs } from "~/types/api-models";
import { AnsettelsesType } from "~/types/api-models";
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

  return (
    <ContainerWithHighlightedTop
      icon={opprettRessursIcon}
      title={
        <div
          css={css`
            display: flex;
            align-items: center;
            gap: var(--a-spacing-4);
          `}
        >
          <CheckmarkCircleFillIcon
            css={css`
              color: var(--a-surface-success-hover);
              font-size: var(--a-spacing-8);
            `}
            title="a11y-title"
          />{" "}
          Medarbeideren er nå registrert i NOM
        </div>
      }
    >
      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-2);
        `}
      >
        <BodyShort>
          Medarbeideren er nå registrert i NOM og NAV-ident{" "}
          <b>{ressurs.navident}</b> er opprettet for{" "}
          <b>{formatVisningsnavn(ressurs.visningsnavn)}</b>.
        </BodyShort>{" "}
        <BodyShort>
          For å fullføre opprettelsen må leder eller identadministrator også gi
          tilganger i Identrutinen i Remedy.
        </BodyShort>
        <BodyShort>
          E-postadresse opprettes automatisk og blir synlig på medarbeiderens
          profilside når den er klar.
        </BodyShort>
        <BodyShort>
          Du kan også legge inn teamtilhørighet i{" "}
          <Link href={teamcatBaseUrl} target="_blank">
            Teamkatalogen
          </Link>
          , men det kan ta opp til to virkedager før personen vil bli
          tilgjengelig der. Personen selv eller andre kontaktpersoner kan også
          legge inn informasjon i Teamkatalogen.
        </BodyShort>
      </div>
      <LinkPanel
        border
        css={css`
          width: 60%;
          margin: auto;
        `}
        href="https://it-hjelpa.adeo.no/arsys/home"
      >
        <LinkPanel.Title>Gi tilganger i Remedy</LinkPanel.Title>
      </LinkPanel>
      <LinkPanel
        border
        css={css`
          width: 60%;
          margin: auto;
        `}
        href={`/ressurs/${ressurs.navident}`}
      >
        <LinkPanel.Title>Gå til profilsiden</LinkPanel.Title>
      </LinkPanel>
      <LinkPanel
        border
        css={css`
          width: 60%;
          margin: auto;
        `}
        href="/"
      >
        <LinkPanel.Title>Gå tilbake til forsiden</LinkPanel.Title>
      </LinkPanel>
    </ContainerWithHighlightedTop>
  );
}

function StatligInstructions({ ressurs }: { ressurs: Ressurs }) {
  return (
    <>
      <InformationText ressurs={ressurs} />
      <div
        css={css`
          display: flex;
          gap: var(--a-spacing-4);
          justify-content: center;
        `}
      >
        <RemedyLink />
        <Unit4Link />
      </div>
    </>
  );
}

function EksternInstructions({ ressurs }: { ressurs: Ressurs }) {
  return (
    <>
      <InformationText ressurs={ressurs} />
      <RemedyLink />
    </>
  );
}

function InformationText({ ressurs }: { ressurs: Ressurs }) {
  const teamcatBaseUrl = isInDevelopment()
    ? "https://teamkatalog.ekstern.dev.nav.no"
    : "https://teamkatalog.nav.no";

  return (
    <>
      <BodyShort>
        Medarbeideren er nå registrert i NOM og NAV-ident {ressurs.navident} er
        opprettet for {formatVisningsnavn(ressurs.visningsnavn)}. For å fullføre
        opprettelsen må det også gis tilganger i identrutina i Remedy. Leder
        eller identadministrator kan gjøre dette.
      </BodyShort>
      <BodyShort>
        Du kan også legge inn teamtilhørighet i{" "}
        <Link href={teamcatBaseUrl} target="_blank">
          Teamkatalogen
        </Link>
      </BodyShort>
    </>
  );
}

function Unit4Link() {
  const { navident, ansettelsesType } = useSearch({
    from: "/ressurs/create/$step",
  });

  const navidentWithoutFirstLetter = navident?.slice(1);
  const unit4BaseUrl = isInDevelopment()
    ? "https://agresso.test.local/agrt01"
    : "https://agresso.adeo.no/agresso";

  return (
    <Button
      as="a"
      css={css`
        width: max-content;
      `}
      href={`${unit4BaseUrl}/Default.aspx?&accessId=003316&attribute_id=S108&client=HR&personId=${navidentWithoutFirstLetter}&PersonellType=${
        ansettelsesType === AnsettelsesType.FAST ? "F" : "M"
      }`}
      icon={<ExternalLinkIcon aria-hidden />}
      iconPosition="right"
      rel="noopener noreferrer"
      target="_blank"
      variant="primary"
    >
      Registrer lønnsopplysninger i Unit4
    </Button>
  );
}

function RemedyLink() {
  return (
    <Button
      as="a"
      css={css`
        width: max-content;
      `}
      href="https://it-hjelpa.adeo.no/arsys/home"
      icon={<ExternalLinkIcon aria-hidden />}
      iconPosition="right"
      rel="noopener noreferrer"
      target="_blank"
      variant="primary"
    >
      Gi tilganger i Remedy
    </Button>
  );
}
