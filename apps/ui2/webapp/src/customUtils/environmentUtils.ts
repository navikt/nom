import { useQuery } from "@tanstack/react-query";

import { CACHE_FOR, getEnvironmentConfig } from "~/api/react-query-endpoints";
import { DeployCluster } from "~/types/api-models";

export function useGetEnvironmentConfig() {
  return useQuery({
    queryKey: getEnvironmentConfig.queryKey,
    queryFn: getEnvironmentConfig.queryFn,
    gcTime: CACHE_FOR.anHour,
    staleTime: CACHE_FOR.anHour,
  });
}

export function useInDevelopment() {
  return useGetEnvironmentConfig().data?.deployCluster === DeployCluster.DEV;
}

export function useNavToppnodeOrgEnhetId() {
  return useGetEnvironmentConfig().data?.navToppnodeId ?? "";
}

export function useDirektoratOrgEnhetId() {
  return useGetEnvironmentConfig().data?.direktoratId ?? "";
}

export function useTeknologiavdelingenOrgEnhetId() {
  return useGetEnvironmentConfig().data?.teknologiavdelingenId ?? "";
}

export function useHelfoOrgEnhetId() {
  return useGetEnvironmentConfig().data?.helfoId ?? "";
}

export function useSkatteetatenOrgEnhetId() {
  return useGetEnvironmentConfig().data?.skattId ?? "";
}

export function useRiksrevisjonenOrgEnhetId() {
  return useGetEnvironmentConfig().data?.riksrevisjonenId ?? "";
}

export function useVikafossenOrgEnhetId() {
  return useGetEnvironmentConfig().data?.vikafossenId ?? "";
}

export function useLinjeenhetOrgEnhetIder() {
  return useGetEnvironmentConfig().data?.linjeenhetene ?? "";
}

export function useIsHelfoOrgEnhet(orgEnhetId: string) {
  return orgEnhetId === useHelfoOrgEnhetId();
}

export function useIsSkatteetatenOrgEnhet(orgEnhetId: string) {
  return orgEnhetId === useSkatteetatenOrgEnhetId();
}

export function useIsRiksrevisjonenOrgEnhet(orgEnhetId: string) {
  return orgEnhetId === useRiksrevisjonenOrgEnhetId();
}

export function useIsVikafossenOrgEnhet(orgEnhetId: string) {
  return orgEnhetId === useVikafossenOrgEnhetId();
}

export function useIsEksternSamarbeidspartnerOrgEnhet(orgEnhetId: string) {
  return (
    useIsHelfoOrgEnhet(orgEnhetId) ||
    useIsSkatteetatenOrgEnhet(orgEnhetId) ||
    useIsRiksrevisjonenOrgEnhet(orgEnhetId) ||
    useIsVikafossenOrgEnhet(orgEnhetId)
  );
}

export function useUksterneOrgEnhetIder() {
  return [
    useHelfoOrgEnhetId(),
    useSkatteetatenOrgEnhetId(),
    useRiksrevisjonenOrgEnhetId(),
    useVikafossenOrgEnhetId(),
  ].filter((id) => id !== "");
}

export function useTeamkatalogenBaseUrl() {
  return useInDevelopment()
    ? "https://teamkatalog.ekstern.dev.nav.no"
    : "https://teamkatalog.nav.no";
}

export function useAdminBaseUrl() {
  return useInDevelopment()
    ? `https://nom-admin${useEnvironmentSuffix()}.ansatt.dev.nav.no`
    : "https://nom-admin.ansatt.nav.no";
}

export function useEnvironmentSuffix() {
  const suffix = useGetEnvironmentConfig().data?.environmentSuffix ?? "";

  if (!suffix) {
    return "";
  }

  return `-${suffix}`;
}
