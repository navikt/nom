import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";

import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";
import type { Ressurs } from "~/types/api-models";
import { Sektor } from "~/types/api-models";

export function EditButtonWrapperRessursSlutter({
  ressurs,
  children,
  hasWriteAccessForRessurs,
}: {
  ressurs: Ressurs;
  children: ReactNode;
  hasWriteAccessForRessurs: boolean;
}) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });
  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const ressursCanBeEdited =
    ressurs.sektor !== Sektor.NAV_STATLIG || hasAdminAccess;

  if (
    !editPersonopplysninger &&
    ressursCanBeEdited &&
    (hasAdminAccess || (hasWriteAccess && hasWriteAccessForRessurs))
  ) {
    return children;
  }

  return;
}
