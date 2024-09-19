import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React from "react";

import { useHasAdminOrWriteAccess } from "~/api/useGetSession";
import type { Ressurs } from "~/types/api-models";
import { Sektor } from "~/types/api-models";

export function EditButtonWrapper({
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
  if (
    !useHasAdminOrWriteAccess() ||
    !ressursCanBeEdited ||
    editPersonopplysninger
  ) {
    return <></>;
  }

  return children;
}
