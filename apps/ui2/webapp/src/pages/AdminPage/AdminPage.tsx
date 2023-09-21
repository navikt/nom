import { Tabs } from "@navikt/ds-react";
import { Outlet, useNavigate } from "@tanstack/react-router";

import { orgEnheterAdminRoute, ressurserAdminRoute } from "~/reactRouterRoutes";

export function AdminPage() {
  const navigate = useNavigate();
  const { pathname } = new URL(window.location.href);

  return (
    <Tabs iconPosition="left" value={pathname}>
      <Tabs.List>
        <Tabs.Tab
          label="OrgEnheter"
          onClick={() => navigate({ to: orgEnheterAdminRoute.fullPath })}
          value="/admin/orgenheter"
        />
        <Tabs.Tab
          label="Ressurser"
          onClick={() => navigate({ to: ressurserAdminRoute.fullPath })}
          value="/admin/ressurser"
        />
      </Tabs.List>
      <Outlet />
    </Tabs>
  );
}
