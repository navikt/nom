import { css } from "@emotion/react";
import {
  Alert,
  Heading,
  HStack,
  Pagination,
  Table,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React, { useState } from "react";

import {
  getFysiskeBrukersteder,
  getOrgEnhetList,
  getOrgtilknytningerByNavidentList,
  getRessurserForFysiskBrukerstedAlle,
} from "~/api/react-query-endpoints";
import {
  useHasAdminAccess,
  useHasAdminReadOnlyAccess,
} from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { LoaderWithText } from "~/components/LoaderWithText";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { FysiskBrukerstedSelect } from "~/components/select/FysiskBrukerstedSelect";
import { useTableSort } from "~/hooks/useTableSort";
import { isPeriodActive } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { OrgEnhet } from "~/types/api-models";
import { formatVisningsnavn } from "~/utils";

export function FysiskBrukerstedTabellPage() {
  const hasAdminAccess = useHasAdminAccess();
  const hasAdminReadOnlyAccess = useHasAdminReadOnlyAccess();

  if (!hasAdminReadOnlyAccess && !hasAdminAccess) {
    return;
  }

  return (
    <>
      <PageBanner text={"Medarbeidere i tabell"} />

      <ContentWrapper limitWidth={false}>
        <FysiskBrukerstedTabell />
      </ContentWrapper>
    </>
  );
}

function FysiskBrukerstedTabell() {
  const fysiskeBrukerstederQuery = useQuery(getFysiskeBrukersteder);
  const fysiskeBrukersteder = fysiskeBrukerstederQuery.data || [];

  const [fysiskBrukerstedFilter, setFysiskBrukerstedFilter] = useState("");

  if (fysiskeBrukerstederQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  return (
    <VStack gap="6">
      <div
        css={css`
          width: 100%;
          max-width: 1008px;
          padding: 0 var(--a-spacing-6);
          margin: auto;
        `}
      >
        <HStack
          css={css`
            width: 100%;
            padding: var(--a-spacing-4) var(--a-spacing-6);
            border-radius: var(--a-border-radius-large);
            background-color: #deecef;
          `}
          gap="4"
        >
          <div
            css={css`
              flex: 1;
            `}
          >
            <FysiskBrukerstedSelect
              fysiskeBrukersteder={fysiskeBrukersteder}
              isClearable
              onChange={(value) => setFysiskBrukerstedFilter(value ?? "")}
              value={fysiskBrukerstedFilter}
            />
          </div>
          <div
            css={css`
              flex: 1;
            `}
          ></div>
        </HStack>
      </div>

      {!!fysiskBrukerstedFilter && (
        <FysiskBrukerstedTabellInner
          fysiskBrukerstedNr={fysiskBrukerstedFilter}
        />
      )}
    </VStack>
  );
}

function FysiskBrukerstedTabellInner({
  fysiskBrukerstedNr,
}: {
  fysiskBrukerstedNr: string;
}) {
  const fysiskBrukerstedRessurserAlleQuery = useQuery({
    queryKey: getRessurserForFysiskBrukerstedAlle.queryKey(fysiskBrukerstedNr),
    queryFn: () =>
      getRessurserForFysiskBrukerstedAlle.queryFn(fysiskBrukerstedNr),
    enabled: !!fysiskBrukerstedNr,
  });

  const fysiskBrukerstedRessurser = fysiskBrukerstedRessurserAlleQuery.data;

  const navidentListe =
    fysiskBrukerstedRessurser?.map((ressurs) => ressurs.navident) ?? [];

  const orgtilknytningerQuery = useQuery({
    queryKey: getOrgtilknytningerByNavidentList.queryKey(navidentListe),
    queryFn: () => getOrgtilknytningerByNavidentList.queryFn(navidentListe),
    enabled: navidentListe.length > 0,
  });

  const orgtilknytninger = orgtilknytningerQuery.data;

  const unikeOrgEnhetIder = [
    ...new Set(
      Object.values(orgtilknytninger ?? {}).flatMap((tilknytninger) =>
        tilknytninger.map((tilknytning) => tilknytning.orgEnhetId),
      ),
    ),
  ];

  const orgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(unikeOrgEnhetIder),
    queryFn: () => getOrgEnhetList.queryFn(unikeOrgEnhetIder),
  });

  const orgEnheter = orgEnheterQuery.data;

  const mappedeRessurser: FysiskBrukerstedTabellRow[] =
    fysiskBrukerstedRessurser?.map((ressurs) => {
      const tilknytningerForRessurs =
        orgtilknytninger?.[ressurs.navident] ?? [];

      const relevanteTilknytninger = tilknytningerForRessurs.filter(
        (tilknytning) =>
          tilknytning.primaryFysiskBrukerstedNummer === fysiskBrukerstedNr ||
          tilknytning.additionalFysiskeBrukerstederNummer?.includes(
            fysiskBrukerstedNr,
          ),
      );

      const relevanteOrgEnheter = relevanteTilknytninger
        .map((tilknytning) =>
          orgEnheter?.find(
            (orgEnhet) => orgEnhet.id === tilknytning.orgEnhetId,
          ),
        )
        .filter((orgEnhet) => orgEnhet !== undefined);

      const dolTilknytning = tilknytningerForRessurs.find(
        (tilknytning) =>
          isPeriodActive(tilknytning.gyldigFom, tilknytning.gyldigTom) &&
          tilknytning.erDagligOppfolging,
      );

      const dolTilgangsenhetId =
        orgEnheter?.find(
          (orgEnhet) => orgEnhet.id === dolTilknytning?.orgEnhetId,
        )?.tilgangsenhetId ?? "";

      return {
        navident: ressurs.navident,
        navn: formatVisningsnavn(ressurs.visningsnavn),
        orgEnheter: relevanteOrgEnheter,
        tilgangsenhetId: dolTilgangsenhetId,
      };
    }) ?? [];

  const { data, sort, handleSort } = useTableSort(mappedeRessurser, "navn");

  const sortedRessurser = data;

  const [page, setPage] = useState(1);
  const rowsPerPage = 300;
  const pages = Math.ceil(mappedeRessurser.length / rowsPerPage);

  const tableData = sortedRessurser.slice(
    rowsPerPage * (page - 1),
    rowsPerPage * page,
  );

  if (
    fysiskBrukerstedRessurserAlleQuery.isLoading ||
    orgtilknytningerQuery.isLoading ||
    orgEnheterQuery.isLoading
  ) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  if (
    fysiskBrukerstedRessurserAlleQuery.isError ||
    orgtilknytningerQuery.isError ||
    orgEnheterQuery.isError
  ) {
    return (
      <Alert variant="error">
        <Heading level="3" size="small">
          Det oppstod en feil ved henting av ansatte
        </Heading>
      </Alert>
    );
  }

  return (
    <>
      <Table
        css={css`
          background-color: #fff;
          padding: 6px;
          border-radius: 5px;

          --ac-table-row-border: #071a3636;
        `}
        onSortChange={handleSort}
        sort={sort}
        zebraStripes
      >
        <Table.Header
          css={css`
            border-bottom: 2px;
          `}
        >
          <Table.Row>
            <Table.ColumnHeader scope="col" sortKey="navident" sortable>
              Nav-ident
            </Table.ColumnHeader>

            <Table.ColumnHeader scope="col" sortKey="navn" sortable>
              Navn
            </Table.ColumnHeader>

            <Table.ColumnHeader
              scope="col"
              sortKey="orgEnheter[0].navn"
              sortable
            >
              Organisasjonsenhet
            </Table.ColumnHeader>

            <Table.ColumnHeader scope="col" sortKey="tilgangsenhetId" sortable>
              Tilgangsenhet
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {tableData?.map((rad) => (
            <Table.Row
              css={css`
                height: 48px;
                border-width: 0 0 1px;
              `}
              key={rad.navident}
              shadeOnHover={false}
            >
              <Table.DataCell>
                <SmartLink href={`/ressurs/${rad.navident}`}>
                  {rad.navident}
                </SmartLink>
              </Table.DataCell>
              <Table.DataCell>
                <SmartLink href={`/ressurs/${rad.navident}`}>
                  {rad.navn}
                </SmartLink>
              </Table.DataCell>
              <Table.DataCell>
                <VStack gap="1">
                  {rad.orgEnheter.map((orgEnhet) => (
                    <SmartLink
                      css={css`
                        width: fit-content;
                      `}
                      href={`/org/${orgEnhet.id}`}
                      key={orgEnhet.id}
                    >
                      {`${orgEnhet.navn} (${orgEnhet.id})`}
                    </SmartLink>
                  ))}
                </VStack>
              </Table.DataCell>
              <Table.DataCell>{rad.tilgangsenhetId}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>

      {pages > 1 && (
        <Pagination
          count={pages}
          css={css`
            margin: var(--a-spacing-6) auto 0;

            display: flex;
          `}
          onPageChange={setPage}
          page={page}
          prevNextTexts
        />
      )}
    </>
  );
}

type FysiskBrukerstedTabellRow = {
  navident: string;
  navn: string;
  orgEnheter: OrgEnhet[];
  tilgangsenhetId: string;
};
