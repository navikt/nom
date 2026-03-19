import { createFileRoute } from "@tanstack/react-router";

import { TabellvisningOrgEnheter } from "~/pages/OrganisasjonsHierarkiPage/TableView/TabellvisningOrgEnheter";

export const Route = createFileRoute("/org/table")({
  component: TabellvisningOrgEnheter,
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
