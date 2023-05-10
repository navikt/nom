import { css } from "@emotion/react";
import type { ReactNode } from "react";

import { mediaQueryContentWidth } from "~/resources/NomStyles";

type ContentWrapperProperties = {
  children: ReactNode;
};

const ContentWrapper = (properties: ContentWrapperProperties) => (
  <div
    css={[
      mediaQueryContentWidth,
      css`
        margin: var(--a-spacing-8) auto var(--a-spacing-20);
      `,
    ]}
  >
    {properties.children}
  </div>
);

export default ContentWrapper;
