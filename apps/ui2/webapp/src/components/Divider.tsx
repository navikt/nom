import { css } from "@emotion/react";
import React from "react";

export function Divider({
  size = "large",
  noMargins = false,
  className,
}: {
  size?: "large" | "small";
  noMargins?: boolean;
  className?: string;
}) {
  return (
    <div
      className={className}
      css={[
        css`
          height: 1px;
          margin: var(--ax-space-32) 0;
          background-color: var(--ax-border-neutral);
        `,
        size === "large" &&
          css`
            margin: var(--ax-space-32) 0;
          `,
        size === "small" &&
          css`
            margin: var(--ax-space-24) 0;
          `,
        noMargins &&
          css`
            margin: 0;
          `,
      ]}
    />
  );
}
