import { Tabs } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import React from "react";

import {
  RessurserAdminPage,
  type RessursFilterValues,
} from "~/pages/AdminPage/RessurserAdminPage";

export const Route = createFileRoute("/admin/ressurser")({
  component: () => {
    return (
      <Tabs.Panel value="/admin/ressurser">
        <RessurserAdminPage />
      </Tabs.Panel>
    );
  },
  validateSearch: (search: Record<string, unknown>): RessursFilterValues => {
    return {
      ...search,
      includeUnderliggendeOrgEnheter: Boolean(
        search.includeUnderliggendeOrgEnheter,
      ),
      includeInactive: Boolean(search.includeInactive),
    };
  },
});
