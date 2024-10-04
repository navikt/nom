import { useQueryClient } from "@tanstack/react-query";

import {
  getAlleRessurserGraphql,
  getAllOrgEnheter,
} from "~/api/react-query-endpoints";

export function PrefetchUsefulAdminQueries() {
  const queryClient = useQueryClient();

  // Vi dropper prefetch av Admin page data i nom ui nå
  // queryClient.prefetchQuery(getAllOrgEnheter);
  // queryClient.prefetchQuery(getAlleRessurserGraphql);

  return <></>;
}
