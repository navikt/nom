import { css } from "@emotion/react";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  DatePicker,
  Heading,
  List,
  Loader,
  Table,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import React, { useState } from "react";

import {
  avvikleOrgEnhet,
  getOrgEnhet,
  getOrgEnhetList,
  getRessurserByListNavident,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { BekreftModal } from "~/components/BekreftModal";
import ContentWrapper from "~/components/ContentWrapper";
import { LoaderWithText } from "~/components/LoaderWithText";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { formatDateWithoutTimezone } from "~/customUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  IkkeTilgangRedigering,
  OrgEnhetIkkeFunnet,
  OrgEnhetInfoWrapper,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetKvittering } from "~/pages/OrganizationMastering/Common/OrgEnhetKvittering";
import { nomDateIsAfter } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { AvvikleOrgEnhetRequest, OrgEnhet } from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

export const AvsluttOrgEnhet = () => {
  const { id } = useSearch({ from: "/avslutt-organisasjonsenhet" });

  const hasAdminAccess = useHasAdminAccess();

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id ?? ""),
    queryFn: () => getOrgEnhet.queryFn(id ?? ""),
    enabled: !!id,
  });

  const orgEnhet = orgEnhetQuery.data;

  if (!hasAdminAccess) {
    return (
      <ContentWrapper>
        <IkkeTilgangRedigering />
      </ContentWrapper>
    );
  }

  if (!id) {
    return (
      <ContentWrapper>
        <OrgEnhetIkkeFunnet />
      </ContentWrapper>
    );
  }

  const [changeInfo, setChangeInfo] = useState<any>();

  if (orgEnhetQuery.isLoading) {
    return (
      <ContentWrapper>
        <LoaderWithText>Laster...</LoaderWithText>
      </ContentWrapper>
    );
  }

  if (!orgEnhet) {
    return (
      <ContentWrapper>
        <OrgEnhetIkkeFunnet />
      </ContentWrapper>
    );
  }

  return (
    <>
      <PageBanner text="Avslutt organisasjonsenhet" />

      <ContentWrapper>
        {!!changeInfo ? (
          <KvitteringView changeInfo={changeInfo} orgEnhet={orgEnhet} />
        ) : (
          <FormView orgEnhet={orgEnhet} setChangeInfo={setChangeInfo} />
        )}
      </ContentWrapper>
    </>
  );
};

function FormView({
  orgEnhet,
  setChangeInfo,
}: {
  orgEnhet: OrgEnhet;
  setChangeInfo: (response: any) => void;
}) {
  return (
    <ModalProvider>
      <ContainerWithHighlightedTop padding={6}>
        <OrgEnhetInfoWrapper
          heading="Valgt organisasjonsenhet"
          orgEnhet={orgEnhet}
        />

        <AvsluttOrgEnhetForm
          orgEnhet={orgEnhet}
          setChangeInfo={setChangeInfo}
        />
      </ContainerWithHighlightedTop>

      <ModalComponent />
    </ModalProvider>
  );
}

function KvitteringView({
  orgEnhet,
  changeInfo,
}: {
  orgEnhet: OrgEnhet;
  changeInfo: any;
}) {
  return (
    <OrgEnhetKvittering
      orgEnhetId={orgEnhet.id}
      title="Leder på organisasjonsenhet oppdatert"
    >
      <AvsluttOrgEnhetKvitteringContent
        changeInfo={changeInfo}
        orgEnhet={orgEnhet}
      />
    </OrgEnhetKvittering>
  );
}

function AvsluttOrgEnhetForm({
  orgEnhet,
  setChangeInfo,
}: {
  orgEnhet: OrgEnhet;
  setChangeInfo: (response: any) => void;
}) {
  const [sluttdato, setSluttdato] = useState<string>("");

  const [displayDateError, setDisplayDateError] = useState<boolean>(false);
  const [displayErrorSummary, setDisplayErrorSummary] =
    useState<boolean>(false);

  const handleDateChange = (date: string) => {
    setSluttdato(date);

    if (!!date) {
      setDisplayDateError(false);
    } else {
      setDisplayErrorSummary(false);
    }
  };

  const avvikleOrgEnhetMutation = useMutation<
    unknown,
    AxiosError<Error>,
    AvvikleOrgEnhetRequest
  >({
    mutationFn: async (request) => {
      return avvikleOrgEnhet(orgEnhet.id, request);
    },
    onSuccess: async () => {
      setChangeInfo({ sluttdato });

      showToast({ text: "Avsluttet" });
    },
  });

  const { showModal } = useModal();

  const handleSaveClick = async () => {
    if (!sluttdato) {
      setDisplayDateError(true);
    } else {
      if (ressursAvhengigheter.length > 0 || orgEnhetAvhengigheter.length > 0) {
        setDisplayErrorSummary(true);
      } else {
        await showModal(
          <BekreftModal
            content={
              <AvsluttOrgEnhetModalContent
                orgEnhet={orgEnhet}
                sluttdato={sluttdato}
              />
            }
            heading="Bekreft avslutning"
          />,
        ).then(() => {
          avvikleOrgEnhetMutation.mutate({ gyldigTom: sluttdato });
        });
      }
    }
  };

  const ulovligeOrgtilknytninger = orgEnhet.ressursKoblinger.filter(
    (tilknytning) => nomDateIsAfter(tilknytning.gyldigTom, sluttdato),
  );

  const ulovligeNavidenter = ulovligeOrgtilknytninger.map(
    (tilknytning) => tilknytning.navident,
  );

  const ulovligeRessurserQuery = useQuery({
    queryKey: getRessurserByListNavident.queryKey(ulovligeNavidenter),
    queryFn: () => getRessurserByListNavident.queryFn(ulovligeNavidenter),
    enabled: ulovligeNavidenter.length > 0 && !!sluttdato,
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
    .sort((a, b) => a.navn.localeCompare(b.navn, "no"));

  const orgEnhetIder = orgEnhet.underordnedeOrgEnhetId;

  const underliggendeOrgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(orgEnhetIder),
    queryFn: () => getOrgEnhetList.queryFn(orgEnhetIder),
    enabled: orgEnhetIder.length > 0 && !!sluttdato,
  });

  const underliggendeOrgEnheter = underliggendeOrgEnheterQuery.data;

  const orgEnhetAvhengigheter =
    underliggendeOrgEnheter
      ?.filter((enhet) => nomDateIsAfter(enhet.gyldigTom, sluttdato))
      .sort((a, b) => a.navn.localeCompare(b.navn, "no")) ?? [];

  return (
    <>
      <DatePickerWrapper
        error={displayDateError && !sluttdato ? "Obligatorisk" : undefined}
        onChange={handleDateChange}
      />

      {!!sluttdato && (
        <AvslutningTillattStatus
          loading={
            ulovligeRessurserQuery.isLoading ||
            underliggendeOrgEnheterQuery.isLoading
          }
          orgEnhetAvhengigheter={orgEnhetAvhengigheter}
          ressursAvhengigheter={ressursAvhengigheter}
        />
      )}

      {displayErrorSummary &&
        (ressursAvhengigheter.length > 0 ||
          orgEnhetAvhengigheter.length > 0) && (
          <Alert variant="error">
            <Heading level="3" size="small">
              For å kunne gå videre må du rette opp i følgende:
            </Heading>

            <List>
              {ressursAvhengigheter.length > 0 && (
                <List.Item>
                  Medarbeidere ansatt i denne enheten må flyttes eller avvikles
                  før angitt dato
                </List.Item>
              )}

              {orgEnhetAvhengigheter.length > 0 && (
                <List.Item>
                  Enheter under denne enheten må flyttes eller avvikles før
                  angitt dato
                </List.Item>
              )}
            </List>
          </Alert>
        )}

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

        <Button
          icon={<FloppydiskIcon aria-hidden />}
          onClick={handleSaveClick}
          size="small"
        >
          Lagre
        </Button>
      </div>
    </>
  );
}

function AvslutningTillattStatus({
  ressursAvhengigheter,
  orgEnhetAvhengigheter,
  loading,
}: {
  ressursAvhengigheter: RessursAvhengighet[];
  orgEnhetAvhengigheter: OrgEnhet[];
  loading: boolean;
}) {
  return (
    <VStack gap="4">
      <Heading level="2" size="medium">
        Avhengigheter
      </Heading>

      <AvslutningTillatStatusInner
        loading={loading}
        orgEnhetAvhengigheter={orgEnhetAvhengigheter}
        ressursAvhengigheter={ressursAvhengigheter}
      />
    </VStack>
  );
}

function AvslutningTillatStatusInner({
  ressursAvhengigheter,
  orgEnhetAvhengigheter,
  loading,
}: {
  ressursAvhengigheter: RessursAvhengighet[];
  orgEnhetAvhengigheter: OrgEnhet[];
  loading: boolean;
}) {
  if (loading) {
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
  if (ressursAvhengigheter?.length === 0) {
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
  if (orgEnhetAvhengigheter.length === 0) {
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

function AvsluttOrgEnhetModalContent({
  orgEnhet,
  sluttdato,
}: {
  orgEnhet: OrgEnhet;
  sluttdato: string;
}) {
  return (
    <BodyLong>
      Avslutte <b>{orgEnhet.navn}</b> fra og med{" "}
      <b>{formatDisplayDate(sluttdato)}</b>.
    </BodyLong>
  );
}

function AvsluttOrgEnhetKvitteringContent({
  orgEnhet,
  changeInfo,
}: {
  orgEnhet: OrgEnhet;
  changeInfo: any;
}) {
  return (
    <BodyLong>
      <b>{orgEnhet.navn}</b> avsluttet fra og med{" "}
      <b>{formatDisplayDate(changeInfo.sluttdato)}</b>.
    </BodyLong>
  );
}

interface RessursAvhengighet {
  navident: string;
  navn: string;
  sluttdato?: string;
}
