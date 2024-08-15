import { Alert, Heading, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";

import {
  getNomMasteredOptions,
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
  const nomMasteredObjekter = useSuspenseQuery({
    ...getNomMasteredStateOptions(),
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
