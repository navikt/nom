import { css } from "@emotion/react";
import { ChevronLeftIcon, FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyShort,
  Button,
  Checkbox,
  DatePicker,
  Heading,
  Label,
  Link,
  Loader,
  Modal,
  Table,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import type { AxiosError, AxiosResponse } from "axios";
import React, { useState } from "react";

import {
  getOrgEnhet,
  getOrgEnheterSomKanHaRessurser,
  getRessurserByListNavident,
  masseflytting,
} from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { EnhetKontekstInfo } from "~/components/EnhetKontekstInfo";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { Pluralize } from "~/components/Pluralize";
import { OrgEnhetSelect } from "~/components/select/OrgEnhetSelect";
import {
  dateIsAfterToday,
  dateWithoutTimezone,
  formatDateWithoutTimezone,
  todayWithoutTimezone,
} from "~/dateUtils/dateUtils";
import { useTableSort } from "~/hooks/useTableSort";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import {
  isPeriodActive,
  nomDateIsTodayOrAfter,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type {
  MasseFlyttingRequest,
  OrgEnhet,
  OrgEnhetMedLedernavn,
  OrgtilknytningState,
  Ressurs,
} from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

export function Masseflytting() {
  const { id } = useParams({ from: "/masseflytting/$id" });

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id as string),
    queryFn: () => getOrgEnhet.queryFn(id as string),
    enabled: !!id,
  });

  const orgEnhet = orgEnhetQuery.data;

  return (
    <>
      <PageBanner text="Flytt medarbeidere" />

      <ContentWrapper>
        <SmartLink
          css={css`
            display: flex;
            width: fit-content;
            margin-bottom: var(--a-spacing-4);
            color: #000;
          `}
          href={`/org/${id}`}
        >
          <span
            css={css`
              display: flex;
              align-items: center;
              font-size: 1.2rem;
            `}
          >
            <ChevronLeftIcon fontSize="1.8rem" />
            <b>Tilbake til {orgEnhet?.navn ?? id}</b>
          </span>
        </SmartLink>

        <ModalProvider>
          <MasseflyttingTable orgEnhet={orgEnhet} />

          <ModalComponent />
        </ModalProvider>
      </ContentWrapper>
    </>
  );
}

function MasseflyttingTable({ orgEnhet }: { orgEnhet: OrgEnhet | undefined }) {
  const aktiveOrgtilknytninger =
    orgEnhet?.ressursKoblinger?.filter((tilknytning) =>
      isPeriodActive(tilknytning.gyldigFom, tilknytning.gyldigTom),
    ) ?? [];

  const aktiveNavIdentList =
    aktiveOrgtilknytninger.map((tilknytning) => tilknytning.navident) ?? [];

  const hentedeRessurserQuery = useQuery({
    queryKey: getRessurserByListNavident.queryKey(aktiveNavIdentList),
    queryFn: () => getRessurserByListNavident.queryFn(aktiveNavIdentList),
    enabled: aktiveNavIdentList.length > 0,
  });

  const ressurser =
    hentedeRessurserQuery.data?.filter((ressurs) =>
      nomDateIsTodayOrAfter(ressurs.sluttdato),
    ) ?? [];

  const [sektorFilter, setSektorFilter] = useState({
    statlig: true,
    kommunal: true,
    ekstern: true,
  });

  const filteredRessurser = ressurser.filter((ressurs) => {
    if (!ressurs.sektor) {
      return true;
    }

    switch (ressurs.sektor) {
      case Sektor.NAV_STATLIG: {
        return sektorFilter.statlig;
      }
      case Sektor.NAV_KOMMUNAL: {
        return sektorFilter.kommunal;
      }
      case Sektor.EKSTERN: {
        return sektorFilter.ekstern;
      }
    }
  });

  const { data, sort, handleSort } = useTableSort(
    filteredRessurser,
    "visningsnavn.fornavn",
  );

  const sortedRessurser = data;

  const [valgteRessurser, setValgteRessurser] = useState<string[]>([]);

  const [masseflyttingTilOrgEnhet, setMasseflyttingTilOrgEnhet] = useState<
    OrgEnhetMedLedernavn | undefined
  >();

  const [beholdTilganger, setBeholdTilganger] = useState<boolean>(true);
  const [fraDato, setFraDato] = useState<string | null>(
    formatDateWithoutTimezone(
      dateIsAfterToday("2025-05-01")
        ? new Date("2025-05-01")
        : todayWithoutTimezone(),
    ),
  );

  const valgtTilOrgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(masseflyttingTilOrgEnhet?.id ?? ""),
    queryFn: () => getOrgEnhet.queryFn(masseflyttingTilOrgEnhet?.id ?? ""),
    enabled: !!masseflyttingTilOrgEnhet?.id,
  });

  const valgtTilOrgEnhet = valgtTilOrgEnhetQuery.data;

  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      setFraDato(date ? formatDateWithoutTimezone(date) : null);
    },
    defaultSelected: fraDato ? dateWithoutTimezone(fraDato) : undefined,
  });

  function toggleValgtRessurs(value: string, state: boolean) {
    setValgteRessurser((liste) =>
      state
        ? [...liste, value]
        : liste.filter((navident) => navident !== value),
    );
  }

  function toggleSektorFilter(filterSektor: Sektor, state: boolean) {
    switch (filterSektor) {
      case Sektor.NAV_STATLIG: {
        setSektorFilter((valgtSektorFilter) => {
          return { ...valgtSektorFilter, statlig: state };
        });
        break;
      }
      case Sektor.NAV_KOMMUNAL: {
        setSektorFilter((valgtSektorFilter) => {
          return { ...valgtSektorFilter, kommunal: state };
        });
        break;
      }
      case Sektor.EKSTERN: {
        setSektorFilter((valgtSektorFilter) => {
          return { ...valgtSektorFilter, ekstern: state };
        });
        break;
      }
    }

    if (!state) {
      setValgteRessurser((valgteRessurserListe) =>
        valgteRessurserListe.filter(
          (navident) =>
            !ressurser.some(
              (ressurs) =>
                ressurs.navident === navident &&
                ressurs.sektor === filterSektor,
            ),
        ),
      );
    }
  }

  const orgEnheterSomKanHaRessurser =
    useQuery(getOrgEnheterSomKanHaRessurser).data || [];

  const [masseflyttingResponse, setMasseflyttingResponse] =
    useState<MasseflyttingResponseInfo>();

  const masseflyttingMutation = useMutation<
    AxiosResponse<MasseflyttingData>,
    AxiosError<Error>,
    MasseFlyttingRequest
  >({
    mutationFn: async (request) => {
      return await masseflytting(request);
    },
    onSuccess: (response) => {
      setMasseflyttingResponse({
        status: response.status,
        data: response.data,
      });
    },
    onError: (error) => {
      setMasseflyttingResponse({
        status: error.response?.status ?? 500,
      });
    },
  });

  const { showModal } = useModal();

  async function handleClick() {
    if (valgteRessurser.length > 0 && !!masseflyttingTilOrgEnhet) {
      await showModal(
        <MasseflyttingBekreftModal
          beholdTilganger={beholdTilganger}
          gyldigFom={fraDato}
          ressurser={sortedRessurser}
          tilOrgEnhet={masseflyttingTilOrgEnhet.id}
          valgteRessurser={valgteRessurser}
        />,
      )
        .then(() =>
          masseflyttingMutation.mutate(
            lagMasseFlyttingRequest(
              aktiveOrgtilknytninger ?? [],
              valgteRessurser,
              masseflyttingTilOrgEnhet.id,
              beholdTilganger,
              fraDato,
            ),
          ),
        )
        .catch(() => {});
    }
  }

  if (hentedeRessurserQuery.isLoading || !orgEnhet) {
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

  return (
    <VStack gap="4">
      <div
        css={css`
          background-color: #deecef;
          padding: var(--a-spacing-4) var(--a-spacing-6);
          border-radius: var(--a-border-radius-large);
          display: grid;
          gap: var(--a-spacing-2);
        `}
      >
        <Heading level="1" size="xsmall">
          Flytt fra
        </Heading>

        <OrgEnhetInfo orgEnhet={orgEnhet} />
      </div>

      <VStack
        css={css`
          background-color: #deecef;
          padding: var(--a-spacing-4) var(--a-spacing-6);
          border-radius: var(--a-border-radius-large);
          display: grid;
        `}
        gap="4"
      >
        <VStack gap="2">
          <Heading level="1" size="xsmall">
            Flytt til
          </Heading>

          <OrgEnhetSelect
            helpText="Med organisasjonsenhet menes hvilken enhet medarbeideren skal
              tilhøre og dermed hvilken leder som er ansvarlig for vedkommende."
            label="Velg organisasjonsenhet"
            onChange={(value) => setMasseflyttingTilOrgEnhet(value)}
            orgEnheter={orgEnheterSomKanHaRessurser}
            value={masseflyttingTilOrgEnhet?.id}
          />

          <EnhetKontekstInfo displayUni4Niv orgEnhet={valgtTilOrgEnhet} />
        </VStack>

        <Checkbox
          checked={beholdTilganger}
          onChange={(event) => setBeholdTilganger(event.target.checked)}
        >
          Gamle tilganger skal bli med videre
        </Checkbox>

        <DatePicker {...datePickerProperties.datepickerProps}>
          <DatePicker.Input
            {...datePickerProperties.inputProps}
            label="Gyldig fra og med"
          />
        </DatePicker>

        <div
          css={css`
            justify-self: end;
            width: fit-content;
            display: flex;
            align-items: center;
          `}
        >
          <BodyShort>
            <b>{valgteRessurser.length}</b> medarbeidere valgt
          </BodyShort>

          <Button
            css={css`
              margin-left: var(--a-spacing-4);
            `}
            disabled={valgteRessurser.length === 0 || !masseflyttingTilOrgEnhet}
            onClick={() => {
              handleClick().then(() => {});
            }}
            size="small"
          >
            Flytt medarbeidere
          </Button>
        </div>
      </VStack>

      <MasseflyttingResponseAlert
        orgtilknytninger={aktiveOrgtilknytninger}
        response={masseflyttingResponse}
        ressurser={sortedRessurser}
      />

      <div
        css={css`
          margin-left: var(--a-spacing-2);
          display: flex;
        `}
      >
        <div>
          <Label>Vis sektorer:</Label>

          <div
            css={css`
              display: flex;
              gap: var(--a-spacing-4);
            `}
          >
            <Checkbox
              checked={sektorFilter.statlig}
              onChange={(event) =>
                toggleSektorFilter(Sektor.NAV_STATLIG, event.target.checked)
              }
              size="small"
            >
              Statlig
            </Checkbox>

            <Checkbox
              checked={sektorFilter.kommunal}
              onChange={(event) =>
                toggleSektorFilter(Sektor.NAV_KOMMUNAL, event.target.checked)
              }
              size="small"
            >
              Kommunal
            </Checkbox>

            <Checkbox
              checked={sektorFilter.ekstern}
              onChange={(event) =>
                toggleSektorFilter(Sektor.EKSTERN, event.target.checked)
              }
              size="small"
            >
              Ekstern
            </Checkbox>
          </div>
        </div>
      </div>

      <Table
        css={css`
          background-color: #fff;
          padding: 6px;
          border-radius: 5px;

          --ac-table-row-border: #071a3636;
        `}
        onSortChange={(sortKey) => handleSort(sortKey)}
        sort={sort}
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
            <Table.ColumnHeader scope="col">
              Organisasjonsenhet
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col">
              <Checkbox
                checked={
                  valgteRessurser.length === sortedRessurser.length &&
                  sortedRessurser.length > 0
                }
                disabled={sortedRessurser.length === 0}
                indeterminate={
                  valgteRessurser.length > 0 &&
                  valgteRessurser.length < sortedRessurser.length
                }
                onChange={() =>
                  setValgteRessurser(
                    valgteRessurser.length > 0
                      ? []
                      : sortedRessurser.map((ressurs) => ressurs.navident),
                  )
                }
              >
                Velg alle
              </Checkbox>
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {sortedRessurser.map((ressurs) => (
            <Table.Row
              css={css`
                --ac-table-row-border: none;
                height: 48px;
                border-width: 0 0 1px;
              `}
              key={ressurs.navident}
              selected={valgteRessurser.includes(ressurs.navident)}
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
              <Table.DataCell>{`${orgEnhet.navn} (${orgEnhet.id})`}</Table.DataCell>
              <Table.DataCell>
                <Checkbox
                  checked={valgteRessurser.includes(ressurs.navident)}
                  hideLabel
                  onChange={(event) =>
                    toggleValgtRessurs(ressurs.navident, event.target.checked)
                  }
                >
                  Velg
                </Checkbox>
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>

      {sortedRessurser.length === 0 && (
        <div
          css={css`
            display: block;
            margin-top: var(--a-spacing-8);
            justify-items: center;
          `}
        >
          <BodyShort
            css={css`
              font-size: var(--a-font-size-heading-2xlarge);
              font-weight: bold;
            `}
          >
            Ingen ansatte å vise
          </BodyShort>
        </div>
      )}
    </VStack>
  );
}

export function OrgEnhetInfo({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  return (
    <VStack gap="2">
      <BodyShort
        css={css`
          font-size: var(--a-font-size-xlarge);
        `}
      >
        <b>{orgEnhet.navn}</b>
      </BodyShort>

      <EnhetKontekstInfo orgEnhet={orgEnhet} />
    </VStack>
  );
}

function lagMasseFlyttingRequest(
  orgtilknytninger: OrgtilknytningState[],
  valgteRessurser: string[],
  tilOrgEnhet: string,
  beholdTilganger: boolean,
  gyldigFom: string | null,
): MasseFlyttingRequest {
  const valgteRessurserOrgtilknytningFids = valgteRessurser.map(
    (ressurs) =>
      orgtilknytninger.find(
        (orgtilknytning) => orgtilknytning.navident === ressurs,
      )?.fid,
  );

  return {
    beholdTilganger: beholdTilganger,
    nyOrgenhetId: tilOrgEnhet,
    orgtilknytningerFid: valgteRessurserOrgtilknytningFids as string[],
    gyldigFomFlyttedato:
      gyldigFom ?? formatDateWithoutTimezone(todayWithoutTimezone()),
  };
}

function MasseflyttingBekreftModal({
  onCancel,
  onConfirm,
  valgteRessurser,
  ressurser,
  tilOrgEnhet,
  beholdTilganger,
  gyldigFom,
}: {
  onCancel?: () => void;
  onConfirm?: () => void;
  valgteRessurser: string[];
  ressurser: Ressurs[];
  tilOrgEnhet: string;
  beholdTilganger: boolean;
  gyldigFom: string | null;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(tilOrgEnhet as string),
    queryFn: () => getOrgEnhet.queryFn(tilOrgEnhet as string),
    enabled: !!tilOrgEnhet,
  });

  const orgEnhet = orgEnhetQuery.data;

  return (
    <Modal
      css={css`
        padding: var(--a-spacing-6);
        border-radius: 0;
      `}
      header={{ heading: "Bekreft flytting", closeButton: false }}
      onClose={onCancel}
      open
      width="548px"
    >
      <Modal.Body>
        {valgteRessurser.length > 1 ? (
          <>
            <b>{valgteRessurser.length}</b> medarbeidere
          </>
        ) : (
          "Én medarbeider"
        )}{" "}
        blir fra{" "}
        <b>
          {gyldigFom
            ? formatDisplayDate(gyldigFom)
            : formatDisplayDate(todayWithoutTimezone())}
        </b>{" "}
        flyttet til organisasjonsenheten{" "}
        <b>{orgEnhet ? `${orgEnhet.navn} (${orgEnhet.id})` : tilOrgEnhet}</b>:
        <VStack
          css={css`
            max-height: 500px;
            background: var(--a-grayalpha-100);
            margin-top: var(--a-spacing-4);
          `}
          gap="1"
          overflow="auto"
        >
          {valgteRessurser.map((navident) => {
            const ressurs = ressurser.find(
              (ressurs) => ressurs.navident === navident,
            );

            return (
              <div
                css={css`
                  display: flex;
                  justify-content: space-between;
                  background: #fff;
                `}
                key={navident}
              >
                <BodyShort>
                  {formatVisningsnavn(ressurs?.visningsnavn)}
                </BodyShort>

                <BodyShort>({ressurs?.navident})</BodyShort>
              </div>
            );
          })}
        </VStack>
        <BodyShort
          css={css`
            margin-top: var(--a-spacing-4);
          `}
        >
          Eksisterende tilganger blir {beholdTilganger ? "" : <b>ikke</b>}{" "}
          overført til den nye enheten.
        </BodyShort>
      </Modal.Body>

      <Modal.Footer>
        <Button
          icon={<FloppydiskIcon aria-hidden />}
          onClick={onConfirm}
          size="small"
        >
          Bekreft flytting
        </Button>

        <Button onClick={onCancel} size="small" variant="secondary">
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

type FeiletFlytting = {
  orgtilknytningFid: string;
  feilmelding: string;
};

export type MasseflyttingData = {
  feiledeFlyttinger: FeiletFlytting[];
  antallFlyttet: number;
};

type MasseflyttingResponseInfo = {
  status: number;
  data?: MasseflyttingData;
};

function MasseflyttingResponseAlert({
  response,
  orgtilknytninger,
  ressurser,
}: {
  response: MasseflyttingResponseInfo | undefined;
  orgtilknytninger: OrgtilknytningState[];
  ressurser: Ressurs[];
}) {
  if (!response) {
    return;
  }

  if (response.status !== 200) {
    return (
      <Alert variant="error">Det oppstod en feil ved masseflyttingen</Alert>
    );
  }

  const data = response.data;

  if (data?.feiledeFlyttinger.length === 0) {
    return <Alert variant="success">Masseflytting vellykket</Alert>;
  }

  return (
    <Alert variant="warning">
      <VStack gap="4">
        <Heading size="small">
          Flytting av {data?.feiledeFlyttinger.length}{" "}
          <Pluralize
            numberOfElements={data?.feiledeFlyttinger.length ?? 0}
            pluralForm="ressurser"
            singleForm="ressurs"
          />{" "}
          feilet:
        </Heading>

        {data?.feiledeFlyttinger.map((flytting) => {
          const feiletOt = orgtilknytninger.find(
            (orgtilknytning) =>
              orgtilknytning.fid === flytting.orgtilknytningFid,
          );

          const feiletRessurs = ressurser.find(
            (ressurs) => ressurs.navident === feiletOt?.navident,
          );

          return (
            <BodyShort key={flytting.orgtilknytningFid}>
              <b>
                {feiletRessurs
                  ? formatVisningsnavn(feiletRessurs.visningsnavn)
                  : ""}
              </b>
              : "{flytting.feilmelding}"
            </BodyShort>
          );
        })}
      </VStack>
    </Alert>
  );
}
