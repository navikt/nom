import { Tabs } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import React from "react";

import { OrgEnheterAdminPage } from "~/pages/AdminPage/OrgEnheterAdminPage";

export const Route = createFileRoute("/admin/orgenheter")({
  component: () => {
    return (
      <Tabs.Panel value="/admin/orgenheter">
        <OrgEnheterAdminPage />
      </Tabs.Panel>
    );
  },
  validateSearch: (
    search: Record<string, unknown>,
  ): { allowEdit?: boolean } => {
    return {
      allowEdit: Boolean(search.allowEdit),
    };
  },
});
