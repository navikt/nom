import { css } from "@emotion/react";
import { Next } from "@navikt/ds-icons";
import { Heading } from "@navikt/ds-react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";

import { apolloClient } from "~/api/apolloclient";
import type { OrgEnhet, Ressurs } from "~/api/generated/graphql_generated";
import { HentHierarkiDocument } from "~/api/generated/graphql_generated";

import { building, peopleFilledIcon } from "../../../components/Images";
import UserImage from "../../../components/UserImage";

export function DynamicColumnContainer({
  children,
  shouldUseDynamicSecondColumn,
}: {
  children: ReactNode;
  shouldUseDynamicSecondColumn: boolean;
}) {
  return (
    <div
      css={css(
        css`
          display: grid;
          grid-template-rows: min-content;
          column-gap: 1rem;
          row-gap: 1rem;
          grid-template-columns: 1fr;
        `,
        shouldUseDynamicSecondColumn &&
          css`
            @media (min-width: 60rem) {
              grid-template-columns: 1fr 1fr;
            }
          `
      )}
    >
      {children}
    </div>
  );
}

export function OrgEnhetView({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const fornavnAndEtternavnForImg =
    orgEnhet.leder[0]?.ressurs.visningsnavn ?? "";

  const navidentForImg = orgEnhet.leder[0]?.ressurs.navident;

  return (
    <Link
      css={hideLinkCss}
      onMouseOver={() => {
        apolloClient.query({
          query: HentHierarkiDocument,
          variables: {
            agressoId: orgEnhet.agressoId,
            orgNiv: orgEnhet.orgNiv,
          },
          fetchPolicy: "cache-first",
        });
      }}
      to={`/org/${orgEnhet.agressoId}/${orgEnhet.orgNiv}`}
    >
      <UnitContainerLink>
        <VerticalEdge>
          {/*<Building />*/}
          <img alt={"Enhet"} src={building} />
        </VerticalEdge>
        <div
          css={css`
            padding: 1rem 0;
          `}
        >
          <Heading level={"2"} size="medium">
            {orgEnhet.navn}
          </Heading>
          Leder:{" "}
          <span>
            {orgEnhet.leder[0]?.ressurs.visningsnavn || "Leder ikke funnet"}
          </span>
        </div>
        <UserImage navident={navidentForImg} navn={fornavnAndEtternavnForImg} />
      </UnitContainerLink>
    </Link>
  );
}

export function ResourceView({ ressurs }: { ressurs: Ressurs }) {
  return (
    <Link css={hideLinkCss} key={ressurs.navident} to={ressurs.navident}>
      <UnitContainerLink>
        <VerticalEdge>
          {/*<PeopleFilled color="white" />*/}
          <img alt={"Person"} src={peopleFilledIcon} />
        </VerticalEdge>
        <Heading
          css={css`
            padding: 1rem 0;
          `}
          level={"2"}
          size="medium"
        >
          {ressurs.visningsnavn ?? "Uten navn"}
        </Heading>
        <div />
      </UnitContainerLink>
    </Link>
  );
}

function UnitContainerLink({ children }: { children: ReactNode }) {
  return (
    <div
      css={css`
        height: 100%;
        border-radius: 5px;
        border: 1px solid var(--a-gray-400);
        background-color: var(--a-white);
        display: grid;
        grid-template-columns: min-content 1fr max-content 40px;
        align-items: center;
        gap: 1rem;
        cursor: pointer;

        &:hover {
          background: var(--nom-secondary-hover);
        }
      `}
    >
      {children}
      <Next height="2rem" width="2rem" />
    </div>
  );
}

function VerticalEdge({ children }: { children: ReactNode }) {
  return (
    <div
      css={css`
        background-color: var(--a-lightblue-800);
        display: flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        border-radius: 4px 0 0 4px;
        height: 100%;
      `}
    >
      {children}
    </div>
  );
}

export function HorizontalEdge() {
  return (
    <div
      css={css`
        background-color: var(--a-lightblue-800);
        height: 0.5rem;
        width: 100%;
      `}
    />
  );
}

export function OverEnheterDivider() {
  return (
    <div
      css={css`
        align-self: center;
        height: var(--a-spacing-4);
        background-color: var(--a-lightblue-800);
        width: 2px;
      `}
    />
  );
}

const hideLinkCss = css`
  text-decoration: none;
  color: inherit;
`;
