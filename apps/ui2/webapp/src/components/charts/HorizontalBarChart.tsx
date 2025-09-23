import { css } from "@emotion/react";
import { PersonFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Heading, HelpText, HGrid } from "@navikt/ds-react";
import maxBy from "lodash/maxBy";
import type { ReactNode } from "react";
import React from "react";

import { formatDisplayNumber } from "~/utils";

export function HorizontalBarChart({
  rows,
  total,
  title,
  helpText,
}: {
  rows: ChartRow[];
  total?: number;
  title?: string;
  helpText?: string | ReactNode;
}) {
  const data = normalizeData(rows);

  return (
    <div
      css={css`
        width: 100%;
        line-height: 1.5rem;
      `}
    >
      {(!!title || !!helpText) && (
        <div
          css={css`
            display: flex;
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
                justify-content: end;
                display: flex;
              `}
            >
              <HelpText>{helpText}</HelpText>
            </div>
          )}
        </div>
      )}

      {!!total && (
        <div
          css={css`
            display: flex;
            align-items: center;
            font-size: var(--a-font-size-large);
            margin-bottom: var(--a-spacing-4);
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

          <div
            css={css`
              display: flex;
            `}
          >
            <BodyShort
              css={css`
                margin-left: var(--a-spacing-2);
                font-size: inherit;
              `}
            >
              Totalt:
            </BodyShort>
            <span
              css={css`
                margin-left: var(--a-spacing-4);
              `}
            >
              <b>{formatDisplayNumber(total)}</b>
            </span>
          </div>
        </div>
      )}

      <HGrid
        columns="max-content auto max-content"
        css={css`
          width: 100%;
        `}
        gap="4"
      >
        {data.map((row, index) => (
          <BarChartRow key={index} row={row} />
        ))}
      </HGrid>
    </div>
  );
}

function BarChartRow({ row }: { row: PercentageChartRow }) {
  return (
    <>
      <BodyShort>{row.label}</BodyShort>
      <span
        css={css`
          background: var(--a-deepblue-500);
          width: ${row.percentage}%;
          border-radius: 5px;
        `}
      />
      <BodyShort
        css={css`
          display: flex;
          justify-content: end;
        `}
      >
        {formatDisplayNumber(row.value)}
      </BodyShort>
    </>
  );
}

function normalize(value: number, max: number, min: number) {
  const normalizedValue = (value - min) / (max - min);

  return normalizedValue * 100;
}

function normalizeData(rows: ChartRow[]): PercentageChartRow[] {
  const maxValue = maxBy(rows, "value")?.value ?? 0;

  return rows.map((row) => ({
    ...row,
    percentage: normalize(row.value, maxValue, 0),
  }));
}

export type ChartRow = {
  label: string;
  value: number;
};

type PercentageChartRow = {
  label: string;
  value: number;
  percentage: number;
};
