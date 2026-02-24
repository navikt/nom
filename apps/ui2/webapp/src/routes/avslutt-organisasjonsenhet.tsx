import { createFileRoute } from "@tanstack/react-router";

import { AvsluttOrgEnhet } from "~/pages/OrganizationMastering/AvsluttOrgEnhet";

export const Route = createFileRoute("/avslutt-organisasjonsenhet")({
  component: AvsluttOrgEnhet,
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
