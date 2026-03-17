import { createFileRoute } from "@tanstack/react-router";

import { KorrigerOrgEnhetLeder } from "~/pages/OrganizationMastering/KorrigerOrgEnhetLeder";

export const Route = createFileRoute("/korriger-leder-organisasjonsenhet")({
  component: KorrigerOrgEnhetLeder,
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
