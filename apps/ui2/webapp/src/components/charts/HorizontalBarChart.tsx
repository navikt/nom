import { css } from "@emotion/react";
import { Heading, HelpText } from "@navikt/ds-react";
import maxBy from "lodash/maxBy";
import type { ReactNode } from "react";
import { useEffect, useRef, useState } from "react";

import { formatDisplayNumber } from "~/utils";

export type ChartRow = {
  label: string;
  value: number;
  percentage?: number;
};

export function HorizontalBarChart({
  rows,
  title,
  helpText,
}: {
  rows: ChartRow[];
  title: string | ReactNode;
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
        border-radius: 10px;
        line-height: 1.5rem;
      `}
    >
      <div
        css={css`
          display: flex;
          justify-content: space-between;
        `}
      >
        {typeof title === "string" ? (
          <Heading level="2" size="medium">
            {title}
          </Heading>
        ) : (
          <>{title}</>
        )}

        {!!helpText && <HelpText>{helpText}</HelpText>}
      </div>

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
