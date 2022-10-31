import { Navigate, Route, Routes } from "react-router";

import { CreateResourcePageRoutes } from "./pages/CreateResourcePage/CreateResourcePageRoutes";
import { NotfoundPage } from "./pages/NotfoundPage";
import OrganisasjonHierarkiPageNew from "./pages/OrganisasjonsHierarkiPage/OrganisasjonHierarkiPageNew";
import ResourceView from "./pages/ResourceView";

const AppRoutes = () => (
  <Routes>
    <Route element={<Navigate to="/org/0_NAV_NAV" />} path="/" />
    <Route element={<ResourceView />} path="/ressurs/:id" />
    <Route element={<CreateResourcePageRoutes />} path="/ressurs/create/*" />
    <Route element={<NotfoundPage />} path="*" />
    <Route element={<Navigate to="/org/0_NAV_NAV" />} path="/org" />
    <Route element={<OrganisasjonHierarkiPageNew />} path="/org/:id">
      <Route element={<OrganisasjonHierarkiPageNew />} path=":resourceId" />
    </Route>
  </Routes>
);

export default AppRoutes;
