import { createFileRoute } from "@tanstack/react-router";

import { OrganisasjonHierarkiPage } from "~/pages/OrganisasjonsHierarkiPage/OrganisasjonHierarkiPage";

export const Route = createFileRoute("/org/$id")({
  component: OrganisasjonHierarkiPage,
});
