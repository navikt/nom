import { css } from "@emotion/react";
import { Heading, Label, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React from "react";

import { getGlobalStatistikk, getOrgEnhet } from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { OrgEnhetStatistikk } from "~/components/statistikk/OrgEnhetStatistikk";
import {
  forsideOrgEnhetIder,
  navToppnodeOrgEnhetId,
} from "~/customUtils/environmentUtils";
import building from "~/resources/building.svg";
import type { OrgEnhet } from "~/types/api-models";

export const FrontPage = () => {
  const navOrgEnhetId = navToppnodeOrgEnhetId();

  const orgEnhetIder = forsideOrgEnhetIder();

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(navOrgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(navOrgEnhetId),
  });

  const globalStatistikkQuery = useQuery({
    queryKey: getGlobalStatistikk.queryKey(),
    queryFn: () => getGlobalStatistikk.queryFn(),
  });

  const globalStatistikk = globalStatistikkQuery.data;

  const orgEnhet = orgEnhetQuery.data;

  return (
    <ContentWrapper>
      <VStack gap="12">
        <Orghierarki orgEnhetIder={orgEnhetIder} orgEnhet={orgEnhet} />

        <OrgEnhetStatistikk
          gittStatistikk={globalStatistikk}
          header="Nøkkelinfo for hele Nav"
          orgEnhetId={orgEnhet?.id ?? ""}
        />
      </VStack>
    </ContentWrapper>
  );
};

function Orghierarki({
  orgEnhet,
  orgEnhetIder,
}: {
  orgEnhet?: OrgEnhet | undefined;
  orgEnhetIder: string[];
}) {
  if (!orgEnhet) {
    return;
  }

  // TODO: Hente navn på direktorat + linjene automatisk + for-loop?
  return (
    <div>
      <Heading level="2" size="large">
        Organisasjonshierarkiet i Nav
      </Heading>

      <div
        css={css`
          margin-top: var(--a-spacing-6);
          display: grid;
          gap: var(--a-spacing-6);
          grid-template-columns: repeat(4, 1fr);
          justify-content: center;
        `}
      >
        <div
          css={css`
            grid-column: span 2 / 4;
            width: 50%;
            padding-left: 6px;
            padding-right: 6px;
            margin: auto;
          `}
        >
          <OrghierarkiItem
            icon={building}
            label="Arbeids- og velferdsdirektoratet"
            to={`/org/${orgEnhetIder[0]}`}
          />
        </div>

        <div
          css={css`
            grid-row: 2;
          `}
        >
          <OrghierarkiItem
            icon={building}
            label="Arbeids- og tjenestelinjen"
            to={`/org/${orgEnhetIder[1]}`}
          />
        </div>

        <div
          css={css`
            grid-row: 2;
          `}
        >
          <OrghierarkiItem
            icon={building}
            label="Velferdslinjen"
            to={`/org/${orgEnhetIder[2]}`}
          />
        </div>

        <div
          css={css`
            grid-row: 2;
          `}
        >
          <OrghierarkiItem
            icon={building}
            label="Ytelseslinjen"
            to={`/org/${orgEnhetIder[3]}`}
          />
        </div>

        <div
          css={css`
            grid-row: 2;
          `}
        >
          <OrghierarkiItem
            icon={building}
            label="Økonomilinjen"
            to={`/org/${orgEnhetIder[4]}`}
          />
        </div>
      </div>
    </div>
  );
}

export function OrghierarkiItem({
  label,
  to,
  icon,
}: {
  label: string;
  to?: string;
  icon: string;
}) {
  return to ? (
    <SmartLink
      css={css`
        color: #000;

        :hover {
          text-decoration: none;
        }
      `}
      href={to}
    >
      <OrghierarkiItemContent icon={icon} label={label} />
    </SmartLink>
  ) : (
    <OrghierarkiItemContent icon={icon} label={label} />
  );
}

export function OrghierarkiItemContent({
  label,
  icon,
}: {
  label: string;
  icon: string;
}) {
  return (
    <div
      css={css`
        height: 100%;
        background: #fff;
        border: 2px solid #deecef;
        border-radius: 10px;
        align-items: center;
        justify-content: center;
        padding: var(--a-spacing-4);
        text-align: center;

        :hover {
          background: #deecef;
        }
      `}
    >
      <div
        css={css`
          display: flex;
          height: 60px;
          width: 60px;
          background: var(--a-icon-info);
          border-radius: 50%;
          padding: var(--a-spacing-4);
          margin: auto auto var(--a-spacing-4);
        `}
      >
        <img
          alt="orgenhet"
          css={css`
            margin: auto;
            width: 100%;
          `}
          src={icon}
        />
      </div>

      <Label
        css={css`
          font-size: var(--a-font-size-heading-small);
          font-weight: var(--a-font-weight-bold);
          cursor: inherit;
        `}
      >
        {label}
      </Label>
    </div>
  );
}
