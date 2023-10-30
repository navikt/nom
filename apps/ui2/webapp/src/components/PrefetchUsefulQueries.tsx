import { useQueryClient } from "@tanstack/react-query";

import {
  getAlleRessurserGraphql,
  getAllOrgEnheter,
} from "~/api/react-query-endpoints";

export function PrefetchUsefulAdminQueries() {
  const queryClient = useQueryClient();

  queryClient.prefetchQuery(getAllOrgEnheter);
  queryClient.prefetchQuery(getAlleRessurserGraphql);

  return <></>;
}
