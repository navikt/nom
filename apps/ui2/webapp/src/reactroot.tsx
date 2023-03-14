import { ApolloProvider } from "@apollo/client";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "react-query";

import { apolloClient } from "~/api/apolloclient";
const queryClient = new QueryClient();

import { Main } from "./main";

createRoot(document.querySelector("#root") as HTMLElement).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ApolloProvider client={apolloClient}>
        <Main />
      </ApolloProvider>
    </QueryClientProvider>
  </StrictMode>
);
