import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";
import type { ReactNode } from "react";

import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";

export function ContainerWithHighlightedTop({
  children,
  title,
}: {
  children: ReactNode;
  title: string;
}) {
  return (
    <div
      css={css`
        min-height: 300px;
        padding: var(--a-spacing-8);
        border: 1px solid var(--a-gray-400);
        border-top: 5px solid var(--a-lightblue-800);
        display: flex;
        flex-direction: column;
        background: white;
        gap: var(--a-spacing-9);
      `}
    >
      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-4);
        `}
      >
        <img
          alt=""
          css={css`
            height: 65px;
            width: 65px;
            align-self: center;
          `}
          src={opprettRessursIcon}
        />
        <Heading
          css={css`
            align-self: center;
          `}
          size="large"
        >
          {title}
        </Heading>
      </div>
      {children}
    </div>
  );
}
