import { css } from "@emotion/react";
import { PersonFillIcon } from "@navikt/aksel-icons";
import { Heading, Link } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import React from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import {
  OverEnheterDivider,
  VerticalEdge,
} from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import building from "~/resources/building.svg";
import type { OrgEnhet, Ressurs } from "~/types/api-models";
import { formatVisningsnavn } from "~/utils";

export function CompactOrganisasjonHierarki({
  orgEnhet,
  ressurs,
}: {
  orgEnhet: OrgEnhet;
  ressurs: Ressurs;
}) {
  return (
    <>
      <OverEnheter isRoot orgEnhetId={orgEnhet.id} />
      <SelectedResource ressurs={ressurs} />
    </>
  );
}

function OverEnheter({
  orgEnhetId,
  isRoot,
}: {
  orgEnhetId: string;
  isRoot: boolean;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId),
  });

  const overEnhetId = orgEnhetQuery.data?.overordnetEnhetId;

  return (
    <>
      {overEnhetId && <OverEnheter isRoot={false} orgEnhetId={overEnhetId} />}
      <CompactOrgEnhet orgEnhetId={orgEnhetId} selected={isRoot} />
      <OverEnheterDivider />
    </>
  );
}

function CompactOrgEnhet({
  orgEnhetId,
  selected,
}: {
  orgEnhetId: string;
  selected: boolean;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId),
  });

  const orgEnhet = orgEnhetQuery.data;

  if (!orgEnhet) {
    return <></>;
  }

  return (
    <div
      css={[
        css`
          border-radius: 5px;
          border: 1px solid var(--a-gray-400);
          display: grid;
          grid-template-columns: min-content 1fr;
          gap: var(--a-spacing-4);
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
          padding: var(--a-spacing-2) 0;
          display: flex;
          flex-direction: column;
        `}
      >
        <ReactRouterLink params={{ id: orgEnhet.id }} search={{}} to="/org/$id">
          <Link as={Heading} level="3" size="small">
            {orgEnhet.navn}
          </Link>
        </ReactRouterLink>
        <ReactRouterLink
          params={{ id: orgEnhet.ledere[0]?.navident }}
          search={{}}
          to="/ressurs/$id"
        >
          <Link as="span">
            Leder: {formatVisningsnavn(orgEnhet.ledere[0]?.visningsnavn)}
          </Link>
        </ReactRouterLink>
      </div>
    </div>
  );
}

function SelectedResource({ ressurs }: { ressurs: Ressurs }) {
  return (
    <div
      css={css`
        display: flex;
        gap: var(--a-spacing-4);
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
      <b>{formatVisningsnavn(ressurs.visningsnavn)}</b>
    </div>
  );
}
