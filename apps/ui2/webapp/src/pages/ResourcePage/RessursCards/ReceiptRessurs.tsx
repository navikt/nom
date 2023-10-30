import { css } from "@emotion/react";
import { Heading, Link } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import React from "react";

import { getOrgEnhet, getOrgTilknytninger } from "~/api/react-query-endpoints";
import { UserImage } from "~/components/UserImage";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import { HorizontalEdge } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { ressursRoute } from "~/reactRouterRoutes";
import type { Ressurs } from "~/types/api-models";
import { formatVisningsnavn } from "~/utils";

export function ReceiptRessurs({ ressurs }: { ressurs: Ressurs }) {
  // Presently there will only be one orgTilknytning after creation.
  const firstOrgtilknytningOrgEnhetId = useQuery({
    queryKey: getOrgTilknytninger.queryKey(ressurs.navident),
    queryFn: () => getOrgTilknytninger.queryFn(ressurs.navident),
    select: (orgtilknytninger) => orgtilknytninger[0]?.orgEnhetId,
  }).data;

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(firstOrgtilknytningOrgEnhetId as string),
    queryFn: () => getOrgEnhet.queryFn(firstOrgtilknytningOrgEnhetId as string),
    enabled: !!firstOrgtilknytningOrgEnhetId,
  });

  const orgEnhet = orgEnhetQuery.data;
  const leder = orgEnhet?.ledere[0];

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
            {formatVisningsnavn(ressurs.visningsnavn)}
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
              <b>{SektorNameMapping[ressurs.sektor]}</b>
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
                  <Link as="span">
                    {formatVisningsnavn(leder.visningsnavn)}
                  </Link>
                </ReactRouterLink>
              </span>
            )}
          </div>
        </div>
        <UserImage
          navident={ressurs.navident}
          navn={formatVisningsnavn(ressurs.visningsnavn)}
          size="126px"
        />
      </div>
    </div>
  );
}
