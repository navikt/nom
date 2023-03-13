import { css } from "@emotion/react";
import { SuccessFilled } from "@navikt/ds-icons";
import { ExternalLink } from "@navikt/ds-icons";
import {
  Alert,
  BodyShort,
  Button,
  Heading,
  Link,
  Loader,
} from "@navikt/ds-react";
import type { ReactNode } from "react";
import { useParams } from "react-router";

import type { Ressurs } from "~/api/generated/graphql_generated";
import { useHentRessursMedNavidentQuery } from "~/api/generated/graphql_generated";

import { isInDevelopment as isInDevelopment } from "../../../utils";
import { FocusedRessursView } from "../../OrganisasjonsHierarkiPage/components/FocusedRessursView";

export function ResourceCreatedReceiptPage() {
  const { navident } = useParams();

  const hentRessursQuery = useHentRessursMedNavidentQuery({
    variables: { navident },
  });

  if (hentRessursQuery.loading) {
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

  const ressurs = hentRessursQuery.data?.ressurs;

  if (ressurs) {
    return <InnerResourceCreatedPage ressurs={ressurs as Ressurs} />;
  }

  if (hentRessursQuery.error) {
    return <Alert variant="error">{hentRessursQuery.error.message}</Alert>;
  }

  return <></>;
}
function InnerResourceCreatedPage({ ressurs }: { ressurs: Ressurs }) {
  return (
    <div
      css={css`
        display: flex;
        flex-direction: column;
        gap: var(--a-spacing-6);
      `}
    >
      <FocusedRessursView focusedRessurs={ressurs} />
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--a-spacing-8);
          background: var(--a-white);
          padding: var(--a-spacing-8) var(--a-spacing-14);
          border-radius: 5px;
          border: 1px solid var(--a-gray-500);
        `}
      >
        <Heading size="medium">Du er nå ferdig med 1 av 2 steg</Heading>
        <div
          css={css`
            display: grid;
            grid-template-columns: min-content 1fr;
            gap: var(--a-spacing-3);
          `}
        >
          <SuccessFilled
            css={css`
              height: 28px;
              width: 28px;
              color: var(--a-lightblue-700);
            `}
          />
          <span>
            Opprettelse av ressurs er vellykket! NAV-ident {ressurs.navident} er
            opprettet for {ressurs.visningsnavn}.
          </span>
          <CircleWithNumber>2</CircleWithNumber>
          <span>
            Du skal nå registrere lønnsopplysninger i lønnsskjemaet “Etablering”
            i Unit4.
          </span>
        </div>
        <Unit4Link />
      </div>
      <RemedyLink />
    </div>
  );
}

function Unit4Link() {
  const { navident } = useParams();
  const navidentWithoutFirstLetter = navident?.slice(1);
  const unit4BaseUrl = isInDevelopment()
    ? "https://d26isvw00022.test.local"
    : "https://agresso.adeo.no";

  return (
    <Button
      as="a"
      css={css`
        width: max-content;
      `}
      href={`${unit4BaseUrl}/agresso/ContentContainer.aspx?type=topgen&menu_id=003316&activityStepId=1-1&attribute_id=S108&helptext=1&client=HR&personId=${navidentWithoutFirstLetter}&PersonellType=F`}
      icon={<ExternalLink aria-hidden />}
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
  if (isInDevelopment()) {
    return <></>;
  }

  return (
    <BodyShort
      css={css`
        text-align: center;
      `}
    >
      Tilganger tildeles i{" "}
      <Link
        href="https://it-hjelpa.adeo.no/arsys/home"
        rel="noopener noreferrer"
        target="_blank"
      >
        identrutina i Remedy
      </Link>
    </BodyShort>
  );
}

function CircleWithNumber({ children }: { children: ReactNode }) {
  return (
    <div
      css={css`
        border: 2px solid var(--a-lightblue-800);
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        height: 30px;
        width: 30px;
        text-align: center;
        padding-left: 1px;
      `}
    >
      {children}
    </div>
  );
}
