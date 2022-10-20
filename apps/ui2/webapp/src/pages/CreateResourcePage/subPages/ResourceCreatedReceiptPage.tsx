import {
  Alert,
  BodyShort,
  Button,
  Heading,
  Link,
  Loader,
} from "@navikt/ds-react";
import RessursInfo from "../../../components/OrgHierarki/RessursInfo";
import { useParams } from "react-router";
import {
  Ressurs,
  useHentRessursMedNavIdentQuery,
} from "../../../api/generated/graphql_generated";
import { css } from "@emotion/css";
import { SuccessFilled } from "@navikt/ds-icons";
import { ExternalLink } from "@navikt/ds-icons";
import type { ReactNode } from "react";
import { isInDev } from "../../../resources/utils/utils";

export function ResourceCreatedReceiptPage() {
  const { navIdent } = useParams();

  const hentRessursQuery = useHentRessursMedNavIdentQuery({
    variables: { navIdent },
  });

  if (hentRessursQuery.loading) {
    return (
      <Loader
        className={css`
          align-self: center;
        `}
        size="2xlarge"
        title={`Søker etter ${navIdent}`}
      />
    );
  }

  if (hentRessursQuery.error) {
    return <Alert variant="error">{hentRessursQuery.error.message}</Alert>;
  }

  const ressurs = hentRessursQuery.data?.ressurs;

  if (ressurs) {
    return <InnerResourceCreatedPage ressurs={ressurs as Ressurs} />;
  }

  return null;
}
function InnerResourceCreatedPage({ ressurs }: { ressurs: Ressurs }) {
  return (
    <div
      className={css`
        display: flex;
        flex-direction: column;
        gap: var(--navds-spacing-6);
      `}
    >
      <RessursInfo ressurs={ressurs} />
      <div
        className={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--navds-spacing-8);
          background: var(--navds-global-color-white);
          padding: var(--navds-spacing-8) var(--navds-spacing-14);
          border-radius: 5px;
          border: 1px solid var(--navds-global-color-gray-500);
        `}
      >
        <Heading size="medium">Du er nå ferdig med 1 av 2 steg</Heading>
        <div
          className={css`
            display: grid;
            grid-template-columns: min-content 1fr;
            gap: var(--navds-spacing-3);
          `}
        >
          <SuccessFilled
            className={css`
              height: var(--navds-spacing-6);
              width: var(--navds-spacing-6);
              color: var(--navds-global-color-lightblue-700);
            `}
          />
          <span>
            Opprettelse av ressurs er vellykket! NAV-ident {ressurs.navident} er
            opprettet for {ressurs.visningsNavn}. Personens e-post og
            telefonnummer vil være synlig om noen dager.
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
  const { navIdent } = useParams();
  const navIdentWithoutFirstLetter = navIdent?.substring(1);
  const unit4BaseUrl = isInDev()
    ? "https://agresso.test.local"
    : "https://agresso.adeo.no";

  return (
    <Button
      as="a"
      href={`${unit4BaseUrl}/BusinessWorldDemoM7/ContentContainer.aspx?type=topgen&menu_id=00912225&activityStepId=1-1&attribute_id=S108&client=EN&personId=${navIdentWithoutFirstLetter}&PersonellType=F`}
      rel="noopener noreferrer"
      target="_blank"
      variant="primary"
      icon={<ExternalLink aria-hidden />}
      iconPosition="right"
      className={css`
        width: max-content;
      `}
    >
      Registrer lønnsopplysninger i Unit4
    </Button>
  );
}

function RemedyLink() {
  if (isInDev()) {
    return null;
  }

  return (
    <BodyShort
      className={css`
        text-align: center;
      `}
    >
      Tilganger tildeles i{" "}
      <Link
        rel="noopener noreferrer"
        target="_blank"
        href="https://it-hjelpa.adeo.no/arsys/home"
      >
        identrutina i Remedy
      </Link>
    </BodyShort>
  );
}

function CircleWithNumber({ children }: { children: ReactNode }) {
  return (
    <span
      className={css`
        border: 1px solid black;
        border-radius: 50%;
        height: var(--navds-spacing-6);
        width: var(--navds-spacing-6);
        text-align: center;
        padding-left: 1px;
      `}
    >
      {children}
    </span>
  );
}
