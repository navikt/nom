import { Navigate, Route, Routes } from "react-router";

import { CreateResourcePageRoutes } from "./pages/CreateResourcePage/CreateResourcePageRoutes";
import { NotfoundPage } from "./pages/NotfoundPage";
import OrganisasjonHierarkiPage from "./pages/OrganisasjonsHierarkiPage/OrganisasjonHierarkiPage";
import ResourceView from "./pages/ResourceView";

const AppRoutes = () => (
  <Routes>
    <Route element={<Navigate to="/org/NAV/ORGNIV0" />} path="/" />
    <Route element={<ResourceView />} path="/ressurs/:id" />
    <Route element={<CreateResourcePageRoutes />} path="/ressurs/create/*" />
    <Route element={<NotfoundPage />} path="*" />
    <Route element={<Navigate to="/org/NAV/ORGNIV0" />} path="/org" />
    <Route
      element={<OrganisasjonHierarkiPage />}
      path="/org/:agressoId/:orgNiv"
    >
      <Route element={<OrganisasjonHierarkiPage />} path=":resourceId" />
    </Route>
  </Routes>
);

export default AppRoutes;
