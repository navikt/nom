import { Navigate, Route, Routes } from "react-router";
import CreateResourceView from "./pages/CreateResourceView";
import { NotfoundPage } from "./pages/NotfoundPage";
import ResourceView from "./pages/ResourceView";
import OrganisasjonHierarkiPageNew from "./pages/OrganisasjonHierarkiPageNew";

const AppRoutes = () => (
  <Routes>
    <Route path="/" element={<Navigate to="/org/0_NAV_NAV" />} />
    <Route path="/ressurs/:id" element={<ResourceView />} />
    <Route path="/ressurs/create" element={<CreateResourceView />} />

    <Route path={"*"} element={<NotfoundPage />} />

    <Route path="/org" element={<Navigate to="/org/0_NAV_NAV" />} />
    <Route path="/org/:id" element={<OrganisasjonHierarkiPageNew />}>
      <Route path=":resourceId" element={<OrganisasjonHierarkiPageNew />} />
    </Route>
  </Routes>
);

export default AppRoutes;
