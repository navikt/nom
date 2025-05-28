import { css } from "@emotion/react";
import { Buildings3FillIcon, PersonFillIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Heading,
  HelpText,
  HStack,
  Label,
  Loader,
  ReadMore,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React from "react";

import {
  getGlobalStatistikk,
  getOrgEnhet,
  getStatistikkForOrgEnhet,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import type { ChartRow } from "~/components/charts/HorizontalBarChart";
import { HorizontalBarChart } from "~/components/charts/HorizontalBarChart";
import ContentWrapper from "~/components/ContentWrapper";
import building from "~/resources/building.svg";
import type { GlobalStatistikkResponse, OrgEnhet } from "~/types/api-models";
import { formatDisplayNumber, isInDevelopment } from "~/utils";

export const FrontPage = () => {
  const inDevelopment = isInDevelopment();

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
      <VStack gap="12">
        <Orghierarki
          lenker={isInDevelopment() ? developmentLenker : productionLenker}
          orgEnhet={orgEnhet}
        />

        <OrgEnhetStatistikk
          gittStatistikk={globalStatistikk}
          header="Nøkkelinfo for hele Nav"
          orgEnhet={orgEnhet}
        />
      </VStack>
    </ContentWrapper>
  );
};

export function OrgEnhetStatistikk({
  orgEnhet,
  gittStatistikk,
  header,
  expandable = false,
  className,
}: {
  orgEnhet: OrgEnhet | undefined;
  gittStatistikk?: GlobalStatistikkResponse | undefined;
  header?: string;
  expandable?: boolean;
  className?: string;
}) {
  if (!orgEnhet) {
    return;
  }

  const orgEnhetStatistikkQuery = useQuery({
    queryKey: getStatistikkForOrgEnhet.queryKey(orgEnhet.id),
    queryFn: () => getStatistikkForOrgEnhet.queryFn(orgEnhet.id),
    enabled: !gittStatistikk,
  });

  const statistikk = gittStatistikk ?? orgEnhetStatistikkQuery.data;

  const ressursStatistikk = statistikk?.ressursStatistikk;

  const medarbeidereStatistikk: MedarbeidereStatistikk = {
    statlige: ressursStatistikk?.antallStatligeRessurser ?? 0,
    kommunale: ressursStatistikk?.antallKommunaleRessurser ?? 0,
    eksterne: ressursStatistikk?.antallEksterneRessurser ?? 0,
    totalt: ressursStatistikk?.antallAktiveRessurser ?? 0,
  };

  const isLoading = orgEnhetStatistikkQuery.isLoading && !gittStatistikk;

  const visMedarbeidereBarChart = medarbeidereStatistikk.totalt > 0;

  const visOrgEnheterStatistikk = statistikk
    ? Object.entries(statistikk).some(
        ([_, value]) => typeof value === "number" && value > 0,
      )
    : false;

  if (!visMedarbeidereBarChart && !visOrgEnheterStatistikk) {
    return;
  }

  const innerContent = isLoading ? (
    <div
      css={css`
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--a-spacing-2);
      `}
    >
      <Loader size="3xlarge" />
      <BodyShort size="large">Genererer statistikk...</BodyShort>
    </div>
  ) : (
    <div
      css={css`
        margin-top: var(--a-spacing-4);
        display: grid;
        gap: var(--a-spacing-6);
        grid-template-columns: repeat(2, 1fr);
      `}
    >
      <MedarbeidereBarChartGridWrapper statistikk={medarbeidereStatistikk} />

      <OrgEnheterStatistikkGridWrapper statistikk={statistikk} />
    </div>
  );

  if (expandable) {
    return (
      <ReadMore
        className={className}
        css={css`
          .navds-read-more__button {
            color: var(--a-text-default);
            font-size: var(--a-font-size-heading-medium);
            font-weight: var(--a-font-weight-bold);
          }

          .navds-read-more__content {
            margin-left: 0;
            padding-left: 0;
            border-left: none;
          }
        `}
        defaultOpen
        header="Statistikk"
      >
        {innerContent}
      </ReadMore>
    );
  }

  return (
    <div className={className}>
      {!!header && (
        <Heading level="2" size="medium">
          {header}
        </Heading>
      )}
      {innerContent}
    </div>
  );
}

function GridItem({
  columns = 1,
  rows = 1,
  standalone = false,
  children,
}: {
  columns?: number;
  rows?: number;
  standalone?: boolean;
  children: React.ReactNode;
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
        align-items: center;
        ${standalone
          ? `height: fit-content;`
          : `grid-column: span ${columns}; grid-row: span ${rows};`}
      `}
    >
      {children}
    </div>
  );
}

function MedarbeidereBarChartGridWrapper({
  statistikk,
}: {
  statistikk: MedarbeidereStatistikk;
}) {
  if (!statistikk || statistikk.totalt === 0) {
    return;
  }

  return (
    <GridItem standalone>
      <MedarbeidereBarChart statistikk={statistikk} />
    </GridItem>
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
  ].filter((sektor) => sektor.value !== 0);

  return (
    <HorizontalBarChart
      rows={medarbeidereData}
      title={
        <div
          css={css`
            display: flex;
            justify-content: space-between;
          `}
        >
          <div
            css={css`
              display: flex;
              align-items: center;
              font-size: var(--a-font-size-large);
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
                font-size: inherit;
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
        </div>
      }
    />
  );
}

function OrgEnheterStatistikkGridWrapper({
  statistikk,
}: {
  statistikk: GlobalStatistikkResponse | undefined;
}) {
  const hasAdminAccess = useHasAdminAccess();

  const antallNavKontorer = statistikk?.antallNavKontorer ?? 0;
  const antallFylkekontor = statistikk?.antallFylkekontor ?? 0;
  const antallLovnoder = statistikk?.antallLovNoder ?? 0;
  const antallHjelpemidlerOgTilrettelegging =
    statistikk?.antallHjelpemidlerOgTilrettelegging ?? 0;
  const antallKlageinstanser = statistikk?.antallKlageinstanser ?? 0;
  const antallKontrollKonrollenheter =
    statistikk?.antallKontrollKonrollenheter ?? 0;
  const antallNavKontaktsenter = statistikk?.antallNavKontaktsenter ?? 0;
  const antallNavArbeidOgYtelser = statistikk?.antallNavArbeidOgYtelser ?? 0;
  const antallNavFamilieOgPensjonsytelser =
    statistikk?.antallNavFamilieOgPensjonsytelser ?? 0;

  const statistikkItems: OrgEnhetStatistikkItem[] = [];

  if (antallFylkekontor > 0) {
    statistikkItems.push({
      label: "Antall fylkesenheter:",
      value: antallFylkekontor,
      helpText:
        "Antall Nav-regioner. Navs regioner avviker noe fra den administrative inndelingen av landet i fylker.",
    });
  }
  if (antallNavKontorer > 0) {
    statistikkItems.push({
      label: "Antall Nav-kontor:",
      value: antallNavKontorer,
      helpText:
        "Antallet kan avvike noe blant annet fordi det er bevegelser i start/slutt-dato for Nav-kontor ifm. vertskommunesamarbeid.",
    });
  }
  if (antallLovnoder > 0 && hasAdminAccess) {
    statistikkItems.push({
      label: "Organisasjonsenheter:",
      value: antallLovnoder,
      helpText:
        "Antall enheter der det er registrert medarbeidere. På et Nav-kontor telles antall avdelinger.",
    });
  }
  if (antallHjelpemidlerOgTilrettelegging > 0 && hasAdminAccess) {
    statistikkItems.push({
      label: "Hjelpemidler og tilrettelegging:",
      value: antallHjelpemidlerOgTilrettelegging,
      helpText: "Hjelpetekst",
    });
  }
  if (antallKlageinstanser > 0 && hasAdminAccess) {
    statistikkItems.push({
      label: "Antall klageinstanser:",
      value: antallKlageinstanser,
      helpText: "Hjelpetekst",
    });
  }
  if (antallKontrollKonrollenheter > 0 && hasAdminAccess) {
    statistikkItems.push({
      label: "Kontrollenheter:",
      value: antallKontrollKonrollenheter,
      helpText: "Hjelpetekst",
    });
  }
  if (antallNavKontaktsenter > 0 && hasAdminAccess) {
    statistikkItems.push({
      label: "Nav Kontaktsenter:",
      value: antallNavKontaktsenter,
      helpText: "Hjelpetekst",
    });
  }
  if (antallNavArbeidOgYtelser > 0 && hasAdminAccess) {
    statistikkItems.push({
      label: "Nav Arbeid og ytelser:",
      value: antallNavArbeidOgYtelser,
      helpText: "Hjelpetekst",
    });
  }
  if (antallNavFamilieOgPensjonsytelser > 0 && hasAdminAccess) {
    statistikkItems.push({
      label: "Nav Familie og pensjonsytelser:",
      value: antallNavFamilieOgPensjonsytelser,
      helpText: "Hjelpetekst",
    });
  }

  if (statistikkItems.length === 0) {
    return;
  }

  return (
    <GridItem standalone>
      <OrgEnheterStatistikk statistikk={statistikkItems} />
    </GridItem>
  );
}

function OrgEnheterStatistikk({
  statistikk,
}: {
  statistikk: OrgEnhetStatistikkItem[];
}) {
  return (
    <HStack gap="4">
      {statistikk.map((statistikkItem) => (
        <OrgEnhetStatistikkVerdi
          helpText={statistikkItem.helpText}
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
          key={statistikkItem.label}
          label={statistikkItem.label}
          value={statistikkItem.value}
        />
      ))}
    </HStack>
  );
}

function OrgEnhetStatistikkVerdi({
  label,
  value,
  icon,
  helpText,
}: {
  label: string;
  value: string | number;
  icon?: string | ReactNode;
  helpText?: string;
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

      <div
        css={css`
          font-size: var(--a-font-size-large);
          display: flex;
        `}
      >
        <BodyShort
          css={css`
            margin-left: var(--a-spacing-4);
            font-size: inherit;
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

      {!!helpText && (
        <HelpText
          css={css`
            margin-left: var(--a-spacing-4);
          `}
        >
          {helpText}
        </HelpText>
      )}
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
        color: #000;

        :hover {
          text-decoration: none;
        }
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

export type MedarbeidereStatistikk = {
  statlige: number;
  kommunale: number;
  eksterne: number;
  totalt: number;
};

export type OrgEnhetStatistikkItem = {
  label: string;
  value: number;
  icon?: string;
  helpText?: string;
};
