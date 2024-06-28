import { Alert, Heading, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
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
  const nomMasteredIdenter = useSuspenseQuery({
    ...getNomMasteredOptions(),
  }).data;

  return (
    <>
      <Heading className="mt-6 mb-6" level="2" size="large">
        {nomMasteredIdenter.length} medarbeidere er mastret i NOM!
      </Heading>
      <Table size="small" zebraStripes>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navident</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {nomMasteredIdenter.map((navident) => {
            return (
              <Table.Row key={navident}>
                <Table.HeaderCell scope="row">
                  <Link search={{ filterText: navident }} to="/state/ressurs">
                    {navident}
                  </Link>
                </Table.HeaderCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </>
  );
}
