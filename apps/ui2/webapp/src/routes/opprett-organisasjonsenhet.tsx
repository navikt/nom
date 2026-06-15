import { createFileRoute } from "@tanstack/react-router";

import { OpprettOrgEnhet } from "~/pages/OrganizationMastering/OpprettOrgEnhet";

export const Route = createFileRoute("/opprett-organisasjonsenhet")({
  component: OpprettOrgEnhet,
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
