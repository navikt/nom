import { css } from "@emotion/css";

import { mediaQueryContentWidth } from "../resources/NomStyles";

const contentStyle = css(
  mediaQueryContentWidth,
  css`
    margin: 0 auto 2.5rem;
    margin-top: 2rem;
    margin-bottom: 5rem;
  `
);

type ContentWrapperProperties = {
  children: React.ReactElement;
};

const ContentWrapper = (properties: ContentWrapperProperties) => (
  <div className={contentStyle}>{properties.children}</div>
);

export default ContentWrapper;
