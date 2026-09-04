import { css } from "@emotion/react";
import { FileExcelIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyShort,
  Button,
  Heading,
  HStack,
  Pagination,
  Table,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React, { useMemo, useState } from "react";

import {
  downloadExcelRessurserForFysiskBrukerstedAlle,
  getAlleTilgangsenheter,
  getFysiskeBrukersteder,
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
import {
  FysiskBrukerstedSelect,
  TilgangsenhetSelect,
} from "~/components/select/FysiskBrukerstedSelect";
import { useTableSort } from "~/hooks/useTableSort";
import type { RessurserByFysiskBrukerstedAndTilgangsenhetIdRequest } from "~/types/api-models";
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

  const fysiskeBrukersteder = fysiskeBrukerstederQuery.data ?? [];

  const alleTilgangsenheterQuery = useQuery({
    queryKey: getAlleTilgangsenheter.queryKey(),
    queryFn: () => getAlleTilgangsenheter.queryFn(),
  });

  const alleTilgangsenheter = alleTilgangsenheterQuery.data ?? [];

  const [fysiskBrukerstedFilter, setFysiskBrukerstedFilter] = useState("");
  const [tilgangsenhetFilter, setTilgangsenhetFilter] = useState("");

  const [isDownloading, setIsDownloading] = useState(false);

  const eksportFilnavn = () => {
    const valgtBrukersted = fysiskeBrukersteder.find(
      (brukersted) => brukersted.nr === fysiskBrukerstedFilter,
    );
    const valgtTilgangsenhet = alleTilgangsenheter.find(
      (tilgangsenhet) => tilgangsenhet.enhetId === tilgangsenhetFilter,
    );

    const deler: string[] = [];

    if (valgtBrukersted) {
      deler.push(`fysisk brukersted ${valgtBrukersted.navn}`);
    }

    if (valgtTilgangsenhet) {
      deler.push(
        `tilgangsenhet ${valgtTilgangsenhet.navn ?? tilgangsenhetFilter}`,
      );
    }

    return `Medarbeidere ${deler.join(" og ")}.xlsx`;
  };

  const handleDownloadExcel = async () => {
    try {
      setIsDownloading(true);
      await downloadExcelRessurserForFysiskBrukerstedAlle(
        {
          fysiskBrukerstedNr: fysiskBrukerstedFilter || undefined,
          tilgangsenhetId: tilgangsenhetFilter || undefined,
        },
        eksportFilnavn(),
      );
    } finally {
      setIsDownloading(false);
    }
  };

  if (fysiskeBrukerstederQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  return (
    <VStack gap="6">
      <HStack
        css={css`
          margin: auto;
          width: 100%;
          max-width: 1008px;
        `}
        align="end"
        gap="4"
      >
        <VStack
          css={css`
            flex: 1;
            padding: var(--a-spacing-4) var(--a-spacing-6);
            border-radius: var(--a-border-radius-large);
            background-color: #deecef;
          `}
          gap="4"
        >
          <HStack gap="4">
            <div
              css={css`
                flex: 1;
              `}
            >
              <FysiskBrukerstedSelect
                fysiskeBrukersteder={fysiskeBrukersteder}
                isClearable
                label="Primært fysisk brukersted"
                onChange={(value) => setFysiskBrukerstedFilter(value ?? "")}
                value={fysiskBrukerstedFilter}
              />
            </div>

            <div
              css={css`
                flex: 1;
              `}
            >
              <TilgangsenhetSelect
                isClearable
                label="Primær tilgangsenhet"
                onChange={(value) => setTilgangsenhetFilter(value ?? "")}
                tilgangsenheter={alleTilgangsenheter}
                value={tilgangsenhetFilter}
              />
            </div>
          </HStack>

          <Alert variant="info">
            Filtrene baserer seg på brukeres organisasjonstilknytninger med
            daglig oppfølging.
          </Alert>
        </VStack>

        <Button
          disabled={!fysiskBrukerstedFilter && !tilgangsenhetFilter}
          icon={<FileExcelIcon aria-hidden />}
          loading={isDownloading}
          onClick={handleDownloadExcel}
          variant="secondary"
        >
          Eksportér til Excel
        </Button>
      </HStack>

      {(!!fysiskBrukerstedFilter || tilgangsenhetFilter) && (
        <FysiskBrukerstedTabellInner
          fysiskBrukerstedNr={fysiskBrukerstedFilter}
          tilgangsenhet={tilgangsenhetFilter}
        />
      )}
    </VStack>
  );
}

function FysiskBrukerstedTabellInner({
  fysiskBrukerstedNr,
  tilgangsenhet,
}: {
  fysiskBrukerstedNr: string;
  tilgangsenhet: string;
}) {
  const request: RessurserByFysiskBrukerstedAndTilgangsenhetIdRequest = {
    fysiskBrukerstedNr: fysiskBrukerstedNr || undefined,
    tilgangsenhetId: tilgangsenhet || undefined,
  };

  const fysiskBrukerstedRessurserAlleQuery = useQuery({
    queryKey: getRessurserForFysiskBrukerstedAlle.queryKey(request),
    queryFn: () => getRessurserForFysiskBrukerstedAlle.queryFn(request),
    enabled: !!fysiskBrukerstedNr || !!tilgangsenhet,
  });

  const fysiskBrukerstedRessurser = fysiskBrukerstedRessurserAlleQuery.data;

  const ressursRows: FysiskBrukerstedTabellRow[] = useMemo(() => {
    const radPerNavident = new Map<string, FysiskBrukerstedTabellRow>();

    for (const ressurs of fysiskBrukerstedRessurser ?? []) {
      let rad = radPerNavident.get(ressurs.navident);

      if (!rad) {
        rad = {
          navident: ressurs.navident,
          navn: formatVisningsnavn(ressurs.visningsnavn),
          orgEnheter: [],
          fysiskeBrukersteder: [],
          tilgangsenhetId: ressurs.tilgangsenhetId,
        };
        radPerNavident.set(ressurs.navident, rad);
      }

      if (
        ressurs.orgEnhetId &&
        !rad.orgEnheter.some((orgEnhet) => orgEnhet.id === ressurs.orgEnhetId)
      ) {
        rad.orgEnheter.push({
          id: ressurs.orgEnhetId,
          navn: ressurs.orgEnhetNavn,
        });
      }

      if (
        ressurs.fysiskBrukerstedNummer &&
        !rad.fysiskeBrukersteder.some(
          (brukersted) => brukersted.nummer === ressurs.fysiskBrukerstedNummer,
        )
      ) {
        rad.fysiskeBrukersteder.push({
          nummer: ressurs.fysiskBrukerstedNummer,
          navn: ressurs.fysiskBrukerstedNavn,
        });
      }
    }

    return [...radPerNavident.values()];
  }, [fysiskBrukerstedRessurser]);

  const { data, sort, handleSort } = useTableSort(ressursRows, "navn");

  const sortedRessurser = data;

  const [page, setPage] = useState(1);
  const rowsPerPage = 300;
  const pages = Math.ceil(ressursRows.length / rowsPerPage);

  const tableData = sortedRessurser.slice(
    rowsPerPage * (page - 1),
    rowsPerPage * page,
  );

  if (fysiskBrukerstedRessurserAlleQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  if (fysiskBrukerstedRessurserAlleQuery.isError) {
    return (
      <Alert variant="error">
        <Heading level="3" size="small">
          Det oppstod en feil ved henting av ansatte
        </Heading>
      </Alert>
    );
  }

  return (
    <VStack gap="2">
      <BodyShort>
        Antall medarbeidere: <b>{ressursRows.length}</b>
      </BodyShort>

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

            <Table.ColumnHeader
              scope="col"
              sortKey="fysiskeBrukersteder[0].navn"
              sortable
            >
              Fysisk brukersted
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
              <Table.DataCell>
                <VStack gap="1">
                  {rad.fysiskeBrukersteder.map((brukersted) => (
                    <BodyShort key={brukersted.nummer}>
                      {`${brukersted.navn} (${brukersted.nummer})`}
                    </BodyShort>
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
    </VStack>
  );
}

type FysiskBrukerstedTabellRow = {
  navident: string;
  navn: string;
  orgEnheter: { id: string; navn: string }[];
  fysiskeBrukersteder: { nummer: string; navn: string }[];
  tilgangsenhetId: string;
};
