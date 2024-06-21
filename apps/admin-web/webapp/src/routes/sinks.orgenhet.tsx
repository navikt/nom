import { Alert, Button, Table, TextField } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm } from "react-hook-form";

import { getOrgenhetEventsOptions, searchSchema } from "~/api/queries.ts";
import { EventDateRange } from "~/components/EventDateRange.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { formatDisplayDatetime } from "~/utils.ts";

export const Route = createFileRoute("/sinks/orgenhet")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getOrgenhetEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => (
    <>
      <SearchBar />
      <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
        <Suspense fallback="aaa">
          <OrgenhetHistoryPage />
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

function OrgenhetHistoryPage() {
  const search = Route.useSearch();

  const orgEnhetEventsQuery = useSuspenseQuery({
    ...getOrgenhetEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const orgEnhetEvents = orgEnhetEventsQuery.data;

  return (
    <div>
      <EventDateRange events={orgEnhetEvents} />
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Endring</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Dato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Opprinnelse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any -- events can be of any type */}
          {orgEnhetEvents.map((orgEnhetEvent: any, index: number) => {
            return (
              <Table.Row key={index}>
                <Table.DataCell>
                  <JsonModal data={orgEnhetEvent} text={orgEnhetEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(
                    orgEnhetEvent.metadata?.createdTimestamp,
                  )}
                </Table.DataCell>
                <Table.DataCell>
                  {
                    orgEnhetEvent.metadata.inputMetadata
                      .inputInterfaceMethodName
                  }{" "}
                  via {orgEnhetEvent.metadata.inputMetadata.inputInterfaceName}
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
