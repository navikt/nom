import { css } from "@emotion/react";
import { BodyShort, Heading, Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import React from "react";

import { UserImage } from "~/components/UserImage";
import { ResourceLink } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { ressursRoute } from "~/reactRouterRoutes";
import building from "~/resources/building.svg";
import type { OrgEnhet } from "~/types/api-models";
import { formatVisningsnavn } from "~/utils";

export const ValgtOrgEnhet = ({
  ressurser,
  orgEnhet,
}: {
  ressurser: string[];
  orgEnhet: OrgEnhet;
}) => (
  <div
    css={css`
      display: grid;
      border-radius: var(--a-spacing-2);
      background-color: var(--nom-secondary-hover);
      border: 1px solid var(--a-gray-400);
    `}
  >
    <TopBorder />
    <div
      css={css`
        display: flex;
        flex-direction: column;
        gap: var(--a-spacing-6);
        padding: var(--a-spacing-6);
      `}
    >
      <div
        css={css`
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
        `}
      >
        <div>
          <Heading level="2" size="large" spacing>
            {orgEnhet.navn}
          </Heading>
          <div
            css={css`
              display: flex;
              column-gap: var(--a-spacing-4);
            `}
          >
            <BodyShort size="small">ID: {orgEnhet.id}</BodyShort>
            {orgEnhet.remedyEnhetId && (
              <BodyShort size="small">
                Remedy-enhet: {orgEnhet.remedyEnhetId}
              </BodyShort>
            )}
          </div>
        </div>

        <div
          css={css`
            display: flex;
            flex-direction: column;
            gap: var(--a-spacing-4);
            align-items: center;
          `}
        >
          <UserImage
            navident={orgEnhet.ledere[0]?.navident}
            navn={formatVisningsnavn(orgEnhet.ledere[0]?.visningsnavn)}
            size="90px"
          />
          <OrgenhetLederLink orgEnhet={orgEnhet} />
        </div>
      </div>
      <MedarbeidereList orgEnhet={orgEnhet} ressurser={ressurser} />
    </div>
  </div>
);

function MedarbeidereList({
  orgEnhet,
  ressurser,
}: {
  orgEnhet: OrgEnhet;
  ressurser: string[];
}) {
  if (ressurser.length === 0) {
    return <></>;
  }

  return (
    <div>
      <Heading level="3" size="small" spacing>
        Medarbeidere i {orgEnhet.navn}
      </Heading>
      <div
        css={css`
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: var(--a-spacing-4);
        `}
      >
        {ressurser.map((ressurs) => (
          <ResourceLink key={ressurs} navident={ressurs} />
        ))}
      </div>
    </div>
  );
}

function TopBorder() {
  return (
    <div
      css={css`
        display: flex;
        gap: var(--a-spacing-3);
        outline: 2px solid var(--a-lightblue-800);
        outline-offset: -1px;
        background-color: var(--a-lightblue-800);
        border-radius: 4px 4px 0 0;
        color: white;
        padding: var(--a-spacing-1) var(--a-spacing-3);
      `}
    >
      <img alt="" src={building} />
      Organisasjonsenhet
    </div>
  );
}

function OrgenhetLederLink({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const firstLeder = orgEnhet.ledere[0];
  const { navident, visningsnavn } = firstLeder ?? {};

  if (!navident) {
    return <span>Ingen leder</span>;
  }

  return (
    <span>
      Leder:{" "}
      <ReactRouterLink params={{ id: navident }} to={ressursRoute.to}>
        <Link as="span">{formatVisningsnavn(visningsnavn)}</Link>
      </ReactRouterLink>
    </span>
  );
}
