import "@navikt/ds-css";
import "./designSystemOverrides.css";

import { ApolloProvider } from "@apollo/client";
import { css } from "@emotion/react";
import { Alert, Loader } from "@navikt/ds-react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { RouterProvider } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React from "react";
import { Toaster } from "react-hot-toast";

import { apolloClient } from "~/api/apolloclient";
import { useGetSession } from "~/api/useGetSession";
import { router } from "~/reactRouterRoutes";

const queryClient = new QueryClient();

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ReactQueryDevtools buttonPosition="bottom-left" initialIsOpen={false} />
      <ApolloProvider client={apolloClient}>
        <LoadSession>
          <RouterProvider router={router} />
        </LoadSession>
      </ApolloProvider>
      <Toaster toastOptions={{ position: "top-right" }} />
    </QueryClientProvider>
  );
}

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
