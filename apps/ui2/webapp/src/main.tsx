import "@navikt/ds-css";
import "./designSystemOverrides.css";
import "~/types/yupUtility";

import { ApolloProvider } from "@apollo/client";
import { css } from "@emotion/react";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { TracingInstrumentation } from "@grafana/faro-web-tracing";
import { Alert, Box, Heading, Loader } from "@navikt/ds-react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import {
  createRouteMask,
  createRouter,
  RouterProvider,
} from "@tanstack/react-router";
//import FlagProvider from "@unleash/proxy-client-react";
import React, { type ReactNode, StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Toaster } from "react-hot-toast";

import { apolloClient } from "~/api/apolloclient";
import { useGetSession } from "~/api/useGetSession";
import { isInDevelopment } from "~/utils";

import { routeTree } from "./routeTree.gen";

// Don't initialize faro when running the dev-server
if (process.env.NODE_ENV === "production") {
  const url = isInDevelopment()
    ? "https://telemetry.ekstern.dev.nav.no/collect"
    : "https://telemetry.nav.no/collect";

  initializeFaro({
    url,
    app: {
      name: "nom-ui",
    },
    instrumentations: [
      ...getWebInstrumentations(),
      new TracingInstrumentation(),
    ],
  });
}

const queryClient = new QueryClient();

/**
 * Hide search params when creating ressurs
 */
const sensitiveDataMask = createRouteMask({
  routeTree,
  from: "/ressurs/create/$step",
  to: "/ressurs/create/$step",
  search: undefined,
});

const router = createRouter({
  routeTree,
  routeMasks: [sensitiveDataMask],
  context: {
    queryClient,
  },
  defaultNotFoundComponent: () => (
    <Box
      css={css`
        text-align: center;
        padding: var(--a-spacing-8);
      `}
    >
      <Heading level="1" size="large" spacing>
        Beklager, vi fant ikke siden
      </Heading>
    </Box>
  ),
  defaultPreload: "intent",
  // Since we're using React Query, we don't want loader calls to ever be stale
  // This will ensure that the loader is always called when the route is preloaded or visited
  defaultPreloadStaleTime: 0,
});

// Register things for typesafety
declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

/*const UNLEASH_URL = process.env["UNLEASH_SERVER_API_URL"];
const UNLEASH_CLIENT_KEY = process.env["UNLEASH_SERVER_API_TOKEN"];

const config = {
  url: UNLEASH_URL as string, // Your front-end API URL or the Unleash proxy's URL (https://<proxy-url>/proxy)
  clientKey: UNLEASH_CLIENT_KEY as string, // A client-side API token OR one of your proxy's designated client keys (previously known as proxy secrets)
  refreshInterval: 15, // How often (in seconds) the client should poll the proxy for updates
  appName: "nom-ui2", // The name of your application. It's only used for identifying your application
};*/

createRoot(document.querySelector("#root") as HTMLElement).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ReactQueryDevtools buttonPosition="bottom-left" initialIsOpen={false} />
      <ApolloProvider client={apolloClient}>
        <LoadSession>
          <RouterProvider router={router} />
        </LoadSession>
      </ApolloProvider>
      <Toaster toastOptions={{ position: "top-right" }} />
    </QueryClientProvider>
  </StrictMode>,
);

function LoadSession({ children }: { children: ReactNode }) {
  const session = useGetSession();

  if (session.isLoading) {
    return (
      <Loader
        css={css`
          position: absolute;
          top: 50%;
          left: 50%;
        `}
      />
    );
  }

  if (session.isError) {
    return <Alert variant="error">Fant ikke sesjonsinfo</Alert>;
  }

  return children;
}
