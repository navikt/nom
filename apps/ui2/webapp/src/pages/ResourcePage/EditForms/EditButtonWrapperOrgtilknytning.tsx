import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";

import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";

export function EditButtonWrapperOrgtilknytning({
  children,
}: {
  children: ReactNode;
}) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const canEditRessurs = hasAdminAccess || hasWriteAccess;

  if (!editPersonopplysninger && canEditRessurs) {
    return children;
  }

  return;
}
