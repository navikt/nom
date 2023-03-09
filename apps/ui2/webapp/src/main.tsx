import "@navikt/ds-css";
import "@navikt/ds-css-internal";
import "./designSystemOverrides.css";

import { ApolloProvider } from "@apollo/client";
import { css } from "@emotion/react";
import { Loader, Modal } from "@navikt/ds-react";
import React from "react";
import { BrowserRouter } from "react-router-dom";

import { apolloClient } from "./api/apolloclient";
import type { SessionInterface } from "./api/getSession";
import { getSession } from "./api/getSession";
import { Footer, footerHeight } from "./components/Footer";
import NavHeader, { headerHeight } from "./components/NavHeader";
import PilotBanner, { pilotBannerHeight } from "./components/PilotBanner";
import AppRoutes from "./routes";

const layoutStyle = css`
  background-color: var(--a-gray-100);
  min-height: calc(
    100vh - ${headerHeight} - ${pilotBannerHeight} - ${footerHeight}
  );
  width: 100%;
  display: flex;
`;

export enum UserRole {
  ROLE_NONE = "ROLE_NONE",
  ROLE_WRITE = "ROLE_WRITE",
  ROLE_ADMIN = "ROLE_ADMIN",
}

const UserSessionContext = React.createContext<SessionInterface>({
  roles: [],
  navident: "?",
  name: "",
});

export function useSessionContext() {
  return React.useContext(UserSessionContext);
}

export function Main() {
  const [rolesFetched, setRolesFetched] = React.useState(false);
  const [sessionInfo, setSessionInfo] = React.useState<SessionInterface>();

  React.useEffect(() => {
    getSession()
      .then((index) => setSessionInfo(index))
      .finally(() => setRolesFetched(true));
  }, []);

  /**
   * Must set where a modal is mounted for screen readers to work properly: https://aksel.nav.no/designsystem/komponenter/modal
   * Not sure if setting it at the root like this is sufficient, or if it should be part of every modal's useEffects()
   */
  if (Modal.setAppElement) {
    Modal.setAppElement(document.querySelector("#root"));
  }

  if (!rolesFetched || !sessionInfo) {
    return <Loader />;
  }

  return (
    <BrowserRouter>
      <ApolloProvider client={apolloClient}>
        <UserSessionContext.Provider value={sessionInfo}>
          <NavHeader />
          <PilotBanner />
          <div css={layoutStyle}>
            <div css={css({ width: "100%" })}>
              <AppRoutes />
            </div>
          </div>
          <Footer />
        </UserSessionContext.Provider>
      </ApolloProvider>
    </BrowserRouter>
  );
}
