import { css } from "@emotion/react";
import { Heading, Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "@tanstack/react-router";

import type { OrgEnhet } from "~/api/generated/graphql_generated";
import { UserImage } from "~/components/UserImage";
import { ressursRoute } from "~/reactRouterRoutes";
import buildingFilled from "~/resources/buildingFilled.svg";

import { HorizontalEdge } from "./CardViews";

const OrgenhetLederLink = (properties: { enhet: OrgEnhet }) => {
  const { leder } = properties.enhet;
  const firstLeder = leder[0]?.ressurs;
  const { navident, visningsnavn } = firstLeder ?? {};

  if (!navident) {
    return <span>Ingen leder</span>;
  }

  return (
    <ReactRouterLink params={{ id: navident }} to={ressursRoute.to}>
      <Link as="span">{visningsnavn ?? "Uten navn"}</Link>
    </ReactRouterLink>
  );
};

export const ValgtEnhet = ({ valgtEnhet }: { valgtEnhet: OrgEnhet }) => (
  <div
    css={css`
      display: grid;
      border-radius: 5px;
      margin-bottom: 1rem;
      background-color: var(--nom-secondary-hover);
      border: 1px solid var(--a-gray-400);
    `}
  >
    <HorizontalEdge />
    <div
      css={css`
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 2rem;
      `}
    >
      <div>
        <Heading level={"2"} size="large">
          <img
            alt=""
            css={css`
              margin-right: 1rem;
            `}
            src={buildingFilled}
          />
          {valgtEnhet.navn}
        </Heading>
        <div
          css={css`
            display: grid;
            grid-template-columns: 90px 1fr;
            gap: var(--a-spacing-2);
            margin-top: var(--a-spacing-2);
          `}
        >
          <span
            css={css`
              grid-column: span 2;
              font-size: var(--a-font-size-xlarge);
            `}
          >
            Leder: <OrgenhetLederLink enhet={valgtEnhet} />
          </span>
          <span>
            ID: <b>{valgtEnhet.id}</b>
          </span>
          {valgtEnhet.remedyEnhetId && (
            <span>
              Enhetsnummer: <b>{valgtEnhet.remedyEnhetId}</b>
            </span>
          )}
        </div>
      </div>
      <UserImage
        navident={valgtEnhet.leder[0]?.ressurs.navident}
        navn={valgtEnhet.leder[0]?.ressurs.visningsnavn ?? ""}
        size="130px"
      />
    </div>
  </div>
);
