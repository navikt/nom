import { Navigate, Route, Routes } from "react-router";
import { SearchForPerson } from "./pages/CreateResourcePage";
import { NotfoundPage } from "./pages/NotfoundPage";
import ResourceView from "./pages/ResourceView";
import OrganisasjonHierarkiPageNew from "./pages/OrganisasjonHierarkiPageNew";
import ResourceForm from "./components/Resource/ResourceForm";

const AppRoutes = () => (
  <Routes>
    <Route path="/" element={<Navigate to="/org/0_NAV_NAV" />} />
    <Route path="/ressurs/:id" element={<ResourceView />} />
    <Route path="/ressurs/create" element={<SearchForPerson />}>
      <Route path=":personIdent" element={<ResourceForm />} />
    </Route>

    <Route path={"*"} element={<NotfoundPage />} />

    <Route path="/org" element={<Navigate to="/org/0_NAV_NAV" />} />
    <Route path="/org/:id" element={<OrganisasjonHierarkiPageNew />}>
      <Route path=":resourceId" element={<OrganisasjonHierarkiPageNew />} />
    </Route>
  </Routes>
);

export default AppRoutes;
