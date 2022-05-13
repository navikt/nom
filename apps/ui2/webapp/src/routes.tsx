import { Navigate, Route, Routes } from "react-router";
import CreateResourceView from "./pages/CreateResourceView";
import { FormTestPage } from "./pages/FormTestPage";
import MainPage from "./pages/MainPage";
import { NomApiGraphqlTestPage } from "./pages/NomApiGraphqlTestPage";
import { NotfoundPage } from "./pages/NotfoundPage";
import OrganisasjonHierarkiPage from "./pages/OrganisasjonHierarkiPage";
import ResourceView from "./pages/ResourceView";

const AppRoutes = () => (
  <Routes>
    <Route path="/" element={<MainPage />} />
    <Route path="/ressurs/:id" element={<ResourceView />} />
    <Route path="/ressurs/create" element={<CreateResourceView />} />
    <Route path="/org" element={<Navigate to="/org/0_NAV_NAV"/>} />
    <Route path="/org/:id" element={<OrganisasjonHierarkiPage />} />
    <Route path="/formtestpage" element={<FormTestPage />} />
    <Route path={"/nomapigraphqltest"} element={<NomApiGraphqlTestPage />} />
    <Route path={"*"} element={<NotfoundPage />} />
  </Routes>
);

export default AppRoutes
