import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";

import {
  useHasAdminAccess,
  useHasPilotAccess,
  useHasWriteAccess,
} from "~/api/useGetSession";
import type { Ressurs } from "~/types/api-models";
import { Sektor } from "~/types/api-models";

export function EditButtonWrapperOrgtilknytning({
  ressurs,
  children,
}: {
  ressurs: Ressurs;
  children: ReactNode;
}) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();
  const hasPilotAccess = useHasPilotAccess();

  const canEditStatlige = hasAdminAccess || hasPilotAccess;

  const canEditRessurs =
    (ressurs.sektor !== Sektor.NAV_STATLIG || canEditStatlige) &&
    (hasAdminAccess || hasWriteAccess);

  if (!editPersonopplysninger && canEditRessurs) {
    return children;
  }

  return;
}
