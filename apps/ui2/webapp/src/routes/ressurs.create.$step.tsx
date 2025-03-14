import { createFileRoute } from "@tanstack/react-router";
import React from "react";

import { useHasAdminOrWriteAccess } from "~/api/useGetSession";
import {
  CreateRessursPage,
  validateCreateRessursSearch,
} from "~/pages/CreateResourcePage/CreateResourcePageRoutes";

export const Route = createFileRoute("/ressurs/create/$step")({
  component: () => {
    const hasAdminOrWriteAccess = useHasAdminOrWriteAccess();
    if (hasAdminOrWriteAccess) {
      return <CreateRessursPage />;
    }

    return;
  },
  validateSearch: validateCreateRessursSearch,
});
