import { css } from "@emotion/react";
import { Alert, Heading, Pagination, Table, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React, { useState } from "react";

import {
  getFysiskeBrukersteder,
  getOrgEnhetList,
  getOrgtilknytningerByNavidentList,
  getRessurserForFysiskBrukersted,
} from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { LoaderWithText } from "~/components/LoaderWithText";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { FysiskBrukerstedSelect } from "~/components/select/FysiskBrukerstedSelect";
import { useTableSort } from "~/hooks/useTableSort";
import {
  isPeriodActive,
  nomDateIsTodayOrAfter,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { OrgEnhet } from "~/types/api-models";
import { formatVisningsnavn } from "~/utils";

export function FysiskBrukerstedTabellPage() {
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
        <VStack
          css={css`
            background-color: #deecef;
            padding: var(--a-spacing-4) var(--a-spacing-6);
            border-radius: var(--a-border-radius-large);
            width: 100%;
          `}
          gap="4"
        >
          <div
            css={css`
              width: 50%;
            `}
          >
            <FysiskBrukerstedSelect
              fysiskeBrukersteder={fysiskeBrukersteder}
              isClearable
              onChange={(value) => setFysiskBrukerstedFilter(value ?? "")}
              value={fysiskBrukerstedFilter}
            />
          </div>
        </VStack>
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
  const getFysiskBrukerstedRessurserQuery = useQuery({
    queryKey: getRessurserForFysiskBrukersted.queryKey(fysiskBrukerstedNr),
    queryFn: () => getRessurserForFysiskBrukersted.queryFn(fysiskBrukerstedNr),
    enabled: !!fysiskBrukerstedNr,
  });

  const fysiskBrukerstedRessurser = getFysiskBrukerstedRessurserQuery.data;

  const aktiveRessurser = fysiskBrukerstedRessurser?.filter((ressurs) =>
    nomDateIsTodayOrAfter(ressurs.sluttdato),
  );

  const navidentListe =
    aktiveRessurser?.map((ressurs) => ressurs.navident) ?? [];

  const orgtilknytningerQuery = useQuery({
    queryKey: getOrgtilknytningerByNavidentList.queryKey(navidentListe),
    queryFn: () => getOrgtilknytningerByNavidentList.queryFn(navidentListe),
    enabled: navidentListe.length > 0,
  });

  const orgtilknytninger = orgtilknytningerQuery.data;

  const unikeOrgEnhetIder = [
    ...new Set(
      Object.values(orgtilknytninger ?? {}).flatMap((tilknytninger) =>
        tilknytninger.map((t) => t.orgEnhetId),
      ),
    ),
  ];

  const orgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(unikeOrgEnhetIder),
    queryFn: () => getOrgEnhetList.queryFn(unikeOrgEnhetIder),
  });

  const orgEnheter = orgEnheterQuery.data;

  const mappedeRessurser: FysiskBrukerstedTabellRow[] =
    aktiveRessurser?.map((ressurs) => {
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

      const dolFiresiffer =
        orgEnheter?.find(
          (orgEnhet) => orgEnhet.id === dolTilknytning?.orgEnhetId,
        )?.remedyEnhetId ?? "";

      return {
        navident: ressurs.navident,
        navn: formatVisningsnavn(ressurs.visningsnavn),
        orgEnheter: relevanteOrgEnheter,
        firesiffer: dolFiresiffer,
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
    getFysiskBrukerstedRessurserQuery.isLoading ||
    orgtilknytningerQuery.isLoading ||
    orgEnheterQuery.isLoading
  ) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  if (
    getFysiskBrukerstedRessurserQuery.isError ||
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
      {pages > 1 && (
        <Pagination
          count={pages}
          css={css`
            margin-top: var(--a-spacing-6);
            display: flex;
          `}
          onPageChange={setPage}
          page={page}
          prevNextTexts
        />
      )}

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
            height: 48px;
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

            <Table.ColumnHeader scope="col" sortKey="firesiffer" sortable>
              Firesiffer
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {tableData?.map((rad) => (
            <Table.Row
              css={css`
                --ac-table-row-border: none;
                height: 48px;
                border-width: 0 0 1px;
              `}
              key={rad.navident}
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
                    <SmartLink href={`/org/${orgEnhet.id}`} key={orgEnhet.id}>
                      {`${orgEnhet.navn} (${orgEnhet.id})`}
                    </SmartLink>
                  ))}
                </VStack>
              </Table.DataCell>
              <Table.DataCell>{rad.firesiffer}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>

      {pages > 1 && (
        <Pagination
          count={pages}
          css={css`
            margin-top: var(--a-spacing-6);
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
  firesiffer: string;
};
