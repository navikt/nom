import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import React from "react";

import { harLederTilgangForNavidentOptions } from "~/api/react-query-endpoints";
import { ModalProvider } from "~/components/modalProvider/ModalProvider";
import { TerminateResourcePage } from "~/pages/ResourcePage/TerminateResourcePage";

export const Route = createFileRoute("/ressurs-terminate/$id")({
  component: () => {
    const hasWriteAccessForRessurs =
      useQuery(harLederTilgangForNavidentOptions(Route.useParams().id)).data ??
      false;

    if (!hasWriteAccessForRessurs) {
      return;
    }

    return (
      <ModalProvider>
        <TerminateResourcePage />
      </ModalProvider>
    );
  },
});
