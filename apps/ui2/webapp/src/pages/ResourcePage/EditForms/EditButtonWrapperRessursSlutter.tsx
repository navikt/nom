import { useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import type { ReactNode } from "react";

import { harLederTilgangForNavidentOptions } from "~/api/react-query-endpoints";
import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";
import type { Ressurs } from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import { isInDevelopment } from "~/utils";

export function EditButtonWrapperRessursSlutter({
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
  const isDevelopment = isInDevelopment();

  const hasWriteAccessForRessurs =
    useQuery(harLederTilgangForNavidentOptions(ressurs.navident)).data ?? false;

  const ressursCanBeEdited =
    ressurs.sektor !== Sektor.NAV_STATLIG || (hasAdminAccess && isDevelopment);

  if (
    !editPersonopplysninger &&
    ressursCanBeEdited &&
    (hasAdminAccess || (hasWriteAccess && hasWriteAccessForRessurs))
  ) {
    return children;
  }

  return;
}
