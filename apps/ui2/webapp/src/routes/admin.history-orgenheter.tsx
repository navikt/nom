import { Tabs } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import React from "react";

import { OrgEnhetHistoryPage } from "~/pages/AdminPage/OrgEnhetHistoryPage";

export const Route = createFileRoute("/admin/history-orgenheter")({
  component: () => {
    return (
      <Tabs.Panel value="/admin/history-orgenheter">
        <OrgEnhetHistoryPage />
      </Tabs.Panel>
    );
  },
});
