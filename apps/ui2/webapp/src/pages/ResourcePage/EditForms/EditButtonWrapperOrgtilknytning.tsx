import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React from "react";

import {
  useHasAdminAccess,
  useHasAdminOrWriteAccess,
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
  const ressursCanBeEdited = ressurs.sektor !== Sektor.NAV_STATLIG;
  const hasAdminOrWriteAccess = useHasAdminOrWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  if (hasAdminAccess) {
    return children;
  }

  if (!hasAdminOrWriteAccess || !ressursCanBeEdited || editPersonopplysninger) {
    return <></>;
  }

  return children;
}
