import { css } from "@emotion/react";
import { Heading, Label, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React from "react";

import { getGlobalStatistikk } from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { OrgEnhetStatistikk } from "~/components/statistikk/OrgEnhetStatistikk";
import {
  useDirektoratOrgEnhetId,
  useLinjeenhetOrgEnhetIder,
  useNavToppnodeOrgEnhetId,
} from "~/customUtils/environmentUtils";
import building from "~/resources/building.svg";

export const FrontPage = () => {
  const globalStatistikkQuery = useQuery({
    queryKey: getGlobalStatistikk.queryKey(),
    queryFn: () => getGlobalStatistikk.queryFn(),
  });

  const globalStatistikk = globalStatistikkQuery.data;

  return (
    <ContentWrapper>
      <VStack gap="space-48">
        <Orghierarki />

        <OrgEnhetStatistikk
          gittStatistikk={globalStatistikk}
          header="Nøkkelinfo for hele Nav"
          orgEnhetId={useNavToppnodeOrgEnhetId()}
        />
      </VStack>
    </ContentWrapper>
  );
};

function Orghierarki() {
  const direktoratId = useDirektoratOrgEnhetId();

  const linjeenhetIder = useLinjeenhetOrgEnhetIder();

  return (
    <div>
      <Heading level="2" size="large">
        Organisasjonshierarkiet i Nav
      </Heading>

      <div
        css={css`
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          justify-content: center;
          gap: var(--ax-space-24);
          margin-top: var(--ax-space-24);
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
            label="Arbeids- og velferdsdirektoratet"
            to={`/org/${direktoratId}`}
          />
        </div>

        {linjeenhetIder &&
          linjeenhetIder.slice(0, 4).map((enhetId, index) => (
            <div
              key={index}
              css={css`
                grid-row: 2;
              `}
            >
              <OrghierarkiItem
                label={linjeenhetNavn[index]}
                to={`/org/${enhetId}`}
              />
            </div>
          ))}
      </div>
    </div>
  );
}

function OrghierarkiItem({ label, to }: { label: string; to: string }) {
  return (
    <SmartLink
      css={css`
        color: #000;

        :hover {
          text-decoration: none;
        }
      `}
      href={to}
    >
      <div
        css={css`
          height: 100%;
          background: #fff;
          border: 2px solid #deecef;
          border-radius: 10px;
          align-items: center;
          justify-content: center;
          padding: var(--ax-space-16);
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
            background: var(--nom-primary);
            border-radius: 50%;
            padding: var(--ax-space-16);
            margin: auto auto var(--ax-space-16);
          `}
        >
          <img
            alt="orgenhet"
            css={css`
              margin: auto;
              width: 100%;
            `}
            src={building}
          />
        </div>

        <Label
          css={css`
            font-size: var(--ax-font-size-heading-small);
            font-weight: var(--ax-font-weight-bold);
            cursor: inherit;
          `}
        >
          {label}
        </Label>
      </div>
    </SmartLink>
  );
}

const linjeenhetNavn = [
  "Arbeids- og tjenestelinjen",
  "Velferdslinjen",
  "Ytelseslinjen",
  "Økonomilinjen",
];
