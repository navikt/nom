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

type ContentWrapperProps = {
  children: React.ReactElement;
};

const ContentWrapper = (props: ContentWrapperProps) => (
  <div className={contentStyle}>{props.children}</div>
);

export default ContentWrapper;
