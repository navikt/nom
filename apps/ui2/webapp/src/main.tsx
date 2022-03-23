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
import {ApolloProvider} from "@apollo/client";
import {apolloClient} from "./api/apolloclient";
import {NomApiGraphqlTestPage} from "./pages/NomApiGraphqlTestPage";

const AppRoutes = (): JSX.Element => (
    <Routes>
        <Route path="/" element={<MainPage/>}/>
        <Route path="/resource" element={<ResourcePage/>}/>
        <Route path="/formtestpage" element={<FormTestPage/>}/>
        <Route path={"/nomapigraphqltest"} element={<NomApiGraphqlTestPage/>}/>
        <Route path={"*"} element={<Notfound/>}/>
    </Routes>
);


ReactDOM.render(
    <React.StrictMode>
        <BrowserRouter>
            <ApolloProvider client={apolloClient}>
                <AppRoutes/>
            </ApolloProvider>
        </BrowserRouter>
    </React.StrictMode>,
    document.getElementById('root')
)
