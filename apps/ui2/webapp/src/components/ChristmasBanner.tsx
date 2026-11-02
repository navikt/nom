import { css } from "@emotion/react";
import React from "react";

import christmaslights from "~/resources/Christmaslights.svg";
import { isDecember } from "~/utils";

export const christmasBannerHeight = "60px";

export function ChristmasBanner() {
  if (!isDecember()) {
    return;
  }

  return (
    <div
      css={css`
        width: 100%;
        height: ${christmasBannerHeight};
        background: var(--nom-bg);
        background-image: url(${christmaslights});
        background-repeat: repeat-x;
        background-size: auto 100%;
        overflow: hidden;
      `}
    />
  );
}
