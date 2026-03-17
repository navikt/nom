import { createFileRoute } from "@tanstack/react-router";

import { KorrigerOrgEnhetNavn } from "~/pages/OrganizationMastering/KorrigerOrgEnhetNavn";

export const Route = createFileRoute("/korriger-navn-organisasjonsenhet")({
  component: KorrigerOrgEnhetNavn,
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
