import { createFileRoute } from "@tanstack/react-router";

import { ParorendeinfoPage } from "~/pages/Parorendeinfo/ParorendeinfoPage";

export const Route = createFileRoute("/kontaktpersoner/$id")({
  component: ParorendeinfoPage,
  validateSearch: (
    search: Record<string, unknown>,
  ): {
    editKontaktpersoner?: boolean;
  } => {
    return {
      editKontaktpersoner: search.editKontaktpersoner ? true : undefined,
    };
  },
});
