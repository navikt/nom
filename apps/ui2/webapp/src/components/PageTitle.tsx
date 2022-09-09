import { css } from "@emotion/css";

const titleStyles = css({
  fontFamily: "Source Sans Pro",
  fontSize: "22px",
  fontStyle: "normal",
  fontWeight: "800",
  lineHeight: "24px",
  letterSpacing: "0em",
  color: "#262626",
});

type PageTitleProps = {
  title: string;
};

const PageTitle = (props: PageTitleProps) => (
  <span className={titleStyles}>{props.title}</span>
);

export default PageTitle;
