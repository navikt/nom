import { useQueryClient } from "react-query";

import { useGetAllOrgenheterAdminQuery } from "~/api/generated/graphql_generated";
import { getAlleRessurser } from "~/api/react-query-endpoints";

export function PrefetchUsefulAdminQueries() {
  const queryClient = useQueryClient();

  useGetAllOrgenheterAdminQuery();
  queryClient.prefetchQuery(getAlleRessurser);

  return <></>;
}
