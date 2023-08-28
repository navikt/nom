import { css } from "@emotion/react";
import React from "react";
import { Navigate, Outlet, Route } from "react-router";
import {
  createBrowserRouter,
  createRoutesFromElements,
  ScrollRestoration,
} from "react-router-dom";

import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";
import { Footer, footerHeight } from "~/components/Footer";
import { headerHeight, NavHeader } from "~/components/NavHeader";
import PilotBanner, { pilotBannerHeight } from "~/components/PilotBanner";
import { AdminPage } from "~/pages/AdminPage/AdminPage";
import { isInDevelopment } from "~/utils";

import { CreateResourcePageRoutes } from "./pages/CreateResourcePage/CreateResourcePageRoutes";
import { NotfoundPage } from "./pages/NotfoundPage";
import OrganisasjonHierarkiPage from "./pages/OrganisasjonsHierarkiPage/OrganisasjonHierarkiPage";
import { ResourcePage } from "./pages/ResourcePage/ResourcePage";
import SearchdemoPage from "./pages/SearchdemoPage";

const layoutStyle = css`
  background-color: var(--a-gray-100);
  min-height: calc(
    100vh - ${headerHeight} - ${pilotBannerHeight} - ${footerHeight}
  );
  width: 100%;
  display: flex;
`;

const rootOrgEnhet = isInDevelopment() ? "sa312u" : "py437s";

export const router = createBrowserRouter(
  createRoutesFromElements(
    <Route element={<MainContent />} path="/">
      <Route
        element={<Navigate replace to={"/org/" + rootOrgEnhet} />}
        path="/"
      />
      <Route element={<ResourcePage />} path="/ressurs/:id" />

      <Route element={<WriteProtectedRoutes />}>
        <Route
          element={<CreateResourcePageRoutes />}
          path="/ressurs/create/*"
        />
      </Route>
      <Route element={<AdminProtectedRoutes />}>
        <Route element={<AdminPage />} path={"/admin"} />
      </Route>

      <Route element={<SearchdemoPage />} path={"/searchdemo"} />

      <Route element={<Navigate to={"/org/" + rootOrgEnhet} />} path="/org" />
      <Route element={<OrganisasjonHierarkiPage />} path="/org/:id">
        <Route element={<NotfoundPage />} path="*" />
      </Route>
      {/*This redirection "should" have been done in the frackend.*/}
      {/*However, when requesting index.html Express does some kind of caching (304) responses,*/}
      {/*that does not account for our cookie that determines which index.html file is server.*/}
      <Route element={<Navigate replace to="/" />} path="toggle-devserver" />
    </Route>,
  ),
);

function AdminProtectedRoutes() {
  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return <Navigate replace to="/" />;
  }

  return <Outlet />;
}

function WriteProtectedRoutes() {
  const hasWriteAccess = useHasWriteAccess();

  if (!hasWriteAccess) {
    return <Navigate replace to="/" />;
  }

  return <Outlet />;
}

function MainContent() {
  return (
    <>
      <ScrollRestoration />
      <NavHeader />
      <PilotBanner />
      <div css={layoutStyle}>
        <div css={css({ width: "100%" })}>
          <Outlet />
        </div>
      </div>
      <Footer />
    </>
  );
}
