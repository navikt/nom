import { css } from "@emotion/react";
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  DatePicker,
  Heading,
  Loader,
  Table,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import React, { useState } from "react";

import {
  getOrgEnhet,
  getOrgEnhetList,
  getRessurserByListNavident,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { OrgEnhetInfoWrapper } from "~/pages/OrganizationMastering/OpprettOrgEnhet";
import { nomDateIsAfter } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { OrgEnhet } from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

export const AvsluttOrgEnhet = () => {
  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return;
  }

  const { id } = useSearch({ from: "/avslutt-organisasjonsenhet" });

  return (
    <>
      <PageBanner text="Avslutt organisasjonsenhet" />

      <ContentWrapper>
        <ContainerWithHighlightedTop padding={6}>
          <AvslutteOrgEnhet orgEnhetId={id} />
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
};

function AvslutteOrgEnhet({ orgEnhetId }: { orgEnhetId?: string }) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId ?? ""),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId ?? ""),
    enabled: !!orgEnhetId,
  });

  const orgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--a-spacing-2);
        `}
      >
        <Loader size="3xlarge" />
        <BodyShort size="large">Laster...</BodyShort>
      </div>
    );
  }

  if (!orgEnhet) {
    return;
  }

  return (
    <>
      <OrgEnhetInfoWrapper
        heading="Avslutt organisasjonsenhet"
        orgEnhetId={orgEnhetId}
      />

      <AvsluttOrgEnhetInner orgEnhet={orgEnhet} />

      <div
        css={css`
          display: flex;
          justify-content: end;
          gap: var(--a-spacing-4);
        `}
      >
        <SmartLink href="/organisasjonsmastring">
          <Button size="small" variant="secondary">
            Avbryt
          </Button>
        </SmartLink>

        <Button size="small">Lagre</Button>
      </div>
    </>
  );
}

function AvsluttOrgEnhetInner({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const [sluttdato, setSluttdato] = useState<string>("");

  return (
    <>
      <DatePickerWrapper onChange={setSluttdato} />

      <AvslutningTillattStatus orgEnhet={orgEnhet} sluttdato={sluttdato} />
    </>
  );
}

interface RessursAvhengighet {
  navident: string;
  navn: string;
  sluttdato?: string;
}

function AvslutningTillattStatus({
  sluttdato,
  orgEnhet,
}: {
  sluttdato?: string;
  orgEnhet: OrgEnhet;
}) {
  if (!sluttdato) {
    return;
  }

  return (
    <VStack gap="4">
      <Heading level="2" size="medium">
        Avhengigheter:
      </Heading>

      <AvslutningTillatStatusInner orgEnhet={orgEnhet} sluttdato={sluttdato} />
    </VStack>
  );
}

function AvslutningTillatStatusInner({
  sluttdato,
  orgEnhet,
}: {
  sluttdato: string;
  orgEnhet: OrgEnhet;
}) {
  const ulovligeOrgtilknytninger = orgEnhet.ressursKoblinger.filter(
    (tilknytning) => nomDateIsAfter(tilknytning.gyldigTom, sluttdato),
  );

  const ulovligeNavidenter = ulovligeOrgtilknytninger.map(
    (tilknytning) => tilknytning.navident,
  );

  const ulovligeRessurserQuery = useQuery({
    queryKey: getRessurserByListNavident.queryKey(ulovligeNavidenter),
    queryFn: () => getRessurserByListNavident.queryFn(ulovligeNavidenter),
    enabled: ulovligeNavidenter.length > 0,
  });

  const ulovligeRessurser = ulovligeRessurserQuery.data;

  const ressursAvhengigheter: RessursAvhengighet[] = ulovligeOrgtilknytninger
    .map((tilknytning) => ({
      navident: tilknytning.navident,
      navn: formatVisningsnavn(
        ulovligeRessurser?.find(
          (ressurs) => ressurs.navident === tilknytning.navident,
        )?.visningsnavn,
      ),
      sluttdato: tilknytning.gyldigTom,
    }))
    .sort((a, b) => a.navn.localeCompare(b.navn));

  const orgEnhetIder = orgEnhet.underordnedeOrgEnhetId;

  const underliggendeOrgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(orgEnhetIder),
    queryFn: () => getOrgEnhetList.queryFn(orgEnhetIder),
    enabled: orgEnhetIder.length > 0,
  });

  const underliggendeOrgEnheter = underliggendeOrgEnheterQuery.data;

  const orgEnhetAvhengigheter = (
    underliggendeOrgEnheter?.filter((enhet) =>
      nomDateIsAfter(enhet.gyldigTom, sluttdato),
    ) ?? []
  ).sort((a, b) => a.navn.localeCompare(b.navn));

  if (
    ulovligeRessurserQuery.isLoading ||
    underliggendeOrgEnheterQuery.isLoading
  ) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--a-spacing-2);
        `}
      >
        <Loader size="3xlarge" />
        <BodyShort size="large">Laster...</BodyShort>
      </div>
    );
  }

  if (ressursAvhengigheter.length === 0 && orgEnhetAvhengigheter.length === 0) {
    return (
      <Alert variant="success">
        Det er ingen avhengigheter som går utover den valgte datoen.
      </Alert>
    );
  }

  return (
    <VStack gap="4">
      <Alert variant="warning">
        <VStack gap="2">
          <Heading level="3" size="small">
            Det er avhengigheter til denne enheten som går utover valgt dato
          </Heading>

          <BodyLong>
            Det er medarbeidere eller underenheter knyttet til enheten etter den
            valgte datoen. Disse må flyttes eller avvikles før du kan gå videre.
          </BodyLong>
        </VStack>
      </Alert>

      <VStack gap="8">
        <RessursAvhengigheter ressursAvhengigheter={ressursAvhengigheter} />

        <OrgEnhetAvhengigheter orgEnhetAvhengigheter={orgEnhetAvhengigheter} />
      </VStack>
    </VStack>
  );
}

function RessursAvhengigheter({
  ressursAvhengigheter,
}: {
  ressursAvhengigheter: RessursAvhengighet[];
}) {
  if (!ressursAvhengigheter || ressursAvhengigheter?.length === 0) {
    return;
  }

  return (
    <VStack gap="2">
      <Heading level="3" size="small">
        Disse medarbeiderne må flyttes med dato før enhetens sluttdato:
      </Heading>

      <Table
        css={css`
          background-color: #fff;
          padding: 6px;
          border-radius: 5px;

          --ac-table-row-border: #071a3636;
        `}
        size="small"
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
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Siste dato</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>

        <Table.Body
          css={css`
            height: 48px;
          `}
        >
          {ressursAvhengigheter.map((avhengighet, index) => (
            <Table.Row
              css={css`
                height: 48px;
              `}
              key={index}
              shadeOnHover={false}
            >
              <Table.DataCell>{avhengighet.navident}</Table.DataCell>
              <Table.DataCell>{avhengighet.navn}</Table.DataCell>
              <Table.DataCell>
                {avhengighet.sluttdato
                  ? formatDisplayDate(avhengighet.sluttdato)
                  : "Ikke satt"}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </VStack>
  );
}

function OrgEnhetAvhengigheter({
  orgEnhetAvhengigheter,
}: {
  orgEnhetAvhengigheter: OrgEnhet[];
}) {
  if (!orgEnhetAvhengigheter || orgEnhetAvhengigheter.length === 0) {
    return;
  }

  return (
    <VStack gap="2">
      <Heading level="3" size="small">
        Disse organisasjonsenhetene må flyttes med dato før enhetens sluttdato:
      </Heading>

      <Table
        css={css`
          background-color: #fff;
          padding: 6px;
          border-radius: 5px;

          --ac-table-row-border: #071a3636;
        `}
        size="small"
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
            <Table.ColumnHeader scope="col">ID</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Siste dato</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>

        <Table.Body
          css={css`
            height: 48px;
          `}
        >
          {orgEnhetAvhengigheter.map((enhet) => (
            <Table.Row
              css={css`
                height: 48px;
              `}
              key={enhet.fid}
              shadeOnHover={false}
            >
              <Table.DataCell>{enhet.id}</Table.DataCell>
              <Table.DataCell>{enhet.navn}</Table.DataCell>
              <Table.DataCell>
                {enhet.gyldigTom
                  ? formatDisplayDate(enhet.gyldigTom)
                  : "Ikke satt"}
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </VStack>
  );
}

function DatePickerWrapper({
  onChange,
  error,
}: {
  onChange: (date: string) => void;
  error?: string;
}) {
  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      onChange(date ? formatDateWithoutTimezone(date) : "");
    },
  });

  return (
    <DatePicker {...datePickerProperties.datepickerProps}>
      <DatePicker.Input
        error={error}
        {...datePickerProperties.inputProps}
        label="Avslutningsdato"
      />
    </DatePicker>
  );
}
