import { createFileRoute } from "@tanstack/react-router";

import { KorrigerOrgEnhet } from "~/pages/OrganizationMastering/KorrigerOrgEnhet";

export const Route = createFileRoute("/korriger-organisasjonsenhet")({
  component: KorrigerOrgEnhet,
  validateSearch: (
    search: Record<string, unknown>,
  ): {
    id?: string;
  } => {
    return {
      id: search.id ? (search.id as string) : undefined,
    };
  },
});
