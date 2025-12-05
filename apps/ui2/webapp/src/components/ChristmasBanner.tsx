import { css } from "@emotion/react";
import React from "react";

import christmaslights from "~/resources/Christmaslights.svg";
import { isDecember } from "~/utils";

export function ChristmasBanner() {
  if (!isDecember()) {
    return;
  }

  return (
    <div
      css={css`
        width: 100%;
        height: 60px;
        background: var(--nom-bg);
        background-image: url(${christmaslights});
        background-repeat: repeat-x;
        background-size: auto 100%;
        overflow: hidden;
      `}
    />
  );
}
