import { createFileRoute } from "@tanstack/react-router";

import { ResourcePage } from "~/pages/ResourcePage/ResourcePage";

export const Route = createFileRoute("/ressurs/$id")({
  component: ResourcePage,
  validateSearch: (
    search: Record<string, unknown>,
  ): {
    editPersonopplysninger?: boolean;
    editOrgtilknytninger?: boolean;
  } => {
    return {
      editPersonopplysninger: search.editPersonopplysninger ? true : undefined,
      editOrgtilknytninger: search.editOrgtilknytninger ? true : undefined,
    };
  },
});
