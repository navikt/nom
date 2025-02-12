import { Tabs } from "@navikt/ds-react";
import { Outlet, useMatch, useNavigate } from "@tanstack/react-router";

/**
 * NOTE: this component's useMatch is part of a hack to force pathname to be updated.
 * Since pathname is neither a prop nor a hook, it is not react. Therefore, the component will not re-render when it changes.
 * If tanstack-router had a useLocation hook, we would use that, but it does not.
 * This indicates that the way our tabs are linked to the url is not optimal with tanstack router.
 * To avoid having to refactor these tabs, this useMatch hook is here to enforce re-render on path change.
 * A possible solution to restructure tabs+url to work is to make this admin route [a pathless route](https://tanstack.com/router/v1/docs/guide/route-paths#pathless-routes)
 */
export function AdminPage() {
  const navigate = useNavigate({ from: "/admin" });
  const { pathname } = new URL(globalThis.location.href);

  useMatch({ from: "/admin" });

  return (
    <Tabs iconPosition="left" value={pathname}>
      <Tabs.List>
        <Tabs.Tab
          label="OrgEnheter"
          onClick={() => navigate({ to: "/admin/orgenheter" })}
          value="/admin/orgenheter"
        />
        <Tabs.Tab
          label="Ressurser"
          onClick={() => navigate({ to: "/admin/ressurser" })}
          value="/admin/ressurser"
        />
        <Tabs.Tab
          label="Historikk ressurser"
          onClick={() => navigate({ to: "/admin/history-ressurser" })}
          value="/admin/history-ressurser"
        />
        <Tabs.Tab
          label="Historikk orgenheter"
          onClick={() => navigate({ to: "/admin/history-orgenheter" })}
          value="/admin/history-orgenheter"
        />
      </Tabs.List>
      <Outlet />
    </Tabs>
  );
}
