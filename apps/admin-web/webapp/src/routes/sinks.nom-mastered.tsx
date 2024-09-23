import { Alert, Button, Heading, Table, TextField } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm } from "react-hook-form";

import type { searchTypeWithNavident } from "~/api/queries.ts";
import {
  getNomMasteredStateOptions,
  getOrgtilknytninger,
  getRessurs,
} from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import type { MasteredRessursDTO } from "~/types/nom-admin/nom-admin-models.ts";
import {
  formatDisplayDatetime,
  formatVisningsnavn,
  isTodayOrAfter,
  isTodayOrBefore,
} from "~/utils.ts";

export function NavidentOffsetSearchBar({
  search,
}: {
  search: searchTypeWithNavident;
}) {
  const methods = useForm({ defaultValues: search });
  const navigate = useNavigate();

  const submitHandler = methods.handleSubmit((values) => {
    navigate({
      search: (prev) => {
        const overwriteOffset = {
          fromOffset: Number.parseInt((values.fromOffset as never) || "0"),
        };
        return { ...prev, ...values, ...overwriteOffset };
      },
    });
  });

  const tilbakestill = () => {
    methods.reset({ navident: "", fromOffset: 0 });
    navigate({
      search: {},
      replace: true,
    });
  };

  return (
    <form className="flex gap-4 p-6" onSubmit={submitHandler}>
      <TextField label="Navident" {...methods.register("navident")} />
      <TextField label="Fra offset" {...methods.register("fromOffset")} />
      <Button className="h-fit mt-8" type="submit">
        Søk
      </Button>
      <Button
        className="h-fit mt-8"
        onClick={tilbakestill}
        type="button"
        variant="tertiary"
      >
        Tilbakestill
      </Button>
    </form>
  );
}

export const Route = createFileRoute("/sinks/nom-mastered")({
  loaderDeps: ({ search }) => search,
  loader: ({ context }) => {
    context.queryClient.prefetchQuery(getNomMasteredStateOptions(undefined, 0));
  },
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <NavidentOffsetSearchBar search={search} />
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
  const search = Route.useSearch<searchTypeWithNavident>();
  const { navident, fromOffset: offset } = search;
  const nomMasteredObjekter = useSuspenseQuery({
    ...getNomMasteredStateOptions(navident, offset),
  }).data;

  const { data, sort, handleSort } = useTableSort(
    nomMasteredObjekter,
    "opprettet",
    "descending",
  );

  return (
    <>
      <Heading className="mt-6 mb-6" level="2" size="large">
        {nomMasteredObjekter.length} medarbeidere er mastret i NOM!
      </Heading>
      <Table
        onSortChange={(sortKey) => handleSort(sortKey)}
        size="small"
        sort={sort}
        zebraStripes
      >
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">NAV-ident</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Visningsnavn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey="opprettet" sortable>
              Tidspunkt opprettet
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Sektor</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Orgtilknytning</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {data.map((ressursEvent) => {
            return (
              <RessursTableRow
                key={ressursEvent.navident}
                ressursEvent={ressursEvent}
              />
            );
          })}
        </Table.Body>
      </Table>
    </>
  );
}

export function RessursTableRow({
  ressursEvent,
}: {
  ressursEvent: MasteredRessursDTO;
}) {
  const ressurs = useSuspenseQuery({
    ...getRessurs(ressursEvent.navident),
  }).data;

  const orgtilknytninger = useSuspenseQuery({
    ...getOrgtilknytninger(ressursEvent.navident),
  }).data;

  const dolOrgtilknytning = orgtilknytninger.find(
    (orgtilknytning) =>
      orgtilknytning.erDagligOppfolging &&
      isTodayOrBefore(new Date(orgtilknytning.gyldigFom)) &&
      (!orgtilknytning.gyldigTom ||
        isTodayOrAfter(new Date(orgtilknytning.gyldigTom))),
  );

  return (
    <Table.Row>
      <Table.HeaderCell scope="row">
        <Link
          search={{ filterText: ressursEvent.navident }}
          to="/state/ressurs"
        >
          {ressursEvent.navident}
        </Link>
      </Table.HeaderCell>
      <Table.DataCell scope="row">
        {formatVisningsnavn(ressurs.visningsnavn)}
      </Table.DataCell>
      <Table.DataCell scope="row">
        {formatDisplayDatetime(ressursEvent.opprettet)}
      </Table.DataCell>
      <Table.DataCell scope="row">{ressurs.sektor}</Table.DataCell>
      <Table.DataCell scope="row">
        {dolOrgtilknytning ? dolOrgtilknytning.orgEnhetId : ""}
      </Table.DataCell>
    </Table.Row>
  );
}
