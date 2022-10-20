import { css } from "@emotion/css";

const titleStyles = css({
  fontFamily: "Source Sans Pro",
  fontSize: "22px",
  fontStyle: "normal",
  fontWeight: "800",
  lineHeight: "24px",
  letterSpacing: "0em",
  color: "var(--navds-global-color-gray-900)",
});

type PageTitleProperties = {
  title: string;
};

const PageTitle = (properties: PageTitleProperties) => (
  <span className={titleStyles}>{properties.title}</span>
);

export default PageTitle;
