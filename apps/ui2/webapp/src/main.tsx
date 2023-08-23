import "@navikt/ds-css";
import "./designSystemOverrides.css";

import { Loader } from "@navikt/ds-react";
import React from "react";
import { QueryClient, QueryClientProvider } from "react-query";
import { RouterProvider } from "react-router-dom";

import { useGetSession } from "./api/useGetSession";
import { router } from "./routes";

const queryClient = new QueryClient();

export function Main() {
  const sessionQuery = useGetSession();

  if (sessionQuery.isLoading) {
    return <Loader />;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
