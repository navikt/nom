import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";
import type { ReactNode } from "react";

import { blueTopBorder } from "~/resources/NomStyles";

export function ContainerWithHighlightedTop({
  children,
  title,
  icon,
}: {
  children: ReactNode;
  title: string | ReactNode;
  icon: string;
}) {
  return (
    <div
      css={[
        blueTopBorder,
        css`
          min-height: 300px;
          padding: var(--a-spacing-12);
          display: flex;
          flex-direction: column;
          background: white;
          gap: var(--a-spacing-9);
        `,
      ]}
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
          src={icon}
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
