import { Route, Routes } from "react-router";
import CreateResourceView from "./pages/CreateResourceView";
import { FormTestPage } from "./pages/FormTestPage";
import MainPage from "./pages/MainPage";
import { NomApiGraphqlTestPage } from "./pages/NomApiGraphqlTestPage";
import { NotfoundPage } from "./pages/NotfoundPage";
import ResourceView from "./pages/ResourceView";

const AppRoutes = () => (
  <Routes>
    <Route path="/" element={<MainPage />} />
    <Route path="/ressurs/:id" element={<ResourceView />} />
    <Route path="/ressurs/create" element={<CreateResourceView />} />
    <Route path="/formtestpage" element={<FormTestPage />} />
    <Route path={"/nomapigraphqltest"} element={<NomApiGraphqlTestPage />} />
    <Route path={"*"} element={<NotfoundPage />} />
  </Routes>
);

export default AppRoutes
