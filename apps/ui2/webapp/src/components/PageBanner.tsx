import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";

import { mediaQueryContentWidth } from "~/resources/NomStyles";

export function PageBanner({ text }: { text: string }) {
  return (
    <div
      css={css`
        width: 100%;
        height: 78px;
        background: var(--a-lightblue-800);
        color: var(--a-white);
        display: flex;
      `}
    >
      <Heading
        css={css(
          mediaQueryContentWidth,
          css`
            align-self: center;
            margin: auto;
            width: 1100px;
          `
        )}
        level="1"
        size="large"
      >
        {text}
      </Heading>
    </div>
  );
}
