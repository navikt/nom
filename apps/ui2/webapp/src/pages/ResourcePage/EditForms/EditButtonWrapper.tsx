import { useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React from "react";

import { harLederTilgangForNavidentOptions } from "~/api/react-query-endpoints";
import type { Ressurs } from "~/types/api-models";
import { Sektor } from "~/types/api-models";

export function EditButtonWrapper({
  ressurs,
  children,
}: {
  ressurs: Ressurs;
  children: ReactNode;
}) {
  const hasWriteAccessForRessurs =
    useQuery(harLederTilgangForNavidentOptions(ressurs.navident)).data ?? false;

  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });
  const ressursCanBeEdited = ressurs.sektor !== Sektor.NAV_STATLIG;
  if (
    !hasWriteAccessForRessurs ||
    !ressursCanBeEdited ||
    editPersonopplysninger
  ) {
    return <></>;
  }

  return children;
}
