import { css } from "@emotion/react";
import { PersonFillIcon } from "@navikt/aksel-icons";
import { Heading, Label, Link } from "@navikt/ds-react";
import React from "react";
import { Link as ReactRouterLink } from "react-router-dom";

import type {
  HentHierarkiQuery,
  OrgEnhet,
} from "~/api/generated/graphql_generated";
import type { Ressurs } from "~/api/generated/graphql_generated";
import {
  OverEnheterDivider,
  VerticalEdge,
} from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { getOverEnheterBackToRootRecursively } from "~/pages/OrganisasjonsHierarkiPage/OrganisasjonHierarkiPage";
import building from "~/resources/building.svg";

export function CompactOrganisasjonHierarki({
  orgHierarki,
  ressurs,
}: {
  orgHierarki: HentHierarkiQuery;
  ressurs: Ressurs;
}) {
  const firstOverEnhet = (orgHierarki.enhetsKoblinger?.overenheter ?? []).map(
    ({ organisasjonsenhet }) => organisasjonsenhet
  )[0];

  const overEnheter = firstOverEnhet
    ? getOverEnheterBackToRootRecursively(firstOverEnhet as OrgEnhet)
    : [];

  const valgtEnhet = orgHierarki.enhet;

  return (
    <>
      {overEnheter.map((overEnhet) => (
        <React.Fragment key={overEnhet.id}>
          <CompactOrgEnhet orgEnhet={overEnhet} selected={false} />
          <OverEnheterDivider />
        </React.Fragment>
      ))}
      {valgtEnhet && (
        <CompactOrgEnhet orgEnhet={valgtEnhet as OrgEnhet} selected />
      )}
      <OverEnheterDivider />
      <SelectedResource ressurs={ressurs} />
    </>
  );
}

function CompactOrgEnhet({
  orgEnhet,
  selected,
}: {
  orgEnhet: OrgEnhet;
  selected: boolean;
}) {
  return (
    <div
      css={[
        css`
          border-radius: 5px;
          border: 1px solid var(--a-gray-400);
          display: grid;
          grid-template-columns: min-content 1fr;
          gap: 1rem;
        `,
        selected
          ? css`
              background-color: var(--nom-secondary-hover);
            `
          : css`
              background-color: var(--a-white);
            `,
      ]}
    >
      <VerticalEdge>
        <img alt="" src={building} />
      </VerticalEdge>
      <div
        css={css`
          padding: 0.5rem 0;
          display: flex;
          flex-direction: column;
        `}
      >
        <Link as={ReactRouterLink} to={`/org/${orgEnhet.id}`}>
          <Heading level="3" size="small">
            {orgEnhet.navn}
          </Heading>
        </Link>
        <Link
          as={ReactRouterLink}
          to={`/ressurs/${orgEnhet.leder[0]?.ressurs.navident}`}
        >
          Leder:{" "}
          {orgEnhet.leder[0]?.ressurs.visningsnavn || "Leder ikke funnet"}
        </Link>
      </div>
    </div>
  );
}

function SelectedResource({ ressurs }: { ressurs: Ressurs }) {
  return (
    <div
      css={css`
        display: flex;
      `}
    >
      <div>
        <OverEnheterDivider />
        <div
          css={css`
            margin-left: 13px;
            background-color: var(--a-lightblue-800);
            width: 13px;
            height: 2px;
          `}
        />
      </div>
      <div
        css={css`
          display: flex;
          gap: 1rem;
          border-radius: 5px;
          border: 1px solid var(--a-gray-400);
          align-items: center;
          width: 100%;
          height: 40px;
        `}
      >
        <VerticalEdge>
          <PersonFillIcon color="white" />
        </VerticalEdge>
        <Label as="span">{ressurs.visningsnavn}</Label>
      </div>
    </div>
  );
}
