import type { SortState } from "@navikt/ds-react";
import { Alert, Link, Table } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link as TanStackLink } from "@tanstack/react-router";
import sortBy from "lodash/sortBy";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { getRessursStatesOptions, searchSchema } from "~/api/queries.ts";
import type { RessursState } from "~/api-types.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { formatVisningsnavn } from "~/utils.ts";

export const Route = createFileRoute("/state/ressurs")({
  validateSearch: searchSchema.parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <RessursPage />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function RessursPage() {
  const search = Route.useSearch();
  const [sort, setSort] = useState<SortState>({
    orderBy: "navident",
    direction: "ascending",
  });

  const ressurser = useSuspenseQuery({
    ...getRessursStatesOptions(search),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const handleSort = (sortKey: string | undefined) => {
    if (sortKey) {
      setSort({
        orderBy: sortKey,
        direction:
          sort && sortKey === sort.orderBy && sort.direction === "ascending"
            ? "descending"
            : "ascending",
      });
    } else {
      setSort({ orderBy: "navident", direction: "none" });
    }
  };

  const sortedRessurser = sort
    ? sortRessurser({ ressurser: ressurser, sort })
    : ressurser;

  return (
    <Table
      onSortChange={(sortKey) => handleSort(sortKey)}
      size="small"
      sort={sort}
      zebraStripes
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col" sortKey="navident" sortable>
            Navident
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="visningsnavn" sortable>
            Visningsnavn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
            Sektor
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="startdato" sortable>
            Startdato
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
            Sluttdato
          </Table.ColumnHeader>
          <Table.HeaderCell />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedRessurser.map((ressurs) => (
          <Table.Row key={ressurs.fid}>
            <Table.HeaderCell scope="row">
              <Link
                href={`http://nom.nav.no/ressurs/${ressurs.navident}`}
                target="_blank"
              >
                {ressurs.navident}
              </Link>
            </Table.HeaderCell>
            <Table.DataCell>
              {formatVisningsnavn(ressurs.visningsnavn)}
            </Table.DataCell>
            <Table.DataCell>{ressurs.sektor}</Table.DataCell>
            <Table.DataCell>{ressurs.startdato}</Table.DataCell>
            <Table.DataCell>{ressurs.sluttdato}</Table.DataCell>
            <Table.DataCell>
              <TanStackLink
                search={{ filterText: ressurs.fid, fromOffset: 0 }}
                to="/sinks/ressurs"
              >
                Se Events
              </TanStackLink>
              {/*<Button variant="tertiary">Se events</Button>*/}
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}

// TODO: kopiert ukritisk
// function OrgTilknytningDataCell({
//   orgTilknytninger,
// }: {
//   orgTilknytninger: {
//     orgEnhet: {
//       id: string;
//       navn: string;
//     };
//     gyldigFom: string;
//     gyldigTom: string;
//   }[];
// }) {
//   const activeOrgtilknytninger = orgTilknytninger.filter((orgTilknytning) =>
//     isGyldighetsPeriodeActive(
//       orgTilknytning.gyldigFom,
//       orgTilknytning.gyldigTom,
//     ),
//   );
//
//   const pluralDisplay = (
//     <div className="flex flex-row gap-2">
//       <OrgEnhetLink orgenhet={activeOrgtilknytninger[0]?.orgEnhet} />
//       <ExtraRowContent
//         content={activeOrgtilknytninger.slice(1).map((orgTilknytning) => (
//           <OrgEnhetLink
//             key={orgTilknytning.orgEnhet.id}
//             orgenhet={orgTilknytning.orgEnhet}
//           />
//         ))}
//       />
//     </div>
//   );
//
//   return (
//     <Table.DataCell>
//       <Pluralize
//         numberOfElements={activeOrgtilknytninger.length}
//         pluralForm={pluralDisplay}
//         singleForm={
//           <OrgEnhetLink orgenhet={activeOrgtilknytninger[0]?.orgEnhet} />
//         }
//       />
//     </Table.DataCell>
//   );
// }

function sortRessurser({
  ressurser,
  sort,
}: {
  ressurser: RessursState[];
  sort: SortState;
}) {
  const { orderBy, direction } = sort;

  const sortedMembersAscending = sortRessurserAscending(ressurser, orderBy);

  const reversed = direction === "descending";

  return reversed ? sortedMembersAscending.reverse() : sortedMembersAscending;
}

function sortRessurserAscending(ressurser: RessursState[], orderBy: string) {
  if (
    [
      "navident",
      "visningsnavn",
      "startdato",
      "sluttdato",
      "opprettetAv",
      "opprettetDato",
    ].includes(orderBy)
  ) {
    return sortBy(ressurser, orderBy);
  }

  if (orderBy === "sektor") {
    return sortBy(ressurser, (ressurs) => ressurs.sektor);
  }

  return ressurser;
}
