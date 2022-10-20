import { css } from "@emotion/css";

import { slackNom } from "./Images";

export const footerHeigth = "9.5rem";

const outerDiv = css({
  width: "100%",
  bottom: "0",
  height: footerHeigth,
  background: "var(--navds-global-color-lightblue-900)",
});

const innerDiv = css({
  height: "100%",
  width: "100%",
  margin: "0 auto",
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  color: "var(--navds-global-color-white)",
  textAlign: "center",
});

const slackLink = css({
  marginLeft: "3rem",
});

const Footer = () => {
  return (
    <div className={outerDiv}>
      <div className={innerDiv}>
        <a
          className={slackLink}
          href="https://nav-it.slack.com/archives/C0362LA7ZLN"
          rel="noopener noreferrer"
          target="_blank"
        >
          <img alt="nom slack" src={slackNom}></img>
        </a>
      </div>
    </div>
  );
};

export default Footer;
