import { css } from "@emotion/react";
import type { ReactNode } from "react";

import { defaultWidth } from "~/resources/NomStyles";

type ContentWrapperProperties = {
  children: ReactNode;
  limitWidth?: boolean;
};

const ContentWrapper = ({
  children,
  limitWidth = true,
}: ContentWrapperProperties) => (
  <div
    css={[
      css`
        margin-top: var(--ax-space-32);
        margin-bottom: var(--ax-space-80);
        ${limitWidth ? "" : "max-width: 1800px !important;"}
      `,
      defaultWidth,
    ]}
  >
    {children}
  </div>
);

export default ContentWrapper;
