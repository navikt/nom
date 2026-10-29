import "@navikt/ds-css";
import "./designSystemOverrides.css";
import "~/types/yupUtility";

import { ApolloProvider } from "@apollo/client/react";
import { css } from "@emotion/react";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { TracingInstrumentation } from "@grafana/faro-web-tracing";
import { Alert, Box, Heading } from "@navikt/ds-react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import {
  createRouteMask,
  createRouter,
  RouterProvider,
} from "@tanstack/react-router";
import React, { type ReactNode, StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Toaster } from "react-hot-toast";

import { apolloClient } from "~/api/apolloclient";
import { useGetSession } from "~/api/useGetSession";
import { LoaderWithText } from "~/components/LoaderWithText";
import { useGetEnvironmentConfig } from "~/customUtils/environmentUtils";

import { ExpiryNotifier } from "./components/ExpiryNotifier";
import { routeTree } from "./routeTree.gen";

// Don't initialize faro when running the dev-server
if (import.meta.env.PROD) {
  const inDevelopment =
    !!globalThis.location.hostname.match(/localhost|\.dev\./);

  const url = inDevelopment
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
  scrollRestoration: true,
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

createRoot(document.querySelector("#root") as HTMLElement).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ExpiryNotifier />

      <ReactQueryDevtools buttonPosition="bottom-left" initialIsOpen={false} />

      <ApolloProvider client={apolloClient}>
        <LoadSessionAndEnvironment>
          <RouterProvider router={router} />
        </LoadSessionAndEnvironment>
      </ApolloProvider>

      <Toaster toastOptions={{ position: "top-right" }} />
    </QueryClientProvider>
  </StrictMode>,
);

function LoadSessionAndEnvironment({ children }: { children: ReactNode }) {
  const session = useGetSession();
  const environmentVariables = useGetEnvironmentConfig();

  if (session.isLoading || environmentVariables.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  if (session.isError || environmentVariables.isError) {
    return (
      <Alert variant="error">
        Det oppstod problemer med lasting av konfigurasjon
      </Alert>
    );
  }

  if (!session.data || !environmentVariables.data) {
    return;
  }

  return children;
}
