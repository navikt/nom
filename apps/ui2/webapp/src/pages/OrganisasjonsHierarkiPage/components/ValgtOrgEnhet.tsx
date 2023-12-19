import { css } from "@emotion/react";
import { BodyShort, Heading, Link, ReadMore } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import React from "react";

import { getRessurs } from "~/api/react-query-endpoints";
import { UserImage } from "~/components/UserImage";
import { ResourceLink } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { ressursRoute } from "~/reactRouterRoutes";
import building from "~/resources/building.svg";
import type { OrgEnhet } from "~/types/api-models";
import { Sektor } from "~/types/api-models";
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
            <BodyShort size="small">
              ID:{" "}
              <span
                css={css`
                  font-weight: var(--a-font-weight-bold);
                `}
              >
                {orgEnhet.id}
              </span>
            </BodyShort>
            {orgEnhet.remedyEnhetId && (
              <BodyShort size="small">
                Remedy-enhet:{" "}
                <span
                  css={css`
                    font-weight: var(--a-font-weight-bold);
                  `}
                >
                  {orgEnhet.remedyEnhetId}
                </span>
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

  const hentedeRessurser = ressurser.map((ressurs) => {
    const ressursQuery = useQuery({
      queryKey: getRessurs.queryKey(ressurs),
      queryFn: () => getRessurs.queryFn(ressurs),
    });

    return ressursQuery.data;
  });

  return (
    <div>
      {hentedeRessurser.some((ressurs) => {
        return ressurs && !ressurs.sektor.includes(Sektor.EKSTERN);
      }) && (
        <>
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
            {hentedeRessurser.map((ressurs) => {
              if (ressurs && !ressurs.sektor.includes(Sektor.EKSTERN)) {
                return (
                  <ResourceLink
                    key={ressurs.navident}
                    navident={ressurs.navident}
                  />
                );
              }
            })}
          </div>
        </>
      )}
      {hentedeRessurser.some((ressurs) => {
        return ressurs && ressurs.sektor.includes(Sektor.EKSTERN);
      }) && (
        <ReadMore
          css={css`
            margin-top: var(--a-spacing-4);

            .navds-read-more__button {
              color: var(--a-text-default);
              font-size: var(--a-font-size-heading-small);
              font-weight: var(--a-font-weight-bold);
            }

            .navds-read-more__content {
              margin-left: 0;
              padding-left: 0;
              border-left: none;
            }
          `}
          defaultOpen
          header={"Eksterne medarbeidere i " + orgEnhet.navn}
        >
          <div
            css={css`
              margin-top: var(--a-spacing-4);
              display: grid;
              grid-template-columns: repeat(2, 1fr);
              gap: var(--a-spacing-4);
            `}
          >
            {hentedeRessurser.map((ressurs) => {
              if (ressurs && ressurs.sektor.includes(Sektor.EKSTERN)) {
                return (
                  <ResourceLink
                    key={ressurs.navident}
                    navident={ressurs.navident}
                  />
                );
              }
            })}
          </div>
        </ReadMore>
      )}
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
      <ReactRouterLink
        params={{ id: navident }}
        search={{}}
        to={ressursRoute.to}
      >
        <Link as="span">{formatVisningsnavn(visningsnavn)}</Link>
      </ReactRouterLink>
    </span>
  );
}
