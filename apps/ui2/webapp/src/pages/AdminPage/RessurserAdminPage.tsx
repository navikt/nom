import { css } from "@emotion/react";
import type { SortState } from "@navikt/ds-react";
import {
  Alert,
  Loader,
  Pagination,
  Table,
  TextField,
  UNSAFE_Combobox,
} from "@navikt/ds-react";
import type { AxiosResponse } from "axios";
import axios from "axios";
import { useEffect, useState } from "react";
import { useQuery } from "react-query";

type AdminFilter = {
  navn?: string;
  aktiv?: boolean;
};

export function RessurserAdminPage() {
  // Using react query instead of apollo because it runs more smoothly
  const alleRessurser = useQuery<
    AxiosResponse<GraphqlResponseWrapper>,
    unknown,
    RessursFields[]
  >({
    queryKey: "AlleRessurser",
    queryFn: () =>
      axios.post("/nom-api/graphql", {
        operationName: "HentAlleRessurser",
        query: hentAlleRessurserQuery,
        variables: {},
      }),
    select: (data) => {
      return data.data.data.ressurser.map((ressurs) => ressurs.ressurs);
    },
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
        <UNSAFE_Combobox
          defaultValue={"Alle"}
          label="Aktiv"
          onToggleSelected={(option, isSelected) => {
            setFilter((filter) => ({
              ...filter,
              aktiv: isSelected ? activeStringToAdminValue(option) : undefined,
            }));
          }}
          options={["Aktive", "Alle", "Inaktive"]}
          shouldAutocomplete={true}
        />
        <TextField
          label="Navn"
          onChange={(event) =>
            setFilter((filter) => ({ ...filter, navn: event.target.value }))
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
    const nameMatch =
      !adminFilter.navn || searchName(adminFilter.navn, ressurs.visningsnavn);
    if (adminFilter.aktiv === undefined) {
      return nameMatch;
    }
    const isAktiv =
      !ressurs.sluttdato || new Date() < new Date(ressurs.sluttdato);
    return nameMatch && adminFilter.aktiv === isAktiv;
  });
}

const hentAlleRessurserQuery = `
query HentAlleRessurser {
    ressurser(where: {}) {
        ressurs {
            navident,
            orgTilknytning{
                orgEnhet {
                    id
                }
            }
            ledere {
                ressurs {
                    navident
                }
            }
            visningsnavn
            sektor
            sluttdato
            startdato
        }
    }
}
`;

type GraphqlResponseWrapper = {
  data: {
    ressurser: {
      ressurs: RessursFields;
    }[];
  };
};

type RessursFields = {
  navident: string;
  orgTilknytning: {
    orgEnhet: {
      id: string;
    };
  }[];
  ledere: {
    ressurs: {
      navident: string;
    };
  }[];
  visningsnavn: string;
  sektor: string[];
  sluttdato: string;
  startdato: string;
};

function activeStringToAdminValue(option: string) {
  if ("Aktive" === option) return true;
  if ("Inaktive" === option) return false;
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
