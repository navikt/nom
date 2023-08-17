import { css } from "@emotion/react";
import React from "react";
import { Navigate, Outlet, Route } from "react-router";
import {
  createBrowserRouter,
  createRoutesFromElements,
  ScrollRestoration,
} from "react-router-dom";

import { Footer, footerHeight } from "~/components/Footer";
import { headerHeight, NavHeader } from "~/components/NavHeader";
import PilotBanner, { pilotBannerHeight } from "~/components/PilotBanner";
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
    <Route element={<App />} path="/">
      <Route element={<Navigate to={"/org/" + rootOrgEnhet} />} path="/" />
      <Route element={<ResourcePage />} path="/ressurs/:id" />
      <Route element={<CreateResourcePageRoutes />} path="/ressurs/create/*" />
      <Route element={<SearchdemoPage />} path={"/searchdemo"} />

      <Route element={<Navigate to={"/org/" + rootOrgEnhet} />} path="/org" />
      <Route element={<OrganisasjonHierarkiPage />} path="/org/:id">
        <Route element={<NotfoundPage />} path="*" />
      </Route>
    </Route>,
  ),
);

function App() {
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
