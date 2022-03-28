import React from "react";
import ReactDOM from "react-dom";
import "@navikt/ds-css";
import "@navikt/ds-css-internal";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import MainPage from "./pages/MainPage";
import { FormTestPage } from "./pages/FormTestPage";
import { NotfoundPage } from "./pages/NotfoundPage";
import { ApolloProvider } from "@apollo/client";
import { apolloClient } from "./api/apolloclient";
import { NomApiGraphqlTestPage } from "./pages/NomApiGraphqlTestPage";
import { css } from "@emotion/css";
import Header, { headerHeight } from "./components/Header";
import SideBar from "./components/SideBar";
import ResourceView from "./pages/ResourceView";

const layoutStyle = css({
  backgroundColor: "#F1F1F1",
  minHeight: `calc(100vh - ${headerHeight})`,
  display: "flex",
  paddingLeft: "10px",
});

const contentStyle = css({
  margin: "2rem 2rem 0 2rem",
  width: "100%"
});

const AppRoutes = () => (
  <Routes>
    <Route path="/" element={<MainPage />} />
    <Route path="/ressurs/:id" element={<ResourceView />} />
    <Route path="/formtestpage" element={<FormTestPage />} />
    <Route path={"/nomapigraphqltest"} element={<NomApiGraphqlTestPage />} />
    <Route path={"*"} element={<NotfoundPage />} />
  </Routes>
);

ReactDOM.render(
  <React.StrictMode>
    <BrowserRouter>
      <ApolloProvider client={apolloClient}>
        <Header />
        <div className={layoutStyle}>
          <SideBar />
          <div className={contentStyle}><AppRoutes /></div>
        </div>      
      </ApolloProvider>
    </BrowserRouter>
  </React.StrictMode>,
  document.getElementById("root")
);
