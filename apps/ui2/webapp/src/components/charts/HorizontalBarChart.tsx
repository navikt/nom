import { css } from "@emotion/react";
import { PersonFillIcon } from "@navikt/aksel-icons";
import { BodyShort, Heading, HelpText } from "@navikt/ds-react";
import maxBy from "lodash/maxBy";
import type { ReactNode } from "react";
import React from "react";
import { useEffect, useRef, useState } from "react";

import { formatDisplayNumber } from "~/utils";

export type ChartRow = {
  label: string;
  value: number;
  percentage?: number;
};

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
  const maxValueRow = maxBy(data, "normalizedPercentage");

  const widestBarReference = useRef<HTMLSpanElement | null>(null);
  const [widestBarWidth, setWidestBarWidth] = useState(0);

  useEffect(() => {
    if (widestBarReference.current) {
      const width = widestBarReference.current.getBoundingClientRect().width;
      setWidestBarWidth(width);
    }
  }, [data]);

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

      <div
        css={css`
          gap: 1rem;
          display: grid;
          grid-template-columns: max-content 1fr;
          margin-top: 1rem;
        `}
      >
        {data.map((row) => {
          const isWidest =
            row.normalizedPercentage === maxValueRow?.normalizedPercentage;

          if (!isWidest && widestBarWidth === 0) return;

          const barWidth = isWidest
            ? "100%"
            : `${widestBarWidth * (row.value / (maxValueRow?.value ?? 1))}px`;

          return (
            <div
              css={css`
                display: contents;

                a {
                  color: var(--a-gray-900);
                  text-decoration: none;

                  &:hover {
                    text-decoration: underline;
                  }
                }
              `}
              key={row.label}
            >
              <span>{row.label}</span>

              <div
                css={css`
                  display: inline-flex;
                  gap: 1rem;
                `}
              >
                <span
                  css={css`
                    background: var(--a-deepblue-500);
                    width: ${barWidth};
                    border-radius: 5px;
                  `}
                  ref={isWidest ? widestBarReference : null}
                />
                <span
                  css={css`
                    white-space: nowrap;
                  `}
                >
                  {formatDisplayNumber(row.value)}&nbsp;
                  {row.percentage === undefined ? "" : `(${row.percentage}%)`}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function normalize(value: number, max: number, min: number) {
  const normalizedValue = (value - min) / (max - min);

  return normalizedValue * 100;
}

function normalizeData(rows: ChartRow[]) {
  const maxValue = maxBy(rows, "value")?.value ?? 0;

  return rows.map((row) => ({
    ...row,
    normalizedPercentage: normalize(row.value, maxValue, 0),
  }));
}
