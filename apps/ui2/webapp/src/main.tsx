import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { TracingInstrumentation } from "@grafana/faro-web-tracing";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import { isInDevelopment } from "~/utils";

import { App } from "./App";

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

createRoot(document.querySelector("#root") as HTMLElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
