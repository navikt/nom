import { css } from "@emotion/react";
import { BodyShort, Heading, Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import React from "react";

import type { HentHierarkiQuery } from "~/api/generated/graphql_generated";
import { UserImage } from "~/components/UserImage";
import { ResourceLink } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { ressursRoute } from "~/reactRouterRoutes";
import building from "~/resources/building.svg";

type Hierarchyressurs = NonNullable<
  HentHierarkiQuery["ressursKoblinger"]
>["koblinger"][0]["ressurs"];

export const ValgtOrgEnhet = ({
  ressurser,
  valgtEnhet,
}: {
  ressurser: Hierarchyressurs[];
  valgtEnhet: NonNullable<HentHierarkiQuery["enhet"]>;
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
            {valgtEnhet.navn}
          </Heading>
          <div
            css={css`
              display: flex;
              column-gap: var(--a-spacing-4);
            `}
          >
            <BodyShort size="small">ID: {valgtEnhet.id}</BodyShort>
            {valgtEnhet.remedyEnhetId && (
              <BodyShort size="small">
                Remedy-enhet: {valgtEnhet.remedyEnhetId}
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
            navident={valgtEnhet.leder[0]?.ressurs.navident}
            navn={valgtEnhet.leder[0]?.ressurs.visningsnavn ?? ""}
            size="90px"
          />
          <OrgenhetLederLink orgEnhet={valgtEnhet} />
        </div>
      </div>
      <MedarbeidereList orgEnhet={valgtEnhet} ressurser={ressurser} />
    </div>
  </div>
);

function MedarbeidereList({
  orgEnhet,
  ressurser,
}: {
  orgEnhet: NonNullable<HentHierarkiQuery["enhet"]>;
  ressurser: Hierarchyressurs[];
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
          <ResourceLink key={ressurs.navident} ressurs={ressurs} />
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

function OrgenhetLederLink({
  orgEnhet,
}: {
  orgEnhet: NonNullable<HentHierarkiQuery["enhet"]>;
}) {
  const { leder } = orgEnhet;
  const firstLeder = leder[0]?.ressurs;
  const { navident, visningsnavn } = firstLeder ?? {};

  if (!navident) {
    return <span>Ingen leder</span>;
  }

  return (
    <span>
      Leder:{" "}
      <ReactRouterLink params={{ id: navident }} to={ressursRoute.to}>
        <Link as="span">{visningsnavn ?? "Uten navn"}</Link>
      </ReactRouterLink>
    </span>
  );
}
