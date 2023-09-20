import { css } from "@emotion/react";
import {
  Navigate,
  Outlet,
  RootRoute,
  Route,
  Router,
  ScrollRestoration,
  useParams,
} from "@tanstack/react-router";
import React from "react";

import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";
import { Footer, footerHeight } from "~/components/Footer";
import { headerHeight, NavHeader } from "~/components/NavHeader";
import PilotBanner, { pilotBannerHeight } from "~/components/PilotBanner";
import { AdminPage } from "~/pages/AdminPage/AdminPage";
import OrganisasjonHierarkiPage from "~/pages/OrganisasjonsHierarkiPage/OrganisasjonHierarkiPage";
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
  return (
    <>
      <React.Suspense fallback="">
        <TanStackRouterDevtools />
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
    search: Record<string, unknown>,
  ): { alreadyExists?: boolean } => {
    // validate and parse the search params into a typed state
    return {
      alreadyExists: Boolean(search.alreadyExists),
    };
  },
});

function CreateRessursDummyPage() {
  const { step } = useParams({ from: createRessursRoute.id });

  let FormComponent = "Sektor";

  if (step === "2") {
    FormComponent = "Person";
  }
  if (step === "3") {
    FormComponent = "Org";
  }
  if (step === "4") {
    FormComponent = "SUmmary";
  }

  return (
    <div>
      Stepper
      {FormComponent}
    </div>
  );
}

export const createRessursRoute = new Route({
  getParentRoute: () => rootRoute,
  path: "ressurs/create/step/$step",
  component: () => {
    const hasWriteAccess = useHasWriteAccess();
    if (hasWriteAccess) {
      return <CreateRessursDummyPage />;
    }
  },
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
  validateSearch: (
    search: Record<string, unknown>,
  ): { allowEdit?: boolean } => {
    return {
      allowEdit: Boolean(search.allowEdit),
    };
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
  adminRoute,
  notFoundRoute,
]);

export const router = new Router({ routeTree });

declare module "@tanstack/react-router" {
  interface Register {
    // This infers the type of our router and registers it across your entire project
    router: typeof router;
  }
}
