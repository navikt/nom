import { Tabs } from "@navikt/ds-react";
import {
  createFileRoute,
  Outlet,
  useLocation,
  useNavigate,
} from "@tanstack/react-router";

export const Route = createFileRoute("/sanity-checks")({
  component: () => {
    const { pathname } = useLocation();
    const navigate = useNavigate();

    return (
      <Tabs iconPosition={"left"} value={pathname}>
        <Tabs.List>
          <Tabs.Tab
            label="Orgenhet"
            onClick={() => navigate({ to: "/sanity-checks/orgenhet" })}
            value="/sanity-checks/orgenhet"
          />
          <Tabs.Tab
            label="Ressurs"
            onClick={() => navigate({ to: "/sanity-checks/ressurs" })}
            value="/sanity-checks/ressurs"
          />
          <Tabs.Tab
            label="Orgtilknytning"
            onClick={() => navigate({ to: "/sanity-checks/orgtilknytning" })}
            value="/sanity-checks/orgtilknytning"
          />
        </Tabs.List>
        <Outlet />
      </Tabs>
    );
  },
});
