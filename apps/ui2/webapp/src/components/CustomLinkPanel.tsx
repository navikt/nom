import { css } from "@emotion/css";
import { LinkPanel } from "@navikt/ds-react";

const customLinkPanelStyles = css({
  display: "flex",
  alignItems: "center",
  padding: "15px",
});

type CustomLinkPanelProperties = {
  title?: string;
  description?: string;
  icon?: string;
  linkTo?: string;
};

const CustomLinkPanel = (properties: CustomLinkPanelProperties) => {
  return (
    <div>
      <LinkPanel href={properties.linkTo}>
        <div className={customLinkPanelStyles}>
          <img className={css({ marginRight: "50px" })} src={properties.icon} />
          <div>
            <LinkPanel.Title>{properties.title}</LinkPanel.Title>
            <LinkPanel.Description>
              {properties.description}
            </LinkPanel.Description>
          </div>
        </div>
      </LinkPanel>
    </div>
  );
};

export default CustomLinkPanel;
