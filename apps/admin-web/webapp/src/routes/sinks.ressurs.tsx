import { Alert, Button, Link, Table, TextField } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm } from "react-hook-form";

import { getRessursEventsOptions, searchSchema } from "~/api/queries.ts";
import { EventDateRange } from "~/components/EventDateRange.tsx";
import { JsonModal } from "~/components/JsonModal.tsx";
import { NavidentNameLookup } from "~/components/NavidentNameLookup.tsx";
import { formatDisplayDatetime, formatVisningsnavn } from "~/utils.ts";

export const Route = createFileRoute("/sinks/ressurs")({
  loaderDeps: ({ search }) => search,
  loader: ({ context, deps }) => {
    context.queryClient.prefetchQuery(getRessursEventsOptions(deps));
  },
  validateSearch: searchSchema.parse,
  component: () => (
    <>
      <SearchBar />
      <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
        <Suspense fallback="aaa">
          <RessursHistoryPage />
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

function RessursHistoryPage() {
  const search = Route.useSearch();

  const ressursEventsQuery = useSuspenseQuery({
    ...getRessursEventsOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const ressursEvents = ressursEventsQuery.data ?? [];

  return (
    <div>
      <EventDateRange events={ressursEvents} />
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
          {ressursEvents.map((ressursEvent, index) => {
            return (
              <Table.Row key={index}>
                <Table.DataCell>
                  <Link
                    href={`http://nom.nav.no/ressurs/${ressursEvent.additionalData?.navident}`}
                    target="_blank"
                  >
                    {ressursEvent.additionalData?.navident}
                  </Link>
                </Table.DataCell>
                <Table.DataCell scope="row">
                  {formatVisningsnavn(ressursEvent.data?.visningsnavn)}
                </Table.DataCell>

                <Table.DataCell>
                  <JsonModal data={ressursEvent} text={ressursEvent.type} />
                </Table.DataCell>
                <Table.DataCell>
                  {formatDisplayDatetime(
                    ressursEvent.metadata?.createdTimestamp,
                  )}
                </Table.DataCell>
                <Table.DataCell className="text-medium">
                  Utført av{" "}
                  <b>
                    <NavidentNameLookup
                      navident={
                        ressursEvent.metadata.inputMetadata.origination
                          .initiatedBy
                      }
                    />{" "}
                  </b>
                  fra{" "}
                  <b>
                    {
                      ressursEvent.metadata.inputMetadata.origination
                        .initiatedFrom
                    }
                  </b>
                </Table.DataCell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table>
    </div>
  );
}
