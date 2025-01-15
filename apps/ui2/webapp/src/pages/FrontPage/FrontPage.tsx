import { css } from "@emotion/react";
import { Buildings3FillIcon, PersonFillIcon } from "@navikt/aksel-icons";
import {
  BodyLong,
  BodyShort,
  Heading,
  HStack,
  Label,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React from "react";

import { getGlobalStatistikk, getOrgEnhet } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import type { ChartRow } from "~/components/charts/HorizontalBarChart";
import { HorizontalBarChart } from "~/components/charts/HorizontalBarChart";
import ContentWrapper from "~/components/ContentWrapper";
import building from "~/resources/building.svg";
import type { GlobalStatistikkResponse, OrgEnhet } from "~/types/api-models";
import { formatDisplayNumber, isInDevelopment } from "~/utils";

export const FrontPage = () => {
  const inDevelopment = isInDevelopment();

  const hasAdminAccess = useHasAdminAccess();

  const orgEnhetId = inDevelopment ? "sa312u" : "py437s";

  const developmentLenker = ["fa965e", "me177r", "su645c", "hu797w", "tu734a"];
  const productionLenker = ["bu431e", "ry630r", "ky110z", "se232x", "ne994m"];

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId),
  });

  const globalStatistikkQuery = useQuery({
    queryKey: getGlobalStatistikk.queryKey(),
    queryFn: () => getGlobalStatistikk.queryFn(),
  });

  const globalStatistikk = globalStatistikkQuery.data;

  const orgEnhet = orgEnhetQuery.data;

  return (
    <ContentWrapper>
      <div
        css={css`
          margin-bottom: var(--a-spacing-10);
        `}
      >
        <Heading
          css={css`
            justify-self: center;
          `}
          level="1"
          size="xlarge"
        >
          Velkommen til NOM!
        </Heading>

        <BodyLong
          css={css`
            text-align: center;
            width: 90%;
            margin: var(--a-spacing-2) auto auto;
          `}
          size="medium"
        >
          NAV Organisasjonsmaster (NOM) skal bli masterkilde for medarbeidere
          (statlige ansatte, kommunalt ansatte og eksterne) og
          organisasjonsenheter i NAV. Dette innebærer at NOM skal inneholde
          grunnleggende opplysninger om de som jobber i NAV,
          organisasjonshierarkiet, og tilknytningen mellom medarbeidere og
          organisasjonsenheter. Som masterkilde skal NOM forsyne andre
          applikasjoner med data. NOM er under utvikling.
        </BodyLong>
      </div>

      <VStack gap="8">
        {hasAdminAccess && (
          <OrgEnhetStatistikk
            orgEnhet={orgEnhet}
            statistikk={globalStatistikk}
          />
        )}

        <Orghierarki
          lenker={isInDevelopment() ? developmentLenker : productionLenker}
          orgEnhet={orgEnhet}
        />
      </VStack>
    </ContentWrapper>
  );
};

function OrgEnhetStatistikk({
  orgEnhet,
  statistikk,
}: {
  orgEnhet: OrgEnhet | undefined;
  statistikk: GlobalStatistikkResponse | undefined;
}) {
  if (!orgEnhet || !statistikk) {
    return;
  }

  const ressursStatistikk = statistikk?.ressursStatistikk;

  const MedarbeidereStatistikk: MedarbeidereStatistikk = {
    statlige: ressursStatistikk?.antallStatligeRessurser ?? 0,
    kommunale: ressursStatistikk?.antallKommunaleRessurser ?? 0,
    eksterne: ressursStatistikk?.antallEksterneRessurser ?? 0,
    totalt: ressursStatistikk?.antallAktiveRessurser ?? 0,
  };

  return (
    <div>
      <Heading level="2" size="large">
        Nøkkelinfo for {orgEnhet.navn}
      </Heading>

      <div
        css={css`
          margin-top: var(--a-spacing-4);
          display: grid;
          gap: var(--a-spacing-6);
          grid-template-columns: repeat(2, 1fr);
        `}
      >
        <GridItem rows={2}>
          <MedarbeidereBarChart statistikk={MedarbeidereStatistikk} />
        </GridItem>

        <GridItem>
          <OrgEnheterStatistikk statistikk={statistikk} />
        </GridItem>
      </div>
    </div>
  );
}

function GridItem({
  columns = 1,
  rows = 1,
  children,
}: {
  columns?: number;
  rows?: number;
  children: ReactNode;
}) {
  return (
    <div
      css={css`
        width: 100%;
        display: flex;
        background: #deecef;
        padding: var(--a-spacing-6);
        border-radius: 10px;
        border: 2px solid var(--a-icon-info);
        grid-column: span ${columns};
        grid-row: span ${rows};
        align-items: center;
      `}
    >
      {children}
    </div>
  );
}

function Orghierarki({
  orgEnhet,
  lenker,
}: {
  orgEnhet?: OrgEnhet | undefined;
  lenker: string[];
}) {
  if (!orgEnhet) {
    return <></>;
  }

  return (
    <div>
      <Heading level="2" size="large">
        Organisasjonshierarkiet i {orgEnhet.navn}
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
            to={`/org/${lenker[0]}`}
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
            to={`/org/${lenker[1]}`}
          />
        </div>

        <div
          css={css`
            grid-row: 2;
          `}
        >
          <OrghierarkiItem
            icon={building}
            label="Klage- og ankelinjen"
            to={`/org/${lenker[2]}`}
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
            to={`/org/${lenker[3]}`}
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
            to={`/org/${lenker[4]}`}
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
    <Link
      css={css`
        text-decoration: none;
        color: #000;
      `}
      to={to}
    >
      <OrghierarkiItemContent icon={icon} label={label} />
    </Link>
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

function MedarbeidereBarChart({
  statistikk,
}: {
  statistikk: MedarbeidereStatistikk;
}) {
  const medarbeidereData: ChartRow[] = [
    {
      label: "Statlige",
      value: statistikk.statlige,
    },
    {
      label: "Kommunale",
      value: statistikk.kommunale,
    },
    {
      label: "Eksterne",
      value: statistikk.eksterne,
    },
  ];

  return (
    <HorizontalBarChart
      rows={medarbeidereData}
      title={
        <div
          css={css`
            display: flex;
            align-items: center;
          `}
        >
          <div
            css={css`
              background: var(--nom-secondary-hover);
              border-radius: 50%;
              height: 40px;
              width: 40px;
              justify-items: center;
              align-content: center;
            `}
          >
            <PersonFillIcon
              css={css`
                background: #fff;
                border-radius: 50%;
                width: 37px;
                height: 37px;
                padding: var(--a-spacing-1);
              `}
              fontSize="1.75rem"
              title="a11y-title"
            />
          </div>

          <BodyShort
            css={css`
              margin-left: var(--a-spacing-4);
            `}
            size="large"
          >
            Antall medarbeidere:
          </BodyShort>
          <span
            css={css`
              margin-left: var(--a-spacing-4);
            `}
          >
            <b>{formatDisplayNumber(statistikk.totalt)}</b>
          </span>
        </div>
      }
    />
  );
}

function OrgEnheterStatistikk({
  statistikk,
}: {
  statistikk: GlobalStatistikkResponse;
}) {
  if (!statistikk) {
    return;
  }

  return (
    <HStack gap="6">
      {statistikk.antallNavKontorer && statistikk.antallNavKontorer > 0 && (
        <StatistikkVerdiIcon
          icon={
            <Buildings3FillIcon
              css={css`
                background: #fff;
                border-radius: 50%;
                width: 37px;
                height: 37px;
                padding: var(--a-spacing-1);
              `}
              fontSize="1.75rem"
              title="a11y-title"
            />
          }
          label="Antall Nav-kontorer"
          value={statistikk.antallNavKontorer}
        />
      )}

      {statistikk.antallFylkekontor && statistikk.antallFylkekontor > 0 && (
        <StatistikkVerdiIcon
          icon={
            <Buildings3FillIcon
              css={css`
                background: #fff;
                border-radius: 50%;
                width: 37px;
                height: 37px;
                padding: var(--a-spacing-1);
              `}
              fontSize="1.75rem"
              title="a11y-title"
            />
          }
          label="Antall fylkeskontorer"
          value={statistikk.antallFylkekontor}
        />
      )}
    </HStack>
  );
}

function StatistikkVerdiIcon({
  label,
  value,
  icon,
}: {
  label: string;
  value: string | number;
  icon?: string | ReactNode;
}) {
  return (
    <div
      css={css`
        display: flex;
        align-items: center;
      `}
    >
      {icon && (
        <div
          css={css`
            background: var(--nom-secondary-hover);
            border-radius: 50%;
            height: 40px;
            width: 40px;
            justify-items: center;
            align-content: center;
          `}
        >
          {icon}
        </div>
      )}

      <BodyShort
        css={css`
          margin-left: var(--a-spacing-4);
        `}
        size="large"
      >
        {`${label}${label.endsWith(":") ? "" : ":"}`}
      </BodyShort>

      <span
        css={css`
          margin-left: var(--a-spacing-4);
        `}
      >
        <b>{value.toString()}</b>
      </span>
    </div>
  );
}

export type MedarbeidereStatistikk = {
  statlige: number;
  kommunale: number;
  eksterne: number;
  totalt: number;
};
