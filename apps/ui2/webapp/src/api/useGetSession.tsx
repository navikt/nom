import { useQuery } from "@tanstack/react-query";

import { CACHE_FOR, getSession } from "~/api/react-query-endpoints";
import { UserRoles } from "~/types/api-models";

export function useGetSession() {
  return useQuery({
    queryKey: getSession.queryKey,
    queryFn: getSession.queryFn,
    gcTime: CACHE_FOR.anHour,
    staleTime: CACHE_FOR.anHour,
  });
}

export function useHasWriteAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRoles.ROLE_WRITE);
}

export function useHasAdminAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRoles.ROLE_ADMIN);
}

export function useHasPilotAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRoles.ROLE_PILOT);
}

export function useHasAdminOrWriteAccess() {
  return useHasAdminAccess() || useHasWriteAccess();
}
