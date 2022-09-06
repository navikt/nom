import { css } from "@emotion/css";
import { LinkPanel } from "@navikt/ds-react";

const customLinkPanelStyles = css({
  display: "flex",
  alignItems: "center",
  padding: "15px",
});

type CustomLinkPanelProps = {
  title?: string;
  description?: string;
  icon?: string;
  linkTo?: string;
};

const CustomLinkPanel = (props: CustomLinkPanelProps) => {
  return (
    <div>
      <LinkPanel href={props.linkTo}>
        <div className={customLinkPanelStyles}>
          <img src={props.icon} className={css({ marginRight: "50px" })} />
          <div>
            <LinkPanel.Title>{props.title}</LinkPanel.Title>
            <LinkPanel.Description>{props.description}</LinkPanel.Description>
          </div>
        </div>
      </LinkPanel>
    </div>
  );
};

export default CustomLinkPanel;
