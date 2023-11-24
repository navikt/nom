import { css } from "@emotion/react";
import { Tabs } from "@navikt/ds-react";
import {
  Navigate,
  Outlet,
  RootRoute,
  Route,
  Router,
  ScrollRestoration,
} from "@tanstack/react-router";
import React from "react";

import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";
import { Footer, footerHeight } from "~/components/Footer";
import { headerHeight, NavHeader } from "~/components/NavHeader";
import PilotBanner, { pilotBannerHeight } from "~/components/PilotBanner";
import { PrefetchUsefulAdminQueries } from "~/components/PrefetchUsefulQueries";
import { AdminPage } from "~/pages/AdminPage/AdminPage";
import { OrgEnheterAdminPage } from "~/pages/AdminPage/OrgEnheterAdminPage";
import { OrgEnhetHistoryPage } from "~/pages/AdminPage/OrgEnhetHistoryPage";
import type { RessursFilterValues } from "~/pages/AdminPage/RessurserAdminPage";
import { RessurserAdminPage } from "~/pages/AdminPage/RessurserAdminPage";
import { RessursHistoryPage } from "~/pages/AdminPage/RessursHistoryPage";
import {
  CreateRessursPage,
  validateCreateRessursSearch,
} from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import { OrganisasjonHierarkiPage } from "~/pages/OrganisasjonsHierarkiPage/OrganisasjonHierarkiPage";
import { ResourcePage } from "~/pages/ResourcePage/ResourcePage";
import { isInDevelopment } from "~/utils";

const TanStackRouterDevtools =
  process.env.NODE_ENV === "production"
    ? () => null // Render nothing in production
    : React.lazy(() =>
        import("@tanstack/router-devtools").then((response) => {
          return {
            default: response.TanStackRouterDevtools,
          };
        }),
      );

const rootOrgEnhet = isInDevelopment() ? "sa312u" : "py437s";

const rootRoute = new RootRoute({
  component: Root,
});

function Root() {
  const hasAdminAccess = useHasAdminAccess();

  return (
    <>
      {hasAdminAccess && <PrefetchUsefulAdminQueries />}
      <React.Suspense fallback="">
        <TanStackRouterDevtools position="bottom-right" />
      </React.Suspense>
      <ScrollRestoration />
      <NavHeader />
      <PilotBanner />
      <div
        css={css`
          background-color: var(--a-gray-100);
          min-height: calc(
            100vh - ${headerHeight} - ${pilotBannerHeight} - ${footerHeight}
          );
          width: 100%;
          display: flex;
        `}
      >
        <div css={css({ width: "100%" })}>
          <Outlet />
        </div>
      </div>
      <Footer />
    </>
  );
}

function RedirectToTopOrgEnhet() {
  return (
    <Navigate
      from={indexRoute.id}
      params={{ id: rootOrgEnhet }}
      to={orgRoute.to}
    />
  );
}

export const indexRoute = new Route({
  getParentRoute: () => rootRoute,
  path: "/",
  component: RedirectToTopOrgEnhet,
});

export const ressursRoute = new Route({
  getParentRoute: () => rootRoute,
  path: "ressurs/$id",
  component: ResourcePage,
  validateSearch: (
    search,
  ): {
    alreadyExists?: boolean;
    editPersonopplysninger?: boolean;
    editOrgtilknytning?: string;
    addOrgtilknytning?: boolean;
  } => {
    // validate and parse the search params into a typed state
    return {
      alreadyExists: Boolean(search.alreadyExists),
      addOrgtilknytning: Boolean(search.addOrgtilknytning),
      editPersonopplysninger: Boolean(search.editPersonopplysninger),
      editOrgtilknytning: search.editOrgtilknytning
        ? String(search.editOrgtilknytning)
        : undefined,
    };
  },
});

export const createRessursRoute = new Route({
  getParentRoute: () => rootRoute,
  path: "ressurs/create/$step",
  component: () => {
    const hasWriteAccess = useHasWriteAccess();
    if (hasWriteAccess) {
      return <CreateRessursPage />;
    }
  },
  validateSearch: validateCreateRessursSearch,
});

export const orgRoute = new Route({
  getParentRoute: () => rootRoute,
  path: "org/$id",
  component: OrganisasjonHierarkiPage,
});

export const adminRoute = new Route({
  getParentRoute: () => rootRoute,
  path: "admin",
  component: () => {
    const hasAdminAccess = useHasAdminAccess();
    if (hasAdminAccess) {
      return <AdminPage />;
    }
  },
});

export const orgEnheterAdminRoute = new Route({
  getParentRoute: () => adminRoute,
  path: "orgenheter",
  component: () => {
    return (
      <Tabs.Panel value="/admin/orgenheter">
        <OrgEnheterAdminPage />
      </Tabs.Panel>
    );
  },
  validateSearch: (search): { allowEdit?: boolean } => {
    return {
      allowEdit: Boolean(search.allowEdit),
    };
  },
});

export const ressurserAdminRoute = new Route({
  getParentRoute: () => adminRoute,
  path: "ressurser",
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

export const ressursHistoryRoute = new Route({
  getParentRoute: () => adminRoute,
  path: "history-ressurser",
  component: () => {
    return (
      <Tabs.Panel value="/admin/history-ressurser">
        <RessursHistoryPage />
      </Tabs.Panel>
    );
  },
});

export const orgEnhetHistoryRoute = new Route({
  getParentRoute: () => adminRoute,
  path: "history-orgenheter",
  component: () => {
    return (
      <Tabs.Panel value="/admin/history-orgenheter">
        <OrgEnhetHistoryPage />
      </Tabs.Panel>
    );
  },
});

const notFoundRoute = new Route({
  getParentRoute: () => rootRoute,
  path: "*",
  component: () => "Siden finnes ikke",
});

const routeTree = rootRoute.addChildren([
  indexRoute,
  ressursRoute,
  createRessursRoute,
  orgRoute,
  adminRoute.addChildren([
    orgEnheterAdminRoute,
    ressurserAdminRoute,
    ressursHistoryRoute,
    orgEnhetHistoryRoute,
  ]),
  notFoundRoute,
]);

export const router = new Router({ routeTree });

declare module "@tanstack/react-router" {
  interface Register {
    // This infers the type of our router and registers it across your entire project
    router: typeof router;
  }
}
