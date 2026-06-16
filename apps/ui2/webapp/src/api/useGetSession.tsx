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

export function useHasAdminReadOnlyAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRoles.ROLE_READONLY);
}

export function useHasSelvbetjeningAccess(navident: string) {
  return useGetSession().data?.navident === navident;
}

export function useHasAdminOrWriteAccess() {
  return useHasAdminAccess() || useHasWriteAccess();
}

export function useHasRobotAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRoles.ROLE_WRITE_ROBOT);
}

export function useHasVikafossenAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRoles.ROLE_WRITE_VIKAFOSSEN);
}

export function useHasUtenlandskKonsulentAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRoles.ROLE_WRITE_UTL_KONSULENT);
}

export function useHasExternalFundsRecipientAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(
    UserRoles.ROLE_EXTERNAL_FUNDS_RECIPIENT,
  );
}
