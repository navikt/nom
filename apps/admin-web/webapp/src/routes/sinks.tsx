import { Tabs } from "@navikt/ds-react";
import {
  createFileRoute,
  Outlet,
  useLocation,
  useNavigate,
} from "@tanstack/react-router";

import {
  getOrgenhetEventsOptions,
  getRessursEventsOptions,
  getTilgangEventsOptions,
} from "~/api/queries.ts";

export const Route = createFileRoute("/sinks")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    // prefetch as much as possible
    context.queryClient.prefetchQuery(getOrgenhetEventsOptions(deps));
    context.queryClient.prefetchQuery(getTilgangEventsOptions(deps));
    context.queryClient.prefetchQuery(getRessursEventsOptions(deps));
  },
  component: () => {
    const search = Route.useSearch();
    const { pathname } = useLocation();
    const navigate = useNavigate();

    return (
      <Tabs iconPosition="left" value={pathname}>
        <Tabs.List>
          <Tabs.Tab
            label="Historikk ressurser"
            onClick={() => navigate({ to: "/sinks/ressurs", search })}
            value="/sinks/ressurs"
          />
          <Tabs.Tab
            label="Historikk orgenheter"
            onClick={() => navigate({ to: "/sinks/orgenhet", search })}
            value="/sinks/orgenhet"
          />
          <Tabs.Tab
            label="Historikk tilganger"
            onClick={() => navigate({ to: "/sinks/tilgang", search })}
            value="/sinks/tilgang"
          />
          <Tabs.Tab
            label="NOM mastered"
            onClick={() => navigate({ to: "/sinks/nom-mastered", search })}
            value="/sinks/nom-mastered"
          />
        </Tabs.List>
        <Outlet />
      </Tabs>
    );
  },
});
