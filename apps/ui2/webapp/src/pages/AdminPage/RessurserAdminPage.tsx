import { css } from "@emotion/react";
import type { SortState } from "@navikt/ds-react";
import {
  Alert,
  Loader,
  Pagination,
  Select,
  Table,
  TextField,
} from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { useQuery } from "react-query";

import type { RessursFields } from "~/api/react-query-endpoints";
import { getAlleRessurser } from "~/api/react-query-endpoints";

type AdminFilter = {
  navn?: string;
  aktiv?: boolean;
  navident?: string;
};

export function RessurserAdminPage() {
  // Using react query instead of apollo because it runs more smoothly
  const alleRessurser = useQuery<RessursFields[], unknown>({
    ...getAlleRessurser,
  });

  const [filter, setFilter] = useState<AdminFilter>({});
  const [sort, setSort] = useState<SortState | undefined>({
    orderBy: "id",
    direction: "ascending",
  });
  const [page, setPage] = useState(1);
  useEffect(() => {
    setPage(1);
  }, [filter]);

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
      // eslint-disable-next-line unicorn/no-useless-undefined
      setSort(undefined);
    }
  };

  const filteredRessurser = filterRessurser(alleRessurser.data, filter);

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
      <div
        css={css`
          display: grid;
          gap: 1rem;
          grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        `}
      >
        <Select
          defaultValue={"Alle"}
          label="Aktiv"
          onChange={(changeEvent) =>
            setFilter({
              ...filter,
              aktiv: activeStringToAdminValue(changeEvent.target.value),
            })
          }
        >
          <option value={undefined}>Alle</option>
          <option value={"aktive"}>Aktive</option>
          <option value={"inaktive"}>Inaktive</option>
        </Select>
        <TextField
          label="Navn"
          onChange={(event) =>
            setFilter((filter) => ({ ...filter, navn: event.target.value }))
          }
        />
        <TextField
          label="NavIdent"
          onChange={(event) =>
            setFilter((filter) => ({ ...filter, navident: event.target.value }))
          }
        />
      </div>
      <b>Treff: {filteredRessurser.length}</b>
      <Table onSortChange={(sortKey) => handleSort(sortKey)} sort={sort}>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navident</Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortKey={"navn"} sortable>
              Visningsnavn
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Sektor</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">OrgEnhet</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Ledere</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Startdato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Sluttdato</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {ressurserToDisplay.map((ressurs, index) => (
            <Table.Row key={index}>
              <Table.HeaderCell scope="row">
                {ressurs.navident}
              </Table.HeaderCell>
              <Table.DataCell>{ressurs.visningsnavn}</Table.DataCell>
              <Table.DataCell>{ressurs.sektor}</Table.DataCell>
              <Table.DataCell>
                {ressurs.orgTilknytning.map(
                  (orgTilknytning) => orgTilknytning.orgEnhet.id,
                )}
              </Table.DataCell>
              <Table.DataCell>
                {ressurs.ledere.map((leder) => leder.ressurs.navident)}
              </Table.DataCell>
              <Table.DataCell>{ressurs.startdato}</Table.DataCell>
              <Table.DataCell>{ressurs.sluttdato}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
      <Pagination
        count={Math.max(1, Math.ceil(filteredRessurser.length / rowsPerPage))}
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

function filterRessurser(ressurser: RessursFields[], adminFilter: AdminFilter) {
  return ressurser.filter((ressurs) => {
    const navidentmatch =
      !adminFilter.navident ||
      ressurs.navident === adminFilter.navident.toUpperCase();

    const nameMatch =
      !adminFilter.navn || searchName(adminFilter.navn, ressurs.visningsnavn);

    if (adminFilter.aktiv === undefined) {
      return nameMatch && navidentmatch;
    }

    const isAktiv =
      !ressurs.sluttdato || new Date() < new Date(ressurs.sluttdato);

    return nameMatch && navidentmatch && adminFilter.aktiv === isAktiv;
  });
}

function activeStringToAdminValue(option: string) {
  if ("aktive" === option) return true;
  if ("inaktive" === option) return false;
  return;
}

function searchName(search: string, target: string) {
  search = search.toLowerCase();
  target = target.toLowerCase();
  let accept = true;
  const prefixTerms = search.split(/ +/);
  for (const p of prefixTerms) {
    accept = accept && target.includes(p);
  }
  return accept;
}
