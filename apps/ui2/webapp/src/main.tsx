import React from "react";
import { createRoot } from "react-dom/client";
import "@navikt/ds-css";
import "@navikt/ds-css-internal";
import { BrowserRouter } from "react-router-dom";
import { ApolloProvider } from "@apollo/client";
import { apolloClient } from "./api/apolloclient";
import { css } from "@emotion/css";
import AppRoutes from "./routes";
import NavHeader, { headerHeight } from "./components/NavHeader";
import Footer, { footerHeigth } from "./components/Footer";
import PilotBanner from "./components/PilotBanner";

const layoutStyle = css({
  backgroundColor: "#F1F1F1",
  minHeight: `calc(100vh - ${headerHeight} - ${footerHeigth})`,
  width: "100%",
  display: "flex",
});

const container = document.getElementById("root");
// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
const root = createRoot(container!);
root.render(
  <React.StrictMode>
    <BrowserRouter>
      <ApolloProvider client={apolloClient}>
        <NavHeader />
        <PilotBanner />
        <div className={layoutStyle}>
          <div className={css({ width: "100%" })}>
            <AppRoutes />
          </div>
        </div>
        <Footer />
      </ApolloProvider>
    </BrowserRouter>
  </React.StrictMode>
);
