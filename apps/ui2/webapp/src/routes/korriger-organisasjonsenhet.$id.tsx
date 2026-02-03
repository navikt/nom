import { createFileRoute } from "@tanstack/react-router";

import { KorrigerOrgEnhet } from "~/pages/OrganizationMastering/KorrigerOrgEnhet";

export const Route = createFileRoute("/korriger-organisasjonsenhet/$id")({
  component: KorrigerOrgEnhet,
});
