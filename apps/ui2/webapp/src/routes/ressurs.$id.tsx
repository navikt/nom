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
    editOrgtilknytninger?: boolean;
  } => {
    // validate and parse the search params into a typed state
    return {
      addOrgtilknytning: search.addOrgtilknytning ? true : undefined,
      editPersonopplysninger: search.editPersonopplysninger ? true : undefined,
      editOrgtilknytning: search.editOrgtilknytning
        ? String(search.editOrgtilknytning)
        : undefined,
      editOrgtilknytninger: search.editOrgtilknytninger ? true : undefined,
    };
  },
});
