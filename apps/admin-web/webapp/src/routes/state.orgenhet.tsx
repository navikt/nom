import { Alert, Select, Table } from "@navikt/ds-react";
import {
  useMutation,
  useQueryClient,
  useSuspenseQuery,
} from "@tanstack/react-query";
import { createFileRoute, Link as TanStackLink } from "@tanstack/react-router";
import React, { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { Toaster } from "react-hot-toast";
import { z } from "zod";

import { oppdaterOrgEnhetType } from "~/api/apiQueries.ts";
import { getOrgenhetStatesOptions, searchSchema } from "~/api/queries.ts";
import { useHasAdminAccess } from "~/api/useGetSession.tsx";
import { BigLoader } from "~/components/BigLoader.tsx";
import { GenericSearchBar } from "~/components/GenericSearchBar.tsx";
import { NomLink } from "~/components/NomLink.tsx";
import { showToast } from "~/hooks/showToast.tsx";
import { useTableSort } from "~/hooks/useTableSort.ts";
import type { OrgEnhetState } from "~/types/nom-admin/nom-admin-models.ts";
import { OrgEnhetsType } from "~/types/nom-admin/nom-admin-models.ts";

export const Route = createFileRoute("/state/orgenhet")({
  validateSearch: searchSchema.extend({
    allowEdit: z.boolean().optional(),
  }).parse,
  component: () => {
    const search = Route.useSearch();
    return (
      <>
        <GenericSearchBar editButton={true} search={search} />
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgenhetPage />
          </Suspense>
        </ErrorBoundary>
        <Toaster toastOptions={{ position: "top-right" }} />
      </>
    );
  },
});

function OrgenhetPage() {
  const hasAdminAccess = useHasAdminAccess();
  const search = Route.useSearch();

  const orgenheter = useSuspenseQuery({
    ...getOrgenhetStatesOptions({
      filterText: search.filterText,
      fromOffset: search.fromOffset,
    }),
    gcTime: Number.POSITIVE_INFINITY,
    staleTime: Number.POSITIVE_INFINITY,
  }).data;

  const allowEdit = search.allowEdit;

  const { data, sort, handleSort } = useTableSort(orgenheter, "id");

  return (
    <Table
      onSortChange={(sortKey) => handleSort(sortKey)}
      size="small"
      sort={sort}
      zebraStripes
    >
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader scope="col" sortKey="id" sortable>
            Id
          </Table.ColumnHeader>
          <Table.ColumnHeader
            scope="col"
            sortKey="orgEnhetsType"
            sortable
            style={{ maxWidth: "160px" }}
          >
            OrgEnhetsType
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="navn" sortable>
            Navn
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Gyldig fra</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Gyldig til</Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="agressoId" sortable>
            agressoId
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="orgNiv" sortable>
            Orgniv
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="agressoId" sortable>
            remedyEnhetId
          </Table.ColumnHeader>
          <Table.ColumnHeader scope="col" sortKey="nomNivaa" sortable>
            nomNivaa
          </Table.ColumnHeader>
          <Table.ColumnHeader
            scope="col"
            sortKey="agressoOrgenhetType"
            sortable
          >
            agressoOrgenhetType
          </Table.ColumnHeader>
          <Table.HeaderCell />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {data.map((orgEnhet) => {
          return (
            <Table.Row key={orgEnhet.fid}>
              <Table.HeaderCell scope="row">
                <NomLink path={`org/${orgEnhet.id}`}>{orgEnhet.id}</NomLink>
              </Table.HeaderCell>
              {hasAdminAccess ? (
                <OrgEnhetsTypeDataCell
                  allowEdit={allowEdit}
                  orgEnhet={orgEnhet}
                  style={{ maxWidth: "160px" }}
                />
              ) : (
                <Table.DataCell>{orgEnhet.orgEnhetsType}</Table.DataCell>
              )}
              <Table.DataCell>{orgEnhet.navn}</Table.DataCell>
              <Table.DataCell>{orgEnhet.gyldigFom}</Table.DataCell>
              <Table.DataCell>{orgEnhet.gyldigTom}</Table.DataCell>
              <Table.DataCell>{orgEnhet.agressoId}</Table.DataCell>
              <Table.DataCell>{orgEnhet.agressoOrgNivaa}</Table.DataCell>
              <Table.DataCell>{orgEnhet.agressoId}</Table.DataCell>
              <Table.DataCell>{orgEnhet.nomNivaa}</Table.DataCell>
              <Table.DataCell>{orgEnhet.agressoOrgenhetType}</Table.DataCell>
              <Table.DataCell>
                <TanStackLink
                  search={{ filterText: orgEnhet.fid, fromOffset: undefined }}
                  to="/sinks/orgenhet"
                >
                  Se Events
                </TanStackLink>
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}

function OrgEnhetsTypeDataCell({
  orgEnhet,
  allowEdit,
  style,
}: {
  orgEnhet: OrgEnhetState;
  allowEdit: boolean | undefined;
  style?: React.CSSProperties | undefined;
}) {
  const queryClient = useQueryClient();

  const search = Route.useSearch();

  const updateOrgEnhetsTypeMutation = useMutation({
    mutationFn: (newOrgEnhetsType: OrgEnhetsType | null) =>
      oppdaterOrgEnhetType({
        nomId: orgEnhet.id,
        orgEnhetsType: newOrgEnhetsType ?? undefined,
      }),
    onSuccess: (_, newOrgEnhetsType) => {
      /*
         When this mutation returns, we have only sent a message to Kafka to change the orgEnhetsType.
         It is not immediately available in the orgEnhet state.
         Therefore, we do a manual optimistic update of the orgEnhetsType for this orgEnhet in the cache.
      */

      queryClient.setQueryData<OrgEnhetState[]>(
        [
          "STATE",
          "ORGENHET",
          { filterText: search.filterText, fromOffset: search.fromOffset },
        ],
        (previousData) =>
          (previousData ?? []).map((oe) =>
            oe.id === orgEnhet.id
              ? { ...oe, orgEnhetsType: newOrgEnhetsType ?? undefined }
              : oe,
          ),
      );

      showToast({ text: "OrgEnhetsType endret" });
    },
  });

  return (
    <Table.DataCell style={style}>
      <Select
        hideLabel
        label=""
        onChange={(event) => {
          const orgEnhetsType =
            event.target.value === ""
              ? null
              : (event.target.value as OrgEnhetsType);
          updateOrgEnhetsTypeMutation.mutate(orgEnhetsType);
        }}
        readOnly={!allowEdit}
        size="small"
        value={orgEnhet?.orgEnhetsType ?? ""}
      >
        <option value=""></option>
        {Object.values(OrgEnhetsType).map((orgEnhetsType) => (
          <option key={orgEnhetsType} value={orgEnhetsType}>
            {orgEnhetsType}
          </option>
        ))}
      </Select>
    </Table.DataCell>
  );
}
