import { Tabs } from "@navikt/ds-react";
import {
  createFileRoute,
  Outlet,
  useLocation,
  useNavigate,
} from "@tanstack/react-router";

import {
  getOrganiseringStateOptions,
  getOrgenhetStatesOptions,
  getOrgtilknytningStateOptions,
  getRessursStatesOptions,
} from "~/api/queries.ts";

export const Route = createFileRoute("/state")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    // prefetch as much as possible
    context.queryClient.prefetchQuery(getRessursStatesOptions(deps));
    context.queryClient.prefetchQuery(getOrgenhetStatesOptions(deps));
    context.queryClient.prefetchQuery(getOrganiseringStateOptions(deps));
    context.queryClient.prefetchQuery(getOrgtilknytningStateOptions(deps));
  },
  component: () => {
    const { pathname } = useLocation();
    const navigate = useNavigate();

    return (
      <Tabs iconPosition="left" value={pathname}>
        <Tabs.List>
          <Tabs.Tab
            label="Ressurser"
            onClick={() =>
              navigate({
                to: "/state/ressurs",
              })
            }
            value="/state/ressurs"
          />
          <Tabs.Tab
            label="Orgenheter"
            onClick={() =>
              navigate({
                to: "/state/orgenhet",
              })
            }
            value="/state/orgenhet"
          />
          <Tabs.Tab
            label="Organisering"
            onClick={() =>
              navigate({
                to: "/state/organisering",
              })
            }
            value="/state/organisering"
          />
          <Tabs.Tab
            label="Orgtilknytning"
            onClick={() =>
              navigate({
                to: "/state/orgtilknytning",
              })
            }
            value="/state/orgtilknytning"
          />
        </Tabs.List>
        <Outlet />
      </Tabs>
    );
  },
});
