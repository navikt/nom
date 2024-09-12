import { css } from "@emotion/react";
import type { ReactNode } from "react";

import { defaultWidth } from "~/resources/NomStyles";

type ContentWrapperProperties = {
  children: ReactNode;
  limitWidth?: boolean;
};

const ContentWrapper = ({
  children,
  limitWidth = true, // default value is true
}: ContentWrapperProperties) => (
  <div
    css={[
      css`
        margin-top: var(--a-spacing-8);
        margin-bottom: var(--a-spacing-20);
        ${limitWidth ? "" : "max-width: unset !important;"}
      `,
      defaultWidth,
    ]}
  >
    {children}
  </div>
);

export default ContentWrapper;
