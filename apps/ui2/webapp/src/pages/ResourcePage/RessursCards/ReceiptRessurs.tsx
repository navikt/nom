import { css } from "@emotion/react";
import { Heading, Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import React from "react";

import type { HentRessursMedNavidentQuery } from "~/api/generated/graphql_generated";
import { UserImage } from "~/components/UserImage";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import { HorizontalEdge } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { ressursRoute } from "~/reactRouterRoutes";

export function ReceiptRessurs({
  ressurs,
}: {
  ressurs: NonNullable<HentRessursMedNavidentQuery["ressurs"]>;
}) {
  const orgEnhet = ressurs.orgTilknytning.find(
    (kobling) => kobling.erDagligOppfolging,
  )?.orgEnhet;
  const leder = orgEnhet?.leder[0]?.ressurs;

  return (
    <div
      css={css`
        border-radius: 5px;
        background-color: var(--nom-secondary-hover);
        border: 1px solid var(--a-gray-400);
      `}
    >
      <HorizontalEdge />
      <div
        css={css`
          padding: 2rem;
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          flex-wrap: wrap-reverse;
          gap: var(--a-spacing-4);
        `}
      >
        <div>
          <Heading level="1" size="large" spacing>
            {ressurs.visningsnavn}
          </Heading>
          <div
            css={css`
              display: grid;
              gap: 1rem;
              grid-template-columns: 1fr 1fr;
            `}
          >
            <span>
              Sektor:
              <b>
                {ressurs.sektor
                  .map((sektor) => SektorNameMapping[sektor])
                  .join(", ")}
              </b>
            </span>
            <span>
              Navident: <b>{ressurs.navident}</b>
            </span>
            {orgEnhet && (
              <span>
                Org.enhet: <b>{orgEnhet.navn}</b>
              </span>
            )}
            {orgEnhet && (
              <span>
                Enhetsnummer: <b>{orgEnhet.remedyEnhetId}</b>
              </span>
            )}
            {leder && (
              <span>
                Leder:{" "}
                <ReactRouterLink
                  params={{ id: leder.navident }}
                  to={ressursRoute.to}
                >
                  <Link as="span">{leder.visningsnavn}</Link>
                </ReactRouterLink>
              </span>
            )}
          </div>
        </div>
        <UserImage
          navident={ressurs.navident}
          navn={ressurs.visningsnavn ?? ""}
          size="126px"
        />
      </div>
    </div>
  );
}
