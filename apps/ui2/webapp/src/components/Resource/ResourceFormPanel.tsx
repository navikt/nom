import { css } from "@emotion/css";
import { Panel } from "@navikt/ds-react";
import type { ReactNode } from "react";

const panelStyles = css({
  minHeight: "300px",
  margin: "0 auto",
  width: "70%",
  padding: "2rem",
  border: "1px solid  var(--navds-global-color-gray-400)",
  borderTop: "5px solid var(--navds-global-color-lightblue-800)",
  borderRadius: "5px",
  display: "flex",
  flexDirection: "column",
  gap: "1rem",
});

type ResourceFormPanelProperties = {
  children: ReactNode;
};

const ResourceFormPanel = (properties: ResourceFormPanelProperties) => (
  <Panel className={panelStyles}>{properties.children}</Panel>
);

export default ResourceFormPanel;
