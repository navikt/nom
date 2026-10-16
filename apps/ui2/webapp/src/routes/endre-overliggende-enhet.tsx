import { createFileRoute } from "@tanstack/react-router";

import { EndreOverliggendeEnhet } from "~/pages/OrganizationMastering/EndreOverliggendeEnhet";

export const Route = createFileRoute("/endre-overliggende-enhet")({
  component: EndreOverliggendeEnhet,
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
