import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";

import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";

export function EditButtonWrapperRessursSlutter({
  children,
  hasWriteAccessForRessurs,
}: {
  children: ReactNode;
  hasWriteAccessForRessurs: boolean;
}) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const canEditRessurs =
    hasAdminAccess || (hasWriteAccess && hasWriteAccessForRessurs);

  if (!editPersonopplysninger && canEditRessurs) {
    return children;
  }

  return;
}
