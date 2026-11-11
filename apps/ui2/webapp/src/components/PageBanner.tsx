import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";

import { ChristmasBanner } from "~/components/ChristmasBanner";
import { defaultWidth } from "~/resources/NomStyles";

export function PageBanner({ text }: { text: string }) {
  return (
    <>
      <div
        css={css`
          width: 100%;
          height: 78px;
          background: var(--nom-primary);
          color: var(--ax-text-neutral-contrast);
          display: flex;
        `}
      >
        <Heading
          css={css(
            defaultWidth,
            css`
              align-self: center;
              margin: auto;
              width: 1100px;
            `,
          )}
          level="1"
          size="large"
        >
          {text}
        </Heading>
      </div>

      <ChristmasBanner />
    </>
  );
}
