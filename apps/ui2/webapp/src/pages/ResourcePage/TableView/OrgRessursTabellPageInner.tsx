import { css } from "@emotion/react";
import {
  Alert,
  Heading,
  Link,
  Loader,
  Pagination,
  type SortState,
  Table,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import uniqBy from "lodash/uniqBy";
import React, { useState } from "react";

import {
  getRessurserByListNavident,
  harLederTilgangForOrgenhetOptions,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { useTableSort } from "~/hooks/useTableSort";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import type {
  OrgEnhet,
  Ressurs,
  Sektor,
  Telefon,
  Visningsnavn,
} from "~/types/api-models";
import {
  dateWithoutTimezone,
  earliestDateInList,
  formatDisplayDate,
  formatTlf,
  formatVisningsnavn,
  isPeriodActiveOrFuture,
} from "~/utils";

export const OrgRessursTabellPageInner = ({
  orgEnheter = [],
  ressurserNavidents,
  toppOrgEnhetId,
}: {
  orgEnheter: OrgEnhet[];
  ressurserNavidents: string[];
  toppOrgEnhetId: string;
}) => {
  const hentedeRessurserQuery = useQuery({
    queryKey: getRessurserByListNavident.queryKey(ressurserNavidents),
    queryFn: () => getRessurserByListNavident.queryFn(ressurserNavidents),
    enabled: ressurserNavidents.length > 0,
  });

  const [page, setPage] = useState(1);
  const rowsPerPage = 300;
  const ressursData = hentedeRessurserQuery.data;
  const isLeader =
    useQuery(harLederTilgangForOrgenhetOptions(toppOrgEnhetId)).data || false;
  const isLeaderOrHasAdmin = useHasAdminAccess() || isLeader;
  const customRessurser = createCustomRessurser(
    orgEnheter,
    ressursData as Ressurs[],
  );

  const { data, sort, handleSort } = useTableSort(
    customRessurser as CustomRessurs[],
    "visningsnavn.fornavn",
  );

  if (hentedeRessurserQuery.isLoading) {
    return (
      <div
        css={css`
          display: flex;
          align-items: center;
          justify-content: center;
        `}
      >
        <Loader size="3xlarge" variant="neutral" />
      </div>
    );
  }

  if (hentedeRessurserQuery.isError) {
    return (
      <Alert variant="error">
        <Heading level="3" size="small">
          Det oppstod en feil ved henting av ansatte
        </Heading>
      </Alert>
    );
  }

  if (ressursData) {
    if (!data) {
      return <></>;
    }

    let sortData = customSortByOrgEnhetName(data, sort);
    sortData = sortData.slice((page - 1) * rowsPerPage, page * rowsPerPage);

    return (
      <div
        css={css`
          margin-top: 20px;
        `}
      >
        <Table
          css={css`
            background-color: #fff;
            padding: 6px;
            border-radius: 5px;

            --ac-table-row-border: #071a3636;
          `}
          onSortChange={(sortKey) => handleSort(sortKey)}
          size="small"
          sort={sort}
        >
          <Table.Header
            css={css`
              height: 48px;
              border-bottom: 2px;
            `}
          >
            <Table.Row
              css={css`
                height: 48px;
                border-width: 0 0 1px;
              `}
            >
              <Table.ColumnHeader scope="col" sortKey="navident" sortable>
                Nav-ident
              </Table.ColumnHeader>
              <Table.ColumnHeader
                scope="col"
                sortKey="visningsnavn.fornavn"
                sortable
              >
                Navn
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortKey="sektor" sortable>
                Sektor
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortKey="orgEnhet.name" sortable>
                Organisasjonsenhet
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortKey="tidligsteFom" sortable>
                Fra og med
              </Table.ColumnHeader>
              {isLeaderOrHasAdmin && (
                <Table.ColumnHeader scope="col" sortKey="sluttdato" sortable>
                  Sluttdato
                </Table.ColumnHeader>
              )}
              <Table.ColumnHeader scope="col" sortKey="telefonnumer">
                Telefonnr.
              </Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body
            css={css`
              height: 48px;
            `}
          >
            {sortData.map((ressurs) => (
              <Table.Row
                css={css`
                  --ac-table-row-border: none;
                  height: 48px;
                  border-width: 0 0 1px;
                `}
                key={ressurs.navident}
                shadeOnHover={false}
              >
                <Table.DataCell>
                  <Link href={`/ressurs/${ressurs.navident}`}>
                    {ressurs.navident}
                  </Link>
                </Table.DataCell>
                <Table.DataCell>
                  <Link href={`/ressurs/${ressurs.navident}`}>
                    {formatVisningsnavn(ressurs.visningsnavn)}
                  </Link>
                </Table.DataCell>
                <Table.DataCell>
                  {SektorNameMapping[ressurs.sektor]}
                </Table.DataCell>
                <Table.DataCell>
                  {ressurs.orgEnheter.map((orgEnhet, index, array) => (
                    <React.Fragment key={orgEnhet.id}>
                      <Link href={`/org/${orgEnhet.id}`}>
                        {orgEnhet.name + " (" + orgEnhet.id + ")"}
                      </Link>
                      {index < array.length - 1 ? ", " : ""}
                    </React.Fragment>
                  ))}
                </Table.DataCell>
                <Table.DataCell>
                  {ressurs.tidligsteFom
                    ? formatDisplayDate(
                        dateWithoutTimezone(ressurs.tidligsteFom),
                      )
                    : undefined}
                </Table.DataCell>
                {isLeaderOrHasAdmin && (
                  <Table.DataCell>
                    {ressurs.sluttdato
                      ? formatDisplayDate(
                          dateWithoutTimezone(ressurs.sluttdato),
                        )
                      : undefined}
                  </Table.DataCell>
                )}
                <Table.DataCell
                  css={css`
                    min-width: 110px;
                  `}
                >
                  <TelefonNummere ressurs={ressurs} />
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
        <Pagination
          count={Math.ceil(data.length / rowsPerPage)}
          css={css`
            margin-top: 24px;
            display: flex;
            align-items: center;
            justify-content: center;
          `}
          onPageChange={setPage}
          page={page}
          prevNextTexts
          size="medium"
        />
      </div>
    );
  }
};

interface CustomOrgEnhet {
  id: string;
  name: string;
}

interface CustomRessurs {
  navident: string;
  visningsnavn: Visningsnavn;
  primaryTelefonnummer?: string;
  telefoner: Telefon[];
  sektor: Sektor;
  orgEnheter: CustomOrgEnhet[];
  tidligsteFom?: Date | string;
  sluttdato?: Date | string;
}

const createCustomRessurser = (
  orgEnheter: OrgEnhet[],
  hentedeRessurser: Ressurs[],
): CustomRessurs[] | undefined => {
  return hentedeRessurser?.map((ressurs) => {
    const correspondingOrgEnheter = orgEnheter.filter((orgEnhet) =>
      orgEnhet.ressursKoblinger.some(
        (kobling) =>
          kobling.navident === ressurs.navident &&
          isPeriodActiveOrFuture(kobling.gyldigFom, kobling.gyldigTom),
      ),
    );

    const fomDatoer = correspondingOrgEnheter.map(
      (orgEnhet) =>
        orgEnhet.ressursKoblinger.find(
          (kobling) => kobling.navident === ressurs.navident,
        )?.gyldigFom,
    );

    const tidligsteFomDato = earliestDateInList(fomDatoer);

    const customOrgEnheter: CustomOrgEnhet[] = correspondingOrgEnheter.map(
      (orgEnhet) => ({
        id: orgEnhet.id,
        name: orgEnhet.navn,
      }),
    );

    const customRessurs: CustomRessurs = {
      navident: ressurs.navident,
      visningsnavn: ressurs.visningsnavn,
      primaryTelefonnummer: ressurs.primaryTelefonnummer,
      sektor: ressurs.sektor,
      telefoner: ressurs.telefoner,
      orgEnheter: customOrgEnheter,
      tidligsteFom: tidligsteFomDato,
      sluttdato: ressurs.sluttdato === null ? undefined : ressurs.sluttdato,
    };

    return customRessurs;
  });
};

export function TelefonNummere({ ressurs }: { ressurs: CustomRessurs }) {
  const telefonnummere = ressurs.primaryTelefonnummer
    ? [{ nummer: ressurs.primaryTelefonnummer }, ...ressurs.telefoner]
    : ressurs.telefoner;

  const uniqueTelefonnummere = uniqBy(telefonnummere, "nummer");

  return uniqueTelefonnummere.map(({ nummer }) => formatTlf(nummer)).join(", ");
}

function customSortByOrgEnhetName(
  customRessurser: CustomRessurs[],
  sortState: SortState,
): CustomRessurs[] {
  if (sortState.orderBy !== "orgEnhet.name") {
    return customRessurser;
  }
  const sortedData = [...customRessurser];
  sortedData.sort((a, b) => {
    const nameA = a.orgEnheter[0]?.name.toLowerCase() || "";
    const nameB = b.orgEnheter[0]?.name.toLowerCase() || "";

    return sortState.direction === "ascending"
      ? nameA.localeCompare(nameB)
      : nameB.localeCompare(nameA);
  });

  return sortedData;
}
