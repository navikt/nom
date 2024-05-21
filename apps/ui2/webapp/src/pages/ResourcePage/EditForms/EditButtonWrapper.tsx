import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React from "react";

import { useHasWriteAccess } from "~/api/useGetSession";
import type { Ressurs } from "~/types/api-models";
import { Sektor } from "~/types/api-models";

export function EditButtonWrapper({
  ressurs,
  children,
}: {
  ressurs: Ressurs;
  children: ReactNode;
}) {
  const hasWriteAccess = useHasWriteAccess();
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });
  const ressursCanBeEdited = ressurs.sektor !== Sektor.NAV_STATLIG;
  if (!hasWriteAccess || !ressursCanBeEdited || editPersonopplysninger) {
    return <></>;
  }

  return children;
}
