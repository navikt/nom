import { css } from "@emotion/react";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, Link, Loader } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink, useSearch } from "@tanstack/react-router";

import { getRessurs } from "~/api/react-query-endpoints";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { ReceiptRessurs } from "~/pages/ResourcePage/RessursCards/ReceiptRessurs";
import { createRessursRoute, indexRoute } from "~/reactRouterRoutes";
import type { Ressurs } from "~/types/api-models";
import { AnsettelsesType, Sektor } from "~/types/api-models";
import { formatVisningsnavn, isInDevelopment } from "~/utils";

export function SummaryStep() {
  const { navident } = useSearch({ from: createRessursRoute.id });

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
  return (
    <ContainerWithHighlightedTop title="Medarbeideren er nå registrert i NOM">
      <ReceiptRessurs ressurs={ressurs} />
      {ressurs.sektor === Sektor.NAV_STATLIG ? (
        <StatligInstructions ressurs={ressurs} />
      ) : (
        <EksternInstructions ressurs={ressurs} />
      )}
      <ReactRouterLink
        css={css`
          align-self: center;
        `}
        to={indexRoute.to}
      >
        <Link as="span">Gå tilbake til forsiden</Link>
      </ReactRouterLink>
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
    from: createRessursRoute.id,
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
