import { createFileRoute } from "@tanstack/react-router";
import React from "react";

import { useHasAdminAccess } from "~/api/useGetSession";
import { AdminPage } from "~/pages/AdminPage/AdminPage";

export const Route = createFileRoute("/admin")({
  component: () => {
    const hasAdminAccess = useHasAdminAccess();
    if (hasAdminAccess) {
      return <AdminPage />;
    }
  },
});
