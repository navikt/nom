import { Tabs } from "@navikt/ds-react";
import {
  createFileRoute,
  Outlet,
  redirect,
  useLocation,
  useNavigate,
} from "@tanstack/react-router";

export const Route = createFileRoute("/sinks")({
  loader: ({ location }) => {
    if (location.pathname === "/sinks") {
      throw redirect({
        to: "/sinks/ressurs",
      });
    }
  },
  component: () => {
    const { pathname } = useLocation();
    const navigate = useNavigate();

    return (
      <Tabs iconPosition="left" value={pathname}>
        <Tabs.List>
          <Tabs.Tab
            label="Historikk ressurser"
            onClick={() => navigate({ to: "/sinks/ressurs" })}
            value="/sinks/ressurs"
          />
          <Tabs.Tab
            label="Historikk orgenheter"
            onClick={() => navigate({ to: "/sinks/orgenhet" })}
            value="/sinks/orgenhet"
          />
          <Tabs.Tab
            label="Historikk tilganger"
            onClick={() => navigate({ to: "/sinks/tilgang" })}
            value="/sinks/tilgang"
          />
        </Tabs.List>
        <Outlet />
      </Tabs>
    );
  },
});
