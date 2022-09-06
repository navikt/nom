import { css } from "@emotion/css";
import { slackNom } from "./Images";

export const footerHeigth = "9.5rem";

const outerDiv = css({
  width: "100%",
  bottom: "0",
  height: footerHeigth,
  background: "#134852",
});

const innerDiv = css({
  height: "100%",
  width: "100%",
  margin: "0 auto",
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  color: "#ffffff",
  textAlign: "center",
});

const slackLink = css({
  marginLeft: "3rem",
});

const Footer = () => {
  return (
    <div className={outerDiv}>
      <div className={innerDiv}>
        {/* <div className={FooterStyle}>
          <BodyLong> {footerText}</BodyLong>
        </div> */}
        <a
          rel="noopener noreferrer"
          href="https://nav-it.slack.com/archives/C0362LA7ZLN"
          target="_blank"
          className={slackLink}
        >
          <img src={slackNom}></img>
        </a>
      </div>
    </div>
  );
};

export default Footer;
