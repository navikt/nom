import { css } from "@emotion/react";
import { Alert, Loader, Pagination, Table } from "@navikt/ds-react";
import type { AxiosResponse } from "axios";
import axios from "axios";
import { useState } from "react";
import { useQuery } from "react-query";

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

  const ressurserToDisplay = alleRessurser.data.slice(
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
      <b>Treff: {alleRessurser.data.length}</b>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navident</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Visningsnavn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Sektor</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">OrgEnhet</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Ledere</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Sluttdato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Startdato</Table.ColumnHeader>
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
              <Table.DataCell>{ressurs.sluttdato}</Table.DataCell>
              <Table.DataCell>{ressurs.startdato}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
      <Pagination
        count={Math.ceil(alleRessurser.data.length / rowsPerPage)}
        css={css`
          padding: 2rem;
        `}
        hidden={alleRessurser.data.length <= rowsPerPage}
        onPageChange={setPage}
        page={page}
      />
    </div>
  );
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
