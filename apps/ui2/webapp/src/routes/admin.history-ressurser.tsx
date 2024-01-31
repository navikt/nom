import { Tabs } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import React from "react";

import { RessursHistoryPage } from "~/pages/AdminPage/RessursHistoryPage";

export const Route = createFileRoute("/admin/history-ressurser")({
  component: () => {
    return (
      <Tabs.Panel value="/admin/history-ressurser">
        <RessursHistoryPage />
      </Tabs.Panel>
    );
  },
});
