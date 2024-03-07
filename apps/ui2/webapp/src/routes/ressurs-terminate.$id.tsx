import { createFileRoute } from "@tanstack/react-router";
import React from "react";

import { useHasWriteAccess } from "~/api/useGetSession";
import { TerminateResourcePage } from "~/pages/ResourcePage/TerminateResourcePage";

export const Route = createFileRoute("/ressurs-terminate/$id")({
  component: () => {
    const hasWriteAccess = useHasWriteAccess();
    if (hasWriteAccess) {
      return <TerminateResourcePage />;
    }
  },
});
