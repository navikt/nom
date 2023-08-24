import "@navikt/ds-css";
import "./designSystemOverrides.css";

import { ApolloProvider } from "@apollo/client";
import React from "react";
import { QueryClient, QueryClientProvider } from "react-query";
import { RouterProvider } from "react-router-dom";

import { apolloClient } from "~/api/apolloclient";

import { router } from "./routes";

const queryClient = new QueryClient();

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ApolloProvider client={apolloClient}>
        <QueryClientProvider client={queryClient}>
          <RouterProvider router={router} />
        </QueryClientProvider>
      </ApolloProvider>
    </QueryClientProvider>
  );
}
