import { css } from "@emotion/react";
import type { ReactNode } from "react";

import { defaultWidth } from "~/resources/NomStyles";

type ContentWrapperProperties = {
  children: ReactNode;
};

const ContentWrapper = (properties: ContentWrapperProperties) => (
  <div
    css={[
      css`
        margin-top: var(--a-spacing-8);
        margin-bottom: var(--a-spacing-20);
      `,
      defaultWidth,
    ]}
  >
    {properties.children}
  </div>
);

export default ContentWrapper;
