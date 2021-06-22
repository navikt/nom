import React from "react";
import ReactDOM from "react-dom";
import {BrowserRouter, Redirect, Route, Switch} from "react-router-dom";
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
import {ApolloProvider} from '@apollo/client';
import {apolloClient} from './api/apollo_client';
import {BestillingList} from './pages/bestillinger/list/BestillingList';
import {BestillingContext, useBestillingContext} from "./pages/bestillinger/BestillingContext";

function AppRouter(): JSX.Element {
  return (
    <Switch>
      <Route exact path="/index.html">
        <Redirect to="/"/>
      </Route>
      <Route exact path="/">
        <Root/>
      </Route>
      <Route exact path="/about">
        <About/>
      </Route>
      <Route exact path="/graphql">
        <Redirect to="/graphql/4121803"/>
      </Route>
      <Route exact path="/graphql/:id">
        <Graphql/>
      </Route>
      <Route exact path="/organisasjonsenheter">
        <Redirect to="/organisasjonsenheter/NAV"/>
      </Route>
      <Route exact path="/organisasjonsenheter/:id">
        <Organisasjonsenheter/>
      </Route>
      <Route exact path="/medarbeidere">
        <MedarbeidereLinkview/>
      </Route>
      <Route exact path="/medarbeidere/:id">
        <MedarbeidereLinkview/>
      </Route>
      <Route exact path="/bestillinger">
        <Bestillinger/>
      </Route>
      <Route exact path="/bestillinger/nybruker">
        <Nybruker/>
      </Route>
      <Route exact path="/bestillinger/nyenhet">
        <Nyenhet/>
      </Route>
      <Route exact path="/bestillinger/flytteenhet">
        <Nompagewrap pagetitle="Flytte enhet">...TODO</Nompagewrap>
      </Route>
      <Route exact path="/bestillinger/splitteenhet">
        <Nompagewrap pagetitle="Splitte enhet">...TODO</Nompagewrap>
      </Route>
      <Route exact path="/bestillinger/sammenslaaenheter">
        <Nompagewrap pagetitle="Sammenslå enheter">...TODO</Nompagewrap>
      </Route>
      <Route exact path="/bestillinger/kategoriserenhet">
        <Nompagewrap pagetitle="Kategoriser enhet">...TODO</Nompagewrap>
      </Route>
      <Route exact path="/bestillinger/list">
        <BestillingList/>
      </Route>
      <Route exact path="/bestillingsgodkjenning">
        <Bestillingsgodkjenning/>
      </Route>
      <Route>
        {/* default..  */}
        <Redirect to="/"/>
      </Route>
    </Switch>
  );
}

function Main(): JSX.Element {
  return (
    <BestillingContext.Provider value={useBestillingContext()}>
      <AppRouter/>
    </BestillingContext.Provider>
  );
}

ReactDOM.render(
  <React.StrictMode>
    <BrowserRouter>
      <ApolloProvider client={apolloClient}>
        <Main/>
      </ApolloProvider>
    </BrowserRouter>
  </React.StrictMode>,
  document.getElementById("root")
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
