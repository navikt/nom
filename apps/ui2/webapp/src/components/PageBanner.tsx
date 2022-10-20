import { Heading } from "@navikt/ds-react";
import { mediaQueryContentWidth } from "../resources/NomStyles";
import { css } from "@emotion/css";

export function PageBanner({ text }: { text: string }) {
  return (
    <div
      className={css`
        width: 100%;
        height: 78px;
        background: var(--nom-primary);
        color: var(--navds-global-color-white);
        display: flex;
      `}
    >
      <Heading
        className={css(
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
