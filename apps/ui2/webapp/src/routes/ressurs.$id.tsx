import { createFileRoute } from "@tanstack/react-router";

import { ResourcePage } from "~/pages/ResourcePage/ResourcePage";

export const Route = createFileRoute("/ressurs/$id")({
  component: ResourcePage,
  validateSearch: (
    search: Record<string, unknown>,
  ): {
    editPersonopplysninger?: boolean;
    editOrgtilknytning?: string;
    addOrgtilknytning?: boolean;
  } => {
    // validate and parse the search params into a typed state
    return {
      addOrgtilknytning: Boolean(search.addOrgtilknytning),
      editPersonopplysninger: Boolean(search.editPersonopplysninger),
      editOrgtilknytning: search.editOrgtilknytning
        ? String(search.editOrgtilknytning)
        : undefined,
    };
  },
});
