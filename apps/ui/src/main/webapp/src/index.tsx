import React from "react";
import ReactDOM from "react-dom";
import { BrowserRouter, Redirect, Route, Switch } from "react-router-dom";
import About from "./pages/about/About";
import Graphql from "./pages/graphql/Graphql";
import Root from "./pages/root/Root";
import * as serviceWorker from "./serviceWorker";

function AppRouter(): JSX.Element{
  return (
    <Switch>
      <Route exact path="/index.html">
        <Redirect to="/" />
      </Route>
      <Route exact path="/">
        <Root />
      </Route>
      <Route exact path="/about">
        <About />
      </Route>
      <Route exact path="/graphql">
        <Redirect to="/graphql/4121803" />
      </Route>
      <Route exact path="/graphql/:id">
        <Graphql />
      </Route>
      <Route>
        {/* default..  */}
        <Redirect to="/" />
      </Route>
    </Switch>
  );
}

ReactDOM.render(
  <React.StrictMode>
    <BrowserRouter>
      <AppRouter />
    </BrowserRouter>
  </React.StrictMode>,
  document.getElementById("root")
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();

