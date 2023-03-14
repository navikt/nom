import "@navikt/ds-css";
import "@navikt/ds-css-internal";
import "./designSystemOverrides.css";

import { Loader, Modal } from "@navikt/ds-react";
import React from "react";
import { RouterProvider } from "react-router-dom";

import { useGetSession } from "./api/useGetSession";
import { router } from "./routes";

export function Main() {
  /**
   * Must set where a modal is mounted for screen readers to work properly: https://aksel.nav.no/designsystem/komponenter/modal
   * Not sure if setting it at the root like this is sufficient, or if it should be part of every modal's useEffects()
   */
  if (Modal.setAppElement) {
    Modal.setAppElement(document.querySelector("#root"));
  }

  const sessionQuery = useGetSession();

  if (sessionQuery.isLoading) {
    return <Loader />;
  }

  return <RouterProvider router={router} />;
}
