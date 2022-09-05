import { css } from "@emotion/css";
import { Panel } from "@navikt/ds-react";

const panelStyles = css({
  minHeight: "300px",
  margin: "0 auto",
  width: "70%",
  padding: "2rem",
  border: "1px solid #B0B0B0",
  borderTop: "5px solid #236B7D",
  borderRadius: "5px",
});

type ResourceFormPanelProps = {
  children: React.ReactNode;
};

const ResourceFormPanel = (props: ResourceFormPanelProps) => (
  <Panel className={panelStyles}>{props.children}</Panel>
);

export default ResourceFormPanel;
