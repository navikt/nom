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
  return useGetSession().data?.roles.includes(UserRoles.ROLE_WRITE) ?? false;
}

export function useHasAdminAccess() {
  return useGetSession().data?.roles.includes(UserRoles.ROLE_ADMIN) ?? false;
}

export function useHasPilotAccess() {
  return useGetSession().data?.roles.includes(UserRoles.ROLE_PILOT) ?? false;
}

export function useHasAdminReadOnlyAccess() {
  return useGetSession().data?.roles.includes(UserRoles.ROLE_READONLY) ?? false;
}

export function useHasSelvbetjeningAccess(navident: string) {
  return useGetSession().data?.navident === navident;
}

export function useHasAdminOrWriteAccess() {
  return useHasAdminAccess() || useHasWriteAccess();
}

export function useHasRobotAccess() {
  return (
    useGetSession().data?.roles.includes(UserRoles.ROLE_WRITE_ROBOT) ?? false
  );
}

export function useHasVikafossenAccess() {
  return (
    useGetSession().data?.roles.includes(UserRoles.ROLE_WRITE_VIKAFOSSEN) ??
    false
  );
}

export function useHasUtenlandskKonsulentAccess() {
  return (
    useGetSession().data?.roles.includes(UserRoles.ROLE_WRITE_UTL_KONSULENT) ??
    false
  );
}

export function useHasExternalFundsRecipientAccess() {
  return (
    useGetSession().data?.roles.includes(
      UserRoles.ROLE_EXTERNAL_FUNDS_RECIPIENT,
    ) ?? false
  );
}
