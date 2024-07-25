import { css } from "@emotion/react";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import type { SortState } from "@navikt/ds-react";
import {
  Alert,
  Checkbox,
  Heading,
  Link,
  Loader,
  Pagination,
  Table,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import React, { useState } from "react";

import {
  getAlleUnderliggendeOrgEnheter,
  getOrgEnhet,
  getRessurserByListNavident,
} from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { PageBanner } from "~/components/PageBanner";
import { useTableSort } from "~/hooks/useTableSort";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import type {
  OrgEnhet,
  Ressurs,
  Sektor,
  Telefon,
  Visningsnavn,
} from "~/types/api-models";
import { formatVisningsnavn, isGyldighetsPeriodeActive } from "~/utils";

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

const createCustomRessurser = (
  orgEnheter: OrgEnhet[],
  hentedeRessurser: Ressurs[],
): CustomRessurs[] => {
  return hentedeRessurser.map((ressurs) => {
    const correspondingOrgEnheter = orgEnheter.filter((orgEnhet) =>
      orgEnhet.ressursKoblinger.some(
        (kobling) =>
          kobling.navident === ressurs.navident &&
          isGyldighetsPeriodeActive(kobling.gyldigFom, kobling.gyldigTom),
      ),
    );

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
    };

    return customRessurs;
  });
};

function OrgRessursTabellPageInner({
  orgEnheter = [],
}: {
  orgEnheter: OrgEnhet[];
}) {
  const allRessursKoblinger = orgEnheter.flatMap((orgEnhet) =>
    orgEnhet.ressursKoblinger.filter((kobling) =>
      isGyldighetsPeriodeActive(kobling.gyldigFom, kobling.gyldigTom),
    ),
  );

  const ressurserNavidents = [
    ...new Set(allRessursKoblinger.map((tilknytning) => tilknytning.navident)),
  ];

  const hentedeRessurserQuery = useQuery({
    queryKey: getRessurserByListNavident.queryKey(ressurserNavidents),
    queryFn: () => getRessurserByListNavident.queryFn(ressurserNavidents),
    enabled: ressurserNavidents.length > 0,
  });

  if (hentedeRessurserQuery.isLoading) {
    return (
      <div
        css={css`
          justify-self: center;
        `}
      >
        <Loader size="3xlarge" variant="neutral" />
      </div>
    );
  } else if (hentedeRessurserQuery.isError) {
    return (
      <Alert variant="error">
        <Heading level="3" size="small">
          Det oppstod en feil ved henting av ansatte
        </Heading>
      </Alert>
    );
  }

  const customRessurser = createCustomRessurser(
    orgEnheter,
    hentedeRessurserQuery.data as Ressurs[],
  );

  const { data, sort, handleSort } = useTableSort(
    customRessurser,
    "visningsnavn.fornavn",
  );
  const finalSortedData = customSortByOrgEnhetName(data, sort);

  const [page, setPage] = useState(1);
  const rowsPerPage = 50;

  let sortData = finalSortedData;
  sortData = sortData.slice((page - 1) * rowsPerPage, page * rowsPerPage);

  if (hentedeRessurserQuery.data?.length === 0) {
    return <></>;
  }

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
              NAV-ident
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
                //TODO: (Test Testesen 4) Lage egen util funksjon for dette
                {ressurs.orgEnheter.map((orgEnhet, index, array) => (
                  <React.Fragment key={orgEnhet.id}>
                    <Link href={`/org/${orgEnhet.id}`}>{orgEnhet.name}</Link>
                    {index < array.length - 1 ? ", " : ""}
                  </React.Fragment>
                ))}
              </Table.DataCell>
              <Table.DataCell>
                {ressurs.primaryTelefonnummer || ressurs.telefoner[0]?.nummer}
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

export const OrgRessursTabellPage = () => {
  const [includeUnderliggende, setIncludeUnderliggende] = useState(true);
  const { id } = useParams({ from: "/org/table/$id" });

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id as string),
    queryFn: () => getOrgEnhet.queryFn(id as string),
    enabled: !!id,
  });

  const underliggendeOrgEnhetQuery = useQuery({
    queryKey: getAlleUnderliggendeOrgEnheter.queryKey(id as string),
    queryFn: () => getAlleUnderliggendeOrgEnheter.queryFn(id as string),
    enabled: includeUnderliggende && !!id,
  });

  let innerContent;
  let kombinertOrgEnheter: OrgEnhet[] = [];

  if (includeUnderliggende && underliggendeOrgEnhetQuery.data) {
    kombinertOrgEnheter =
      ([
        orgEnhetQuery.data,
        ...underliggendeOrgEnhetQuery.data,
      ] as OrgEnhet[]) || [];
  } else if (orgEnhetQuery.data) {
    kombinertOrgEnheter = [orgEnhetQuery.data];
  }

  if (orgEnhetQuery.data) {
    innerContent = (
      <OrgRessursTabellPageInner orgEnheter={kombinertOrgEnheter} />
    );
  } else {
    innerContent = orgEnhetQuery.isLoading ? (
      <div
        css={css`
          justify-self: center;
        `}
      >
        <Loader size="3xlarge" variant="neutral" />
      </div>
    ) : (
      <Alert variant="error">
        <Heading level="3" size="small">
          Kunne ikke hente organisajoner
        </Heading>
      </Alert>
    );
  }

  return (
    <>
      <PageBanner text={"Medarbeidere i " + orgEnhetQuery.data?.navn || ""} />
      <ContentWrapper>
        <Link
          css={css`
            color: #000;
            font-size: 1.2rem;
            margin-bottom: 20px;
          `}
          href={`/org/${id}`}
        >
          <ChevronLeftIcon fontSize="1.8rem" />
          <b>Tilbake til {orgEnhetQuery.data?.navn}</b>
        </Link>
        <Checkbox
          checked={includeUnderliggende}
          css={css`
            color: #000;
            font-size: 1.2rem;
            margin-bottom: 20px;
          `}
          onChange={() => {
            setIncludeUnderliggende(!includeUnderliggende);
          }}
          value="underliggende-organisasjonsenheter"
        >
          Vis medarbeidere fra underliggende organisasjonsenheter
        </Checkbox>
        //TODO: (Test Testesen 4) Burde splitte dette opp i flere komponenter
        {innerContent}
      </ContentWrapper>
    </>
  );
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
}
