import { useQuery } from "@tanstack/react-query";

import { getUnleashEnabled } from "~/api/react-query-endpoints";

export function useUnleashToggle(toggleName: string) {
  const qry = useQuery({
    queryFn: () => getUnleashEnabled(toggleName),
    queryKey: ["unleashtoggle", toggleName],
  });
  return qry.data?.isUnleashEnabled ?? false;
}
