import { css } from "@emotion/css";
import { LinkPanel } from "@navikt/ds-react";
import { Link } from "react-router-dom";

export enum LinkPanelType {
  CURRENT = "current",
  LARGE = "large",
  SMALL = "small",
}
const commonCss = css({
  width: "100%",
  display: "flex",
  alignItems: "center",
  marginBottom: "15px",
});

const linkPanelCurrentStyles = css(commonCss, {
  backgroundColor: "var(--nom-secondary-hover)",
  height: "150px",
});
const linkPanelLargeStyles = css(commonCss, {
  width: "100%",
  height: "100px",
});

type LinkPanelEnhetProperties = {
  enhetsNavn: string;
  lederNavn: string;
  panelType: LinkPanelType;
  linkTo?: string;
};

const LinkPanelEnhet = (properties: LinkPanelEnhetProperties) => {
  const { enhetsNavn, lederNavn, panelType, linkTo } = properties;
  const setLinkPanelStyles = () => {
    if (panelType === LinkPanelType.CURRENT) return linkPanelCurrentStyles;
    else if (panelType === LinkPanelType.LARGE) return linkPanelLargeStyles;
  };

  return (
    <Link
      className={css({ textDecoration: "none" })}
      to={"/org/" + linkTo || "#"}
    >
      <LinkPanel className={setLinkPanelStyles()}>
        <div>
          {/* <Heading size="medium" className={css({ marginBottom: "5px" })}>{enhetsNavn}</Heading> */}
          <LinkPanel.Title>{enhetsNavn}</LinkPanel.Title>
          <LinkPanel.Description>
            Leder: <span> {lederNavn}</span>
          </LinkPanel.Description>
        </div>
      </LinkPanel>
    </Link>
  );
};

export default LinkPanelEnhet;
