import { css } from "@emotion/react";
import { ChevronLeftIcon, FileExcelIcon } from "@navikt/aksel-icons";
import { Alert, Button, Heading, Loader, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import React, { useState } from "react";

import {
  downloadExcelKontaktpersonerForOrgenhetByOrgenhetId,
  getKonaktpersonerForOrgEnhet,
  getOrgEnhet,
  harLederTilgangForOrgenhetOptions,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { isEksternSamarbeidspartnerOrgEnhet } from "~/customUtils/environmentUtils";
import type { Relasjon } from "~/pages/Parorendeinfo/ParorendeinfoPage";
import { RelasjonNameMapping } from "~/pages/Parorendeinfo/ParorendeinfoPage";
import type { OrgEnhet } from "~/types/api-models";
import { formatTlf } from "~/utils";

export function KontaktpersonerTabellPage() {
  const { id } = useParams({ from: "/kontaktpersoner-tabell/$id" });

  const hasAdminAccess = useHasAdminAccess();
  const hasWriteAccess = useHasWriteAccess();

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id),
    queryFn: () => getOrgEnhet.queryFn(id),
  });

  const orgEnhet = orgEnhetQuery.data;

  const [isDownloading, setIsDownloading] = useState(false);

  const excelFilNavn = () => {
    return "kontaktpersoner for orgenhet - " + orgEnhet?.navn + ".xlsx";
  };

  const handleDownloadKontaktpersonerExcel = async () => {
    try {
      setIsDownloading(true);
      await downloadExcelKontaktpersonerForOrgenhetByOrgenhetId(
        orgEnhet?.id ?? "",
        excelFilNavn(),
      );
    } finally {
      setIsDownloading(false);
    }
  };

  const hasWriteAccessForOrgenhet =
    useQuery(harLederTilgangForOrgenhetOptions(orgEnhet?.id ?? "")).data ??
    false;

  const eksternSamarbeidspartner = isEksternSamarbeidspartnerOrgEnhet(
    orgEnhet?.id ?? "",
  );

  const adminOrWriteForOrgenhet =
    (hasWriteAccess && hasWriteAccessForOrgenhet) || hasAdminAccess;

  if (!adminOrWriteForOrgenhet || eksternSamarbeidspartner) {
    return;
  }

  if (orgEnhetQuery.isLoading) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
        `}
      >
        <Loader size="3xlarge" />
        <span>Laster...</span>
      </div>
    );
  }

  if (orgEnhetQuery.isError || !orgEnhet) {
    return (
      <Alert variant="error">
        <Heading level="3" size="small">
          Kunne ikke hente organisasjonsenhet med ID <b>{id}</b>
        </Heading>
      </Alert>
    );
  }

  return (
    <>
      <PageBanner
        text={`Kontaktpersoner for medarbeidere under ${orgEnhet?.navn ?? id}`}
      />

      <ContentWrapper limitWidth={false}>
        <div
          css={css`
            display: flex;
            justify-content: space-between;
          `}
        >
          <SmartLink
            css={css`
              display: flex;
              width: fit-content;
              font-size: 1.2rem;
              color: #000;
            `}
            href={`/org/${id}`}
          >
            <span
              css={css`
                display: flex;
                align-items: center;
              `}
            >
              <ChevronLeftIcon fontSize="1.8rem" />
              <b>Tilbake til {orgEnhet?.navn ?? id}</b>
            </span>
          </SmartLink>

          <Button
            icon={<FileExcelIcon />}
            loading={isDownloading}
            onClick={handleDownloadKontaktpersonerExcel}
            variant="secondary"
          >
            Eksportér til Excel
          </Button>
        </div>

        <KontaktpersonerTabellInner orgEnhet={orgEnhet} />
      </ContentWrapper>
    </>
  );
}

function KontaktpersonerTabellInner({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const kontaktpersonerQuery = useQuery(
    getKonaktpersonerForOrgEnhet(orgEnhet.id),
  );

  if (kontaktpersonerQuery.isLoading) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
        `}
      >
        <Loader size="3xlarge" />
        <span>Laster...</span>
      </div>
    );
  }

  const kontaktpersoner = kontaktpersonerQuery.data;

  const sortedKontaktpersoner = kontaktpersoner?.sort((a, b) => {
    const aFulltNavn = `${a.fornavn} ${a.etternavn}`;
    const bFulltNavn = `${b.fornavn} ${b.etternavn}`;

    const navnCompare = aFulltNavn.localeCompare(bFulltNavn, "no");
    if (navnCompare !== 0) return navnCompare;

    const aPrioritet = a.kontaktpersonPrioritet ?? "";
    const bPrioritet = b.kontaktpersonPrioritet ?? "";

    const prioritetCompare = aPrioritet.localeCompare(bPrioritet, "no");
    if (prioritetCompare !== 0) return prioritetCompare;

    return (a.kontaktpersonNavn ?? "").localeCompare(
      b.kontaktpersonNavn ?? "",
      "no",
    );
  });

  return (
    <Table
      css={css`
        margin-top: var(--a-spacing-4);
        padding: 6px;
        border-radius: 5px;
        background-color: #fff;

        --ac-table-row-border: #071a3636;
      `}
      zebraStripes
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
          `}
        >
          <Table.ColumnHeader scope="col">Nav-ident</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Navn ansatt</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Telefon ansatt</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Relasjon</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Telefon</Table.ColumnHeader>
          <Table.ColumnHeader scope="col">Prioritet</Table.ColumnHeader>
        </Table.Row>
      </Table.Header>

      <Table.Body
        css={css`
          height: 48px;
        `}
      >
        {sortedKontaktpersoner?.map((kontaktpersonRad, index) => (
          <Table.Row
            css={css`
              height: 48px;
            `}
            key={index}
            shadeOnHover={false}
          >
            <Table.DataCell>{kontaktpersonRad.navident}</Table.DataCell>
            <Table.DataCell>{`${kontaktpersonRad.fornavn} ${kontaktpersonRad.etternavn}`}</Table.DataCell>
            <Table.DataCell>
              {[
                formatTlf(kontaktpersonRad.jobbtelefon ?? ""),
                formatTlf(kontaktpersonRad.privattelefon ?? ""),
              ]
                .filter(Boolean)
                .join(", ")}
            </Table.DataCell>
            <Table.DataCell>
              {kontaktpersonRad.kontaktpersonNavn}
            </Table.DataCell>
            <Table.DataCell>
              {
                RelasjonNameMapping[
                  kontaktpersonRad.kontaktpersonRelasjon as Relasjon
                ]
              }
            </Table.DataCell>
            <Table.DataCell>
              {formatTlf(kontaktpersonRad.kontaktpersonTelefonnr ?? "")}
            </Table.DataCell>
            <Table.DataCell>
              {kontaktpersonRad.kontaktpersonPrioritet}
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
