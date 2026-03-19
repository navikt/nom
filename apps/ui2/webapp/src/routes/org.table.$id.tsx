import { createFileRoute } from "@tanstack/react-router";

import { TabellvisningOrgEnheter } from "~/pages/OrganisasjonsHierarkiPage/TableView/TabellvisningOrgEnheter";
import { OrgRessursTabellPage } from "~/pages/ResourcePage/TableView/OrgRessursTabellPage";

export const Route = createFileRoute("/org/table/$id")({
  component: TabellvisningOrgEnheter,
});
