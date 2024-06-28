import { Alert } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getNomMasteredOptions } from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";

export const Route = createFileRoute("/sinks/nom-mastered")({
  loaderDeps: ({ search }) => search,
  loader: ({ context }) => {
    context.queryClient.prefetchQuery(getNomMasteredOptions());
  },
  component: () => {
    return (
      <>
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <NomMasteredPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function NomMasteredPage() {
  return <div></div>;
}
