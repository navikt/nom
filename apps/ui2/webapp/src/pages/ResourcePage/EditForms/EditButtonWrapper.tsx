import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React from "react";

import { useHasWriteAccess } from "~/api/useGetSession";
import { ressursRoute } from "~/reactRouterRoutes";
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
  const { editPersonopplysninger, editOrgtilknytning, addOrgtilknytning } =
    useSearch({
      from: ressursRoute.id,
    });
  const isEkstern = ressurs.sektor === Sektor.EKSTERN;
  const isEditing =
    editPersonopplysninger || editOrgtilknytning || addOrgtilknytning;

  if (!hasWriteAccess || !isEkstern || isEditing) {
    return <></>;
  }

  return children;
}
