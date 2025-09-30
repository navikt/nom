import { createFileRoute } from "@tanstack/react-router";

import { OpprettOrgEnhet } from "~/pages/OrganizationMastering/OpprettOrgEnhet";

export const Route = createFileRoute("/opprett-organisasjonsenhet")({
  component: OpprettOrgEnhet,
});
