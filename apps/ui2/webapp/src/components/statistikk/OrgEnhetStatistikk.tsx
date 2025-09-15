import { css } from "@emotion/react";
import { Buildings3FillIcon } from "@navikt/aksel-icons";
import {
  BodyLong,
  BodyShort,
  Heading,
  HelpText,
  HStack,
  Loader,
  ReadMore,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React, { type ReactNode } from "react";

import {
  getStatistikkForOrgEnhet,
  getUnderliggendeOrgenheter,
} from "~/api/react-query-endpoints";
import type { ChartRow } from "~/components/charts/HorizontalBarChart";
import { HorizontalBarChart } from "~/components/charts/HorizontalBarChart";
import { SmartLink } from "~/components/navigation/SmartLink";
import type { GlobalStatistikkResponse, OrgEnhet } from "~/types/api-models";
import { isInDevelopment } from "~/utils";

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

  if (
    !orgEnhetStatistikkQuery.isLoading &&
    !visMedarbeidereBarChart &&
    !visOrgEnheterStatistikk
  ) {
    return;
  }

  const inDevelopment = isInDevelopment();

  const tekAvdNavId = inDevelopment ? "je104d" : "te627k";

  const enheterUnderTekAvdQuery = useQuery({
    queryKey: getUnderliggendeOrgenheter.queryKey(tekAvdNavId),
    queryFn: () => getUnderliggendeOrgenheter.queryFn(tekAvdNavId),
  });

  const enheterUnderTekAvd = enheterUnderTekAvdQuery.data;

  const underTeknologiavdelingen =
    orgEnhet.id === tekAvdNavId || enheterUnderTekAvd?.includes(orgEnhet.id);

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
      <MedarbeidereBarChartGridWrapper
        statistikk={medarbeidereStatistikk}
        teknologiavdelingen={underTeknologiavdelingen}
      />

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
  teknologiavdelingen,
}: {
  statistikk: MedarbeidereStatistikk;
  teknologiavdelingen?: boolean;
}) {
  if (!statistikk || statistikk.totalt === 0) {
    return;
  }

  return (
    <GridItem standalone>
      <MedarbeidereBarChart
        statistikk={statistikk}
        teknologiavdelingen={teknologiavdelingen}
      />
    </GridItem>
  );
}

function MedarbeidereBarChart({
  statistikk,
  teknologiavdelingen = false,
}: {
  statistikk: MedarbeidereStatistikk;
  teknologiavdelingen?: boolean;
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
      helpText={
        <VStack gap="2">
          <BodyLong>
            Alle medarbeidere med en aktiv Nav-ident telles én gang.{" "}
            {teknologiavdelingen
              ? "Tallet for eksterne viser antall lånt inn via Teknologiavdelingens avtaler. Teamkatalogen viser hvor disse faktisk jobber."
              : "Eksterne medarbeidere som får utbetalinger, som tolker og lese- og sekretærhjelper, telles også."}{" "}
            Roboter og andre ikke-menneskelige identer telles ikke. Du kan lese
            mer om statistikken i NOM på Navet.
          </BodyLong>
          <BodyLong>
            <SmartLink
              href="https://navno.sharepoint.com/sites/intranett-organisasjon/SitePages/Statistikk-i-NOM.aspx?csf=1&web=1&e=yJDbFQ&CID=9e0a51fa-2cdb-4e98-ac05-32baaac3525c"
              inline
            >
              Statistikk i NOM på Navet
            </SmartLink>
          </BodyLong>
        </VStack>
      }
      rows={medarbeidereData}
      title="Medarbeidere:"
      total={statistikk.totalt}
    />
  );
}

function OrgEnheterStatistikkGridWrapper({
  statistikk,
}: {
  statistikk: GlobalStatistikkResponse | undefined;
}) {
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
      label: "Fylkesenheter:",
      value: antallFylkekontor,
    });
  }
  if (antallNavKontorer > 0) {
    statistikkItems.push({
      label: "Nav-kontorer:",
      value: antallNavKontorer,
    });
  }
  if (antallHjelpemidlerOgTilrettelegging > 0) {
    statistikkItems.push({
      label: "Hjelpemidler og tilrettelegging:",
      value: antallHjelpemidlerOgTilrettelegging,
    });
  }
  if (antallKlageinstanser > 0) {
    statistikkItems.push({
      label: "Klageinstanser:",
      value: antallKlageinstanser,
    });
  }
  if (antallKontrollKonrollenheter > 0) {
    statistikkItems.push({
      label: "Kontrollenheter:",
      value: antallKontrollKonrollenheter,
    });
  }
  if (antallNavKontaktsenter > 0) {
    statistikkItems.push({
      label: "Nav kontaktsenter:",
      value: antallNavKontaktsenter,
    });
  }
  if (antallNavArbeidOgYtelser > 0) {
    statistikkItems.push({
      label: "Nav arbeid og ytelser:",
      value: antallNavArbeidOgYtelser,
    });
  }
  if (antallNavFamilieOgPensjonsytelser > 0) {
    statistikkItems.push({
      label: "Nav familie- og pensjonsytelser:",
      value: antallNavFamilieOgPensjonsytelser,
    });
  }
  if (antallLovnoder > 0) {
    statistikkItems.push({
      label: "Organisasjonsenheter:",
      value: antallLovnoder,
    });
  }

  if (statistikkItems.length === 0) {
    return;
  }

  return (
    <GridItem standalone>
      <OrgEnheterStatistikk
        helpText={
          <VStack gap="2">
            <BodyLong>
              Vi teller organisasjonsenheter basert på hvilken type enhet det
              er, eksempelvis Nav-kontor, kontaktsenter, etc. Du kan lese mer om
              statistikken, telleregler og årsaker til avvik i NOM på Navet.
            </BodyLong>
            <BodyLong>
              <SmartLink
                href="https://navno.sharepoint.com/sites/intranett-organisasjon/SitePages/Statistikk-i-NOM.aspx?csf=1&web=1&e=yJDbFQ&CID=9e0a51fa-2cdb-4e98-ac05-32baaac3525c"
                inline
              >
                Statistikk i NOM på Navet
              </SmartLink>
            </BodyLong>
          </VStack>
        }
        statistikk={statistikkItems}
        title="Organisasjonsenheter:"
      />
    </GridItem>
  );
}

function OrgEnheterStatistikk({
  title,
  helpText,
  statistikk,
}: {
  title?: string;
  helpText?: string | ReactNode;
  statistikk: OrgEnhetStatistikkItem[];
}) {
  return (
    <div>
      {(!!title || !!helpText) && (
        <div
          css={css`
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: var(--a-spacing-6);
          `}
        >
          {!!title && (
            <Heading level="3" size="small">
              {title}
            </Heading>
          )}

          {!!helpText && (
            <div
              css={css`
                width: 100%;
              `}
            >
              <HelpText
                css={css`
                  justify-self: end;
                `}
              >
                {helpText}
              </HelpText>
            </div>
          )}
        </div>
      )}

      <HStack gap="4">
        {statistikk.map((statistikkItem) => (
          <OrgEnhetStatistikkVerdi
            helpText={statistikkItem.helpText}
            icon={
              <div
                css={css`
                  display: flex;
                  align-items: center;
                `}
              >
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
              </div>
            }
            key={statistikkItem.label}
            label={statistikkItem.label}
            value={statistikkItem.value}
          />
        ))}
      </HStack>
    </div>
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
          align-items: center;
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
