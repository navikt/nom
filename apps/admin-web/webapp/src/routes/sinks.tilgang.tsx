import { Alert, Button, Link, Table, TextField } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm } from "react-hook-form";

import { getTilgangEventsOptions, searchSchema } from "~/api/queries.ts";
import { EventDateRange } from "~/components/EventDateRange.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { formatDisplayDatetime, formatVisningsnavn } from "~/utils.ts";

export const Route = createFileRoute("/sinks/tilgang")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getTilgangEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => (
    <>
      <SearchBar />
      <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
        <Suspense fallback="aaa">
          <TilgangHistoryPage />
        </Suspense>
      </ErrorBoundary>
    </>
  ),
});

function SearchBar() {
  const search = Route.useSearch();
  const methods = useForm({ defaultValues: search });
  const navigate = useNavigate();

  const submitHandler = methods.handleSubmit((values) => {
    navigate({
      search: (prev) => ({ ...prev, ...values }),
    });
  });

  return (
    <form className="flex gap-4 p-6" onSubmit={submitHandler}>
      <TextField label="Vilkårlig tekst" {...methods.register("filterText")} />
      <TextField label="Fra offset" {...methods.register("fromOffset")} />
      <Button className="h-fit mt-8" type="submit">
        Søk
      </Button>
    </form>
  );
}

function TilgangHistoryPage() {
  const search = Route.useSearch();

  const tilgangEventsQuery = useSuspenseQuery({
    ...getTilgangEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const tilgangEvents = tilgangEventsQuery.data ?? [];

  return (
    <div>
      <EventDateRange events={tilgangEvents} />

      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navident</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Endring</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Dato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Opprinnelse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any -- events can be of any type */}
          {tilgangEvents.map((tilgangEvent: any, index: number) => {
            return (
              <Table.Row key={index}>
                <Table.DataCell>
                  <Link
                    href={`http://nom.nav.no/tilgang/${tilgangEvent.additionalData?.navident}`}
                    target="_blank"
                  >
                    {tilgangEvent.additionalData?.navident}
                  </Link>
                </Table.DataCell>
                <Table.DataCell scope="row">
                  {formatVisningsnavn(tilgangEvent.data?.visningsnavn)}
                </Table.DataCell>

                <Table.DataCell>
                  <JsonModal data={tilgangEvent} text={tilgangEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(
                    tilgangEvent.metadata?.createdTimestamp,
                  )}
                </Table.DataCell>
                <Table.DataCell>
                  {tilgangEvent.metadata.inputMetadata.inputInterfaceMethodName}{" "}
                  via {tilgangEvent.metadata.inputMetadata.inputInterfaceName}
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
