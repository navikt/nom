import { FilesIcon } from "@navikt/aksel-icons";
import { Alert, Button, Heading, Table, TextField } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import type { AnyRoute } from "@tanstack/react-router";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm } from "react-hook-form";

import {
  getNomMasteredStateOptions,
  getOrgtilknytningerForListOfNavidents,
  getRessurser,
  searchSchemaWithNavident,
} from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { EmbeddedJsonView } from "~/components/KafkaEventTable.tsx";
import { copyToClipboard } from "~/components/quickdialog.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import type { MasteredRessursDTO } from "~/types/nom-admin/nom-admin-models.ts";
import type {
  OrgtilknytningState,
  Ressurs,
} from "~/types/nom-api/nom-api-models.ts";
import {
  formatDisplayDatetime,
  formatVisningsnavn,
  isTodayOrAfter,
  isTodayOrBefore,
} from "~/utils.ts";

export function NavidentOffsetSearchBar({ route }: { route: AnyRoute }) {
  const search = route.useSearch();
  const methods = useForm({ defaultValues: search });
  const navigate = useNavigate();

  const submitHandler = methods.handleSubmit((values) => {
    navigate({
      to: route.to,
      search: {
        ...search,
        navident:
          values.navident && values.navident !== ""
            ? values.navident
            : undefined,
        fromOffset: values.fromOffset
          ? Number.parseInt(values.fromOffset)
          : undefined,
      },
    }).then();
  });

  const tilbakestill = () => {
    methods.reset({
      navident: undefined,
      fromOffset: undefined,
    });

    const newSearch = {
      ...search,
      navident: undefined,
      fromOffset: undefined,
    };

    navigate({
      search: newSearch,
      replace: true,
    }).then();
  };

  return (
    <form className="flex gap-4 p-6" onSubmit={submitHandler}>
      <TextField label="Navident" {...methods.register("navident")} />
      <TextField label="Fra offset" {...methods.register("fromOffset")} />
      <Button className="h-fit mt-8 self-end" type="submit">
        Søk
      </Button>
      <Button
        className="h-fit mt-8 self-end"
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
  validateSearch: searchSchemaWithNavident.parse,
  component: () => {
    return (
      <>
        <NavidentOffsetSearchBar route={Route} />

        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <NomMasteredPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function getOrgtilknytning(
  ressursMastered: MasteredRessursDTO,
  orgtilknytninger: Map<string, OrgtilknytningState[]>,
) {
  return orgtilknytninger.get(ressursMastered.navident as string) ?? [];
}

function NomMasteredPage() {
  const search = Route.useSearch();
  const { navident, fromOffset: offset } = search;
  const nomMasteredObjekter = useSuspenseQuery({
    ...getNomMasteredStateOptions(navident, offset),
  }).data;

  const { data, sort, handleSort } = useTableSort(
    nomMasteredObjekter,
    "opprettet",
    "descending",
  );

  const alleNavidenter = nomMasteredObjekter.map(
    (ressursEvent) => ressursEvent.navident,
  ) as string[];

  const ressurser = useSuspenseQuery({
    ...getRessurser(alleNavidenter),
  }).data;

  const orgtilknytninger = useSuspenseQuery({
    ...getOrgtilknytningerForListOfNavidents(alleNavidenter),
  }).data;

  return (
    <>
      <Heading className="mt-6 mb-6" level="2" size="large">
        {nomMasteredObjekter.length} medarbeidere er mastret i NOM!
      </Heading>
      <Button
        icon={<FilesIcon />}
        onClick={() => {
          const rows = data.map((ressursMastered) => ({
            ...ressursMastered,
            ressurs: ressurser.find(
              (value) => value.navident === ressursMastered.navident,
            ),
            orgtilknytninger: getOrgtilknytning(
              ressursMastered,
              orgtilknytninger,
            ),
          }));
          return copyToClipboard(rows);
        }}
        size={"xsmall"}
        variant={"secondary"}
      >
        Kopier JSON[]
      </Button>
      <Table
        onSortChange={(sortKey) => handleSort(sortKey)}
        size="small"
        sort={sort}
        zebraStripes
      >
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
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
          {data.map((ressursMastered) => {
            return (
              <RessursTableRow
                key={ressursMastered.navident}
                orgtilknytninger={getOrgtilknytning(
                  ressursMastered,
                  orgtilknytninger,
                )}
                ressurs={ressurser.find(
                  (value) => value.navident === ressursMastered.navident,
                )!}
                ressursMastered={ressursMastered}
              />
            );
          })}
        </Table.Body>
      </Table>
    </>
  );
}

export function RessursTableRow({
  ressursMastered,
  orgtilknytninger,
  ressurs,
}: {
  orgtilknytninger: OrgtilknytningState[];
  ressursMastered: MasteredRessursDTO;
  ressurs: Ressurs;
}) {
  const dolOrgtilknytning = determineDolOrgtilknytning(orgtilknytninger);

  return (
    <Table.ExpandableRow
      content={
        <EmbeddedJsonView
          jsonData={{ ressurs, ressursMastered, dolOrgtilknytning }}
        />
      }
    >
      <Table.HeaderCell scope="row">
        <Link
          search={{ filterText: ressursMastered.navident }}
          to="/state/ressurs"
        >
          {ressursMastered.navident}
        </Link>
      </Table.HeaderCell>
      <Table.DataCell scope="row">
        {formatVisningsnavn(ressurs.visningsnavn)}
      </Table.DataCell>
      <Table.DataCell scope="row">
        {formatDisplayDatetime(ressursMastered.opprettet)}
      </Table.DataCell>
      <Table.DataCell scope="row">{ressurs.sektor}</Table.DataCell>
      <Table.DataCell scope="row">
        {dolOrgtilknytning ? dolOrgtilknytning.orgEnhetId : ""}
      </Table.DataCell>
    </Table.ExpandableRow>
  );
}

function determineDolOrgtilknytning(orgtilknytninger: OrgtilknytningState[]) {
  return (
    orgtilknytninger?.find(
      (orgtilknytning) =>
        orgtilknytning.erDagligOppfolging &&
        isTodayOrBefore(new Date(orgtilknytning.gyldigFom)) &&
        (!orgtilknytning.gyldigTom ||
          isTodayOrAfter(new Date(orgtilknytning.gyldigTom))),
    ) ?? null
  );
}
