import { Tabs } from "@navikt/ds-react";
import {
  createFileRoute,
  Outlet,
  useLocation,
  useNavigate,
} from "@tanstack/react-router";

import { getOrgenhetStatesOptions, getRessursStatesOptions } from "~/api/queries.ts";

export const Route = createFileRoute("/state")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    // prefetch as much as possible
    context.queryClient.prefetchQuery(getRessursStatesOptions(deps));
    context.queryClient.prefetchQuery(getOrgenhetStatesOptions(deps));
  },
  component: () => {
    const search = Route.useSearch();
    const { pathname } = useLocation();
    const navigate = useNavigate();

    return (
      <Tabs iconPosition="left" value={pathname}>
        <Tabs.List>
          <Tabs.Tab
            label="Ressurser"
            onClick={() => navigate({ to: "/state/ressurs", search })}
            value="/state/ressurs"
          />
          <Tabs.Tab
            label="Orgenheter"
            onClick={() => navigate({ to: "/state/orgenhet", search })}
            value="/state/orgenhet"
          />
        </Tabs.List>
        <Outlet />
      </Tabs>
    );
  },
});
