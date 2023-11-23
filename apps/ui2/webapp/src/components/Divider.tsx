import { css } from "@emotion/react";
import React from "react";

export function Divider({ size = "large" }: { size?: "large" | "small" }) {
  return (
    <div
      css={[
        css`
          height: 1px;
          margin: var(--a-spacing-8) 0;
          background-color: var(--a-gray-400);
        `,
        size === "large" &&
          css`
            margin: var(--a-spacing-8) 0;
          `,
        size === "small" &&
          css`
            margin: var(--a-spacing-6) 0;
          `,
      ]}
    />
  );
}
