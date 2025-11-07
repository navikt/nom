import { css } from "@emotion/react";
import { PersonFillIcon } from "@navikt/aksel-icons";
import { Heading } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { SmartLink } from "~/components/navigation/SmartLink";
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
  ressurs?: Ressurs;
}) {
  return (
    <>
      <OverEnheter hasRessurs={!!ressurs} isRoot orgEnhetId={orgEnhet.id} />
      {!!ressurs && <SelectedResource ressurs={ressurs} />}
    </>
  );
}

function OverEnheter({
  orgEnhetId,
  isRoot,
  hasRessurs,
}: {
  orgEnhetId: string;
  isRoot: boolean;
  hasRessurs: boolean;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId),
  });

  const overEnhetId = orgEnhetQuery.data?.overordnetEnhetId;

  return (
    <>
      {overEnhetId && (
        <OverEnheter
          hasRessurs={false}
          isRoot={false}
          orgEnhetId={overEnhetId}
        />
      )}
      <CompactOrgEnhet orgEnhetId={orgEnhetId} selected={isRoot} />
      {(!isRoot || hasRessurs) && <OverEnheterDivider />}
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
        <SmartLink href={`/org/${orgEnhet.id}`}>
          <Heading level="3" size="small">
            {orgEnhet.navn}
          </Heading>
        </SmartLink>

        <span>
          Leder:{" "}
          <SmartLink href={`/ressurs/${orgEnhet.leder?.navident}`} inline>
            {orgEnhet.leder?.visningsnavn}
          </SmartLink>
        </span>
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
