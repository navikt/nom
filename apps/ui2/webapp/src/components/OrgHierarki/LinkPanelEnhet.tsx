import { LinkPanel } from "@navikt/ds-react";
import { css } from "@emotion/css";
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
  backgroundColor: "#DEECEF",
  height: "150px",
});
const linkPanelLargeStyles = css(commonCss, {
  width: "100%",
  height: "100px",
});

type LinkPanelEnhetProps = {
  enhetsNavn: string;
  lederNavn: string;
  panelType: LinkPanelType;
  linkTo?: string;
};

const LinkPanelEnhet = (props: LinkPanelEnhetProps) => {
  const { enhetsNavn, lederNavn, panelType, linkTo } = props;
  const setLinkPanelStyles = () => {
    if (panelType === LinkPanelType.CURRENT) return linkPanelCurrentStyles;
    else if (panelType === LinkPanelType.LARGE) return linkPanelLargeStyles;
  };

  return (
    <Link
      to={"/org/" + linkTo || "#"}
      className={css({ textDecoration: "none" })}
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
