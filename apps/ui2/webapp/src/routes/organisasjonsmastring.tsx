import { createFileRoute } from "@tanstack/react-router";

import { OrganisasjonMastringForside } from "~/pages/OrganizationMastering/OrganisasjonMastringForside";

export const Route = createFileRoute("/organisasjonsmastring")({
  component: OrganisasjonMastringForside,
});
