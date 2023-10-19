import { useQuery } from "@tanstack/react-query";
import axios from "axios";

import type { SessionInfoResponse } from "~/types/api-models";
import { UserRoles } from "~/types/api-models";

export function useGetSession() {
  return useQuery({
    queryKey: ["SESSION"],
    queryFn: () => axios.get<SessionInfoResponse>("/nom-api/app/sessioninfo"),
    gcTime: 60 * 60 * 1000,
    staleTime: 60 * 60 * 1000,
    select: (response) => response.data,
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
