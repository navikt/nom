import { css } from "@emotion/react";
import { ChevronLeftIcon, FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyShort,
  Box,
  Button,
  Checkbox,
  Heading,
  HStack,
  Label,
  Link,
  Loader,
  Modal,
  Table,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { useState } from "react";

import {
  getOrgEnhet,
  getOrgEnheterSomKanHaRessurser,
  getRessurserByListNavident,
  masseflytting,
} from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { PageBanner } from "~/components/PageBanner";
import { OrgEnhetSelect } from "~/components/select/OrgEnhetSelect";
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
import { formatVisningsnavn } from "~/utils";

const blockStyle = css`
  background-color: #deecef;
  padding: var(--a-spacing-4) var(--a-spacing-6);
  border-radius: var(--a-border-radius-large);
`;

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
      <PageBanner text={"Flytt medarbeidere"} />

      <ContentWrapper>
        <Link
          css={css`
            color: #000;
            font-size: 1.2rem;
            margin-bottom: var(--a-spacing-6);
          `}
          href={`/org/${id}`}
        >
          <ChevronLeftIcon fontSize="1.8rem" />
          <b>Tilbake til {orgEnhet?.navn ?? id}</b>
        </Link>

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

  const navigate = useNavigate();

  const masseflyttingMutation = useMutation<
    unknown,
    AxiosError<Error>,
    MasseFlyttingRequest
  >({
    mutationFn: async (request) => {
      return masseflytting(request);
    },
    onSuccess: async () => {
      navigate({
        to: `/org/${orgEnhet?.id}`,
      }).then();
    },
  });

  const { showModal } = useModal();

  async function handleClick() {
    if (valgteRessurser.length > 0 && !!masseflyttingTilOrgEnhet) {
      await showModal(
        <MasseflyttingBekreftModal
          beholdTilganger={beholdTilganger}
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
    <>
      <VStack
        css={css`
          background-color: #deecef;
          padding: var(--a-spacing-4) var(--a-spacing-6);
          border-radius: var(--a-border-radius-large);
          display: grid;
        `}
        gap="4"
      >
        <Heading level="1" size="medium">
          Flytting av medarbeidere{" "}
          {orgEnhet?.navn ? `i ${orgEnhet.navn} (${orgEnhet.id})` : ""}
        </Heading>
        <>
          <VStack css={blockStyle} gap="2">
            <Label
              css={css`
                font-size: var(--a-font-size-xlarge);
                font-weight: var(--a-font-weight-bold);
              `}
            >
              Fra
            </Label>

            <BodyShort
              css={css`
                font-size: var(--a-font-size-xlarge);
                padding: 0.5rem;
                border-radius: 0.5rem;
              `}
            >
              {orgEnhet.navn}
            </BodyShort>

            <EnhetKontekstInfo
              orgEnhet={orgEnheterSomKanHaRessurser.find(
                (it) => it.id === orgEnhet.id,
              )}
            />
          </VStack>
          <VStack css={blockStyle} gap={"2"}>
            <Box marginBlock={"0 2"}>
              <OrgEnhetSelect
                helpText="Med organisasjonsenhet menes hvilken enhet medarbeideren skal
              tilhøre og dermed hvilken leder som er ansvarlig for vedkommende."
                label="Til (velg orgenhet)"
                onChange={(value) => setMasseflyttingTilOrgEnhet(value)}
                orgEnheter={orgEnheterSomKanHaRessurser}
                value={masseflyttingTilOrgEnhet?.id}
              />
            </Box>

            <EnhetKontekstInfo orgEnhet={masseflyttingTilOrgEnhet} />

            <Checkbox
              checked={beholdTilganger}
              onChange={(event) => setBeholdTilganger(event.target.checked)}
            >
              Gamle tilganger skal bli med videre
            </Checkbox>

            <div
              css={css`
                align-self: end;
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
                disabled={
                  valgteRessurser.length === 0 || !masseflyttingTilOrgEnhet
                }
                onClick={() => {
                  handleClick();
                }}
                size="small"
              >
                Flytt medarbeidere
              </Button>
            </div>
          </VStack>
        </>
      </VStack>
      <div
        css={css`
          margin-left: var(--a-spacing-2);
          margin-top: var(--a-spacing-4);
          margin-bottom: var(--a-spacing-4);
          display: flex;
          justify-content: space-between;
        `}
      >
        <div>
          <Label>Vis sektorer:</Label>

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
    </>
  );
}

function EnhetKontekstInfo({
  orgEnhet,
  fallbackMessage,
}: {
  orgEnhet: OrgEnhetMedLedernavn | undefined;
  fallbackMessage?: string;
}) {
  const extraAgressoInfo = useExtraAgressoInfo(orgEnhet?.id);
  if (!orgEnhet) {
    return <>{fallbackMessage}</>;
  }

  return (
    <HStack gap={"4"} marginBlock={"0 0"}>
      <span>
        NOM-ID: <b>{orgEnhet?.id}</b>
      </span>
      <span>
        Leder: <b>{orgEnhet?.ledere[0]}</b>
      </span>
      <span>
        Enhet i Remedy: <b>{orgEnhet?.remedyEnhetId}</b>
      </span>
      {extraAgressoInfo && (
        <span>
          Agresso-ID: <b>{extraAgressoInfo.agressoId}</b>
        </span>
      )}
    </HStack>
  );
}

function useExtraAgressoInfo(orgEnhetId: string | undefined) {
  const qry = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId || "nothing"),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId || "nothing"),
    enabled: orgEnhetId !== undefined,
  });

  if (!qry.data) {
    return null;
  }
  return {
    agressoId: qry.data.agressoId,
    agressoOrgenhetType: qry.data.agressoOrgenhetType,
  };
}

function lagMasseFlyttingRequest(
  orgtilknytninger: OrgtilknytningState[],
  valgteRessurser: string[],
  tilOrgEnhet: string,
  beholdTilganger: boolean,
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
  };
}

function MasseflyttingBekreftModal({
  onCancel,
  onConfirm,
  valgteRessurser,
  ressurser,
  tilOrgEnhet,
  beholdTilganger,
}: {
  onCancel?: () => void;
  onConfirm?: () => void;
  valgteRessurser: string[];
  ressurser: Ressurs[];
  tilOrgEnhet: string;
  beholdTilganger: boolean;
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
        blir flyttet til organisasjonsenheten{" "}
        <span
          css={css`
            flex-wrap: nowrap;
            display: flex;
          `}
        >
          <b>{orgEnhet ? `${orgEnhet.navn} (${orgEnhet.id})` : tilOrgEnhet}</b>:
        </span>
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
