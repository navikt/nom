import { css } from "@emotion/react";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { Alert, Heading, Loader, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import React from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import type { Relasjon } from "~/pages/Parorendeinfo/ParorendeinfoPage";
import { RelasjonNameMapping } from "~/pages/Parorendeinfo/ParorendeinfoPage";
import type { OrgEnhet } from "~/types/api-models";
import { formatTlf } from "~/utils";

export function KontaktpersonerTabellPage() {
  const { id } = useParams({ from: "/kontaktpersoner-tabell/$id" });

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id),
    queryFn: () => getOrgEnhet.queryFn(id),
  });

  const orgEnhet = orgEnhetQuery.data;

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
          Kunne ikke hente organisasjonsenhet med id <b>{id}</b>
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

        <KontaktpersonerTabellInner orgEnhet={orgEnhet} />
      </ContentWrapper>
    </>
  );
}

function KontaktpersonerTabellInner({ orgEnhet }: { orgEnhet: OrgEnhet }) {
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
        {mockKontaktpersoner.map((kontaktpersonRad, index) => (
          <Table.Row
            css={css`
              height: 48px;
            `}
            key={index}
            shadeOnHover={false}
          >
            <Table.DataCell>{kontaktpersonRad.navident}</Table.DataCell>
            <Table.DataCell>{kontaktpersonRad.navn}</Table.DataCell>
            <Table.DataCell>
              {[
                formatTlf(kontaktpersonRad.jobbtelefon),
                formatTlf(kontaktpersonRad.privattelefon),
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
              {formatTlf(kontaktpersonRad.kontaktpersonTelefonnummer)}
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

interface Kontaktperson {
  navident: string;
  navn: string;
  jobbtelefon: string;
  privattelefon: string;
  kontaktpersonNavn: string;
  kontaktpersonRelasjon: string;
  kontaktpersonTelefonnummer: string;
  kontaktpersonPrioritet: string;
}

const mockKontaktpersoner: Kontaktperson[] = [
  {
    navident: "E000001",
    navn: "Test Testesen 1",
    jobbtelefon: "4700000001",
    privattelefon: "",
    kontaktpersonNavn: "",
    kontaktpersonRelasjon: "",
    kontaktpersonTelefonnummer: "",
    kontaktpersonPrioritet: "",
  },
  {
    navident: "M000001",
    navn: "Test Testesen 2",
    jobbtelefon: "4700000002",
    privattelefon: "",
    kontaktpersonNavn: "",
    kontaktpersonRelasjon: "",
    kontaktpersonTelefonnummer: "",
    kontaktpersonPrioritet: "",
  },
  {
    navident: "H000001",
    navn: "Test Testesen 3",
    jobbtelefon: "4700000003",
    privattelefon: "",
    kontaktpersonNavn: "",
    kontaktpersonRelasjon: "",
    kontaktpersonTelefonnummer: "",
    kontaktpersonPrioritet: "",
  },
  {
    navident: "K000001",
    navn: "Test Testesen 4",
    jobbtelefon: "4700000004",
    privattelefon: "",
    kontaktpersonNavn: "Derp Derpeses",
    kontaktpersonRelasjon: "BARN",
    kontaktpersonTelefonnummer: "12345678",
    kontaktpersonPrioritet: "1",
  },
  {
    navident: "K000001",
    navn: "Test Testesen 4",
    jobbtelefon: "4700000004",
    privattelefon: "",
    kontaktpersonNavn: "Derpina Derpeses",
    kontaktpersonRelasjon: "SOSKEN",
    kontaktpersonTelefonnummer: "56781234",
    kontaktpersonPrioritet: "1",
  },
  {
    navident: "K000001",
    navn: "Test Testesen 4",
    jobbtelefon: "4700000004",
    privattelefon: "",
    kontaktpersonNavn: "Derp jr Derpeses",
    kontaktpersonRelasjon: "BARN",
    kontaktpersonTelefonnummer: "87654321",
    kontaktpersonPrioritet: "1",
  },
  {
    navident: "M000002",
    navn: "Test Testesen 5",
    jobbtelefon: "4700000005",
    privattelefon: "",
    kontaktpersonNavn: "",
    kontaktpersonRelasjon: "",
    kontaktpersonTelefonnummer: "",
    kontaktpersonPrioritet: "",
  },
  {
    navident: "U000001",
    navn: "Test Testesen 6",
    jobbtelefon: "4700000006",
    privattelefon: "",
    kontaktpersonNavn: "",
    kontaktpersonRelasjon: "",
    kontaktpersonTelefonnummer: "",
    kontaktpersonPrioritet: "",
  },
  {
    navident: "W000001",
    navn: "Test Testesen 7",
    jobbtelefon: "4700000007",
    privattelefon: "",
    kontaktpersonNavn: "Testperson 1",
    kontaktpersonRelasjon: "SOSKEN",
    kontaktpersonTelefonnummer: "88776655",
    kontaktpersonPrioritet: "1",
  },
  {
    navident: "N000001",
    navn: "Test Testesen 8",
    jobbtelefon: "4700000008",
    privattelefon: "",
    kontaktpersonNavn: "",
    kontaktpersonRelasjon: "",
    kontaktpersonTelefonnummer: "",
    kontaktpersonPrioritet: "",
  },
];
