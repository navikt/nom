import axios from "axios";
import { useQuery } from "react-query";

export interface SessionInterface {
  name: string;
  navident: string;
  roles: UserRole[];
}

enum UserRole {
  ROLE_NONE = "ROLE_NONE",
  ROLE_WRITE = "ROLE_WRITE",
  ROLE_ADMIN = "ROLE_ADMIN",
}

export function useGetSession() {
  return useQuery({
    queryKey: ["SESSION"],
    queryFn: () => axios.get<SessionInterface>("/nom-api/app/sessioninfo"),
    cacheTime: 60 * 60 * 1000,
    staleTime: 60 * 60 * 1000,
    select: (response) => response.data,
  });
}

export function useHasWriteAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRole.ROLE_WRITE);
}

export function useHasAdminAccess() {
  const session = useGetSession();
  return !!session.data?.roles.includes(UserRole.ROLE_ADMIN);
}
