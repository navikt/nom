import { createFileRoute } from "@tanstack/react-router";
import React from "react";

import { useHasWriteAccess } from "~/api/useGetSession";
import {
  CreateRessursPage,
  validateCreateRessursSearch,
} from "~/pages/CreateResourcePage/CreateResourcePageRoutes";

export const Route = createFileRoute("/ressurs/create/$step")({
  component: () => {
    const hasWriteAccess = useHasWriteAccess();
    if (hasWriteAccess) {
      return <CreateRessursPage />;
    }
  },
  validateSearch: validateCreateRessursSearch,
});
