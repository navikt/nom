import "react-json-view-lite/dist/index.css";

import { css } from "@emotion/react";
import { Alert, Loader, Pagination, Table } from "@navikt/ds-react";
import { useSearch } from "@tanstack/react-router";
import { useState } from "react";
import { allExpanded, defaultStyles, JsonView } from "react-json-view-lite";
import { useQuery } from "react-query";

import type { RessursFields } from "~/api/react-query-endpoints";
import {
  getAlleRessurser,
  getUnderliggendeOrgenheter,
} from "~/api/react-query-endpoints";
import { OrgEnhetSearch } from "~/pages/AdminPage/components/OrgEnhetSearch";
import { ressurserAdminRoute } from "~/reactRouterRoutes";

export function RessurserAdminPage() {
  const ressursFilterValues = useSearch({ from: ressurserAdminRoute.id });
  // Using react query instead of apollo because it runs  more smoothly
  const alleRessurser = useQuery<RessursFields[], unknown>({
    ...getAlleRessurser,
  });

  const underliggendeOrgenheter = useQuery<string[], unknown>({
    queryKey: getUnderliggendeOrgenheter.queryKey(
      ressursFilterValues.orgEnhet as string,
    ),
    queryFn: () =>
      getUnderliggendeOrgenheter.queryFn(
        ressursFilterValues.orgEnhet as string,
      ),
    enabled: Boolean(ressursFilterValues.orgEnhet),
  });

  const [page, setPage] = useState(1);

  if (alleRessurser.isLoading) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          padding-top: 200px;
        `}
      >
        <Loader size="large" />
        <span>Laster ressurser ...</span>
      </div>
    );
  }

  if (alleRessurser.data === undefined) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          padding-top: 200px;
        `}
      >
        <Alert variant="error">Noe gikk galt</Alert>
      </div>
    );
  }

  const rowsPerPage = 50;
  const filteredRessurser = alleRessurser.data.filter((ressurs) =>
    applyFilter(
      ressurs,
      ressursFilterValues,
      underliggendeOrgenheter.data ?? [],
    ),
  );

  const ressurserToDisplay = filteredRessurser.slice(
    (page - 1) * rowsPerPage,
    page * rowsPerPage,
  );

  return (
    <div
      css={css`
        padding: 2rem;
        background: white;
      `}
    >
      <RessursFilter />
      <b>Treff: {filteredRessurser.length}</b>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.ColumnHeader scope="col">Navident</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Visningsnavn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Sektor</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Org Tilknytning</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Ledere</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Sluttdato</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {ressurserToDisplay.map((ressurs, index) => (
            <Table.ExpandableRow
              content={<FullRessursInfo ressurs={ressurs} />}
              key={index}
            >
              <Table.HeaderCell scope="row">
                {ressurs.navident}
              </Table.HeaderCell>
              <Table.DataCell>{ressurs.visningsnavn}</Table.DataCell>
              <Table.DataCell>{ressurs.sektor}</Table.DataCell>
              <Table.DataCell>
                {ressurs.orgTilknytning.map(
                  (orgTilknytning) => orgTilknytning.orgEnhet.navn,
                )}
              </Table.DataCell>
              <Table.DataCell>
                {ressurs.ledere.map((leder) => leder.ressurs.navident)}
              </Table.DataCell>
              <Table.DataCell>{ressurs.sluttdato}</Table.DataCell>
            </Table.ExpandableRow>
          ))}
        </Table.Body>
      </Table>
      <Pagination
        count={Math.ceil(filteredRessurser.length / rowsPerPage)}
        css={css`
          padding: 2rem;
        `}
        hidden={filteredRessurser.length <= rowsPerPage}
        onPageChange={setPage}
        page={page}
      />
    </div>
  );
}

function RessursFilter() {
  return (
    <div>
      <OrgEnhetSearch />
    </div>
  );
}

function FullRessursInfo({ ressurs }: { ressurs: RessursFields }) {
  return (
    <JsonView
      data={ressurs}
      shouldExpandNode={allExpanded}
      style={defaultStyles}
    />
  );
}

function applyFilter(
  ressurs: RessursFields,
  filter: RessursFilterValues,
  underLiggendeOrgenheter: string[],
) {
  const orgEnhetMatch = filter.orgEnhet
    ? ressurs.orgTilknytning.some((orgTilknytning) => {
        const orgEnhetMatch = orgTilknytning.orgEnhet.id === filter.orgEnhet;
        const underLiggendeMatch = filter.includeUnderliggendeOrgEnheter
          ? underLiggendeOrgenheter.includes(orgTilknytning.orgEnhet.id)
          : false;
        return orgEnhetMatch || underLiggendeMatch;
      })
    : true;

  const underliggendeOrgEnhetMatch =
    Boolean(filter.includeUnderliggendeOrgEnheter) &&
    ressurs.orgTilknytning.some((orgTilknytning) =>
      underLiggendeOrgenheter.includes(orgTilknytning.orgEnhet.id),
    );

  return orgEnhetMatch || underliggendeOrgEnhetMatch;
}

export type RessursFilterValues = {
  orgEnhet?: string;
  includeUnderliggendeOrgEnheter?: boolean;
};
