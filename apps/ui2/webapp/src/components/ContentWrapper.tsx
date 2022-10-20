import { css } from "@emotion/css";
import type { ReactElement } from "react";

import { mediaQueryContentWidth } from "../resources/NomStyles";

const contentStyle = css(
  mediaQueryContentWidth,
  css`
    margin: var(--navds-spacing-8) auto var(--navds-spacing-20);
  `
);

type ContentWrapperProperties = {
  children: ReactElement;
};

const ContentWrapper = (properties: ContentWrapperProperties) => (
  <div className={contentStyle}>{properties.children}</div>
);

export default ContentWrapper;
