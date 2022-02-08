import React from "react";
import ReactDOM from "react-dom";
import {BrowserRouter, Navigate, Route, Routes} from "react-router-dom";
import About from "./pages/about/About";
import Graphql from "./pages/graphql/Graphql";
import Organisasjonsenheter from "./pages/organisasjonsenheter/Organisasjonsenheter";
import Root from "./pages/root/Root";
import * as serviceWorker from "./serviceWorker";

import "./index.css";
import MedarbeidereLinkview from "./pages/medarbeidere/MedarbeidereLinkview";
import Bestillinger from "./pages/bestillinger/Bestillinger";
import Bestillingsgodkjenning from "./pages/bestillingsgodkjenning/Bestillingsgodkjenning";
import Nybruker from "./pages/bestillinger/nybruker/Nybruker";
import Nompagewrap from "./components/nompagewrap/Nompagewrap";
import Nyenhet from "./pages/bestillinger/nyenhet/Nyenhet";
import { ApolloProvider } from "@apollo/client";
import { apolloClient } from "./api/apollo_client";
import { BestillingList } from "./pages/bestillinger/list/BestillingList";
import {
  BestillingContext,
  useBestillingContext,
} from "./pages/bestillinger/BestillingContext";
import Enhetstyper from "./pages/enhetstyper/Enhetstyper";

function AppRouter(): JSX.Element {
  return (
      <Routes>
        <Route path="/index.html" element={<Navigate to={"/"}/>}/>
        <Route path="/" element={<Root/>}/>
        <Route path="/about" element={<About/>}/>
        <Route path="/graphql" element={<Graphql/>}/>
        <Route path="/organisasjonsenheter" element={<Navigate to="/organisasjonsenheter/0_NAV"/>}>
        </Route>
        <Route path="/organisasjonsenheter/:id" element={<Organisasjonsenheter/>}/>
        <Route path="/medarbeidere" element={<MedarbeidereLinkview/>}/>
        <Route path="/medarbeidere/:id" element={<MedarbeidereLinkview/>}/>
        <Route path="/bestillinger" element={<Bestillinger/>}/>
        <Route path="/bestillinger/nybruker" element={<Nybruker/>}/>
        <Route path="/bestillinger/nyenhet" element={<Nyenhet/>}/>
        <Route path="/bestillinger/flytteenhet" element={<Nompagewrap pagetitle="Flytte enhet">...TODO</Nompagewrap>}/>
        <Route path="/bestillinger/splitteenhet"
               element={<Nompagewrap pagetitle="Splitte enhet">...TODO</Nompagewrap>}/>
        <Route path="/bestillinger/sammenslaaenheter"
               element={<Nompagewrap pagetitle="Sammenslå enheter">...TODO</Nompagewrap>}/>
        <Route path="/bestillinger/kategoriserenhet"
               element={<Nompagewrap pagetitle="Kategoriser enhet">...TODO</Nompagewrap>}/>
        <Route path="/bestillinger/list" element={<BestillingList/>}/>
        <Route path="/bestillingsgodkjenning" element={<Bestillingsgodkjenning/>}/>
        <Route path="/enhetstyper" element={<Enhetstyper/>}/>
        <Route element={<Navigate to="/"/>}/>
      </Routes>
  );
}

function Main(): JSX.Element {
  return (
    <BestillingContext.Provider value={useBestillingContext()}>
       <AppRouter />
        {/* <Root />*/}
    </BestillingContext.Provider>
  );
}

ReactDOM.render(
  <React.StrictMode>
    <BrowserRouter>
      <ApolloProvider client={apolloClient}>
        <Main />
      </ApolloProvider>
    </BrowserRouter>
  </React.StrictMode>,
  document.getElementById("root")
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
