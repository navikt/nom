import React from 'react'
import ReactDOM from 'react-dom'
import "@navikt/ds-css";
import "@navikt/ds-css-internal";
import { BrowserRouter, Outlet, Route, Routes } from "react-router-dom";
import Root from "./components/Root";
import MainPage from "./pages/MainPage";
import ResourcePage from "./pages/ResourcePage";
import { FormTestPage } from './pages/FormTestPage';
import { Notfound } from './pages/Notfound';

const AppRoutes = (): JSX.Element => (
    <Routes>
      <Route path="/" element={<MainPage />} />
      <Route path="/resource" element={<ResourcePage />} />
      <Route path="/formtestpage" element={<FormTestPage />} />
      <Route path={"*"} element={<Notfound />} />
    </Routes>
);


ReactDOM.render(
  <React.StrictMode>
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  </React.StrictMode>,
  document.getElementById('root')
)
