import { createFileRoute } from "@tanstack/react-router";

import { OrgRessursTabellPage } from "~/pages/ResourcePage/TableView/OrgRessursTabellPage";

export const Route = createFileRoute("/org/resources-table/$id")({
  component: OrgRessursTabellPage,
});
