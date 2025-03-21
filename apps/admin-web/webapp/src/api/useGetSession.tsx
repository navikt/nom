import { useQuery } from "@tanstack/react-query";

import { getSessionInfo } from "~/api/queries.ts";
import { UserRoles } from "~/types/nom-admin/nom-admin-models.ts";

export function useGetSession() {
  return useQuery(getSessionInfo());
}

export function useHasAdminAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRoles.ROLE_WRITE);
}

export function useHasReadonlyAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRoles.ROLE_READONLY);
}
