import { css } from "@emotion/react";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  Checkbox,
  Heading,
  Label,
  Loader,
  Modal,
  Table,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import React, { useState } from "react";

import type { FysiskBrukersted } from "~/api/react-query-endpoints";
import {
  getFysiskeBrukersteder,
  getOrgEnhetList,
  getOrgtilknytningerByNavidentList,
  getRessurserForFysiskBrukersted,
  updateFysiskBrukerstedForRessurser,
} from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { FysiskBrukerstedSelect } from "~/components/select/FysiskBrukerstedSelect";
import { useTableSort } from "~/hooks/useTableSort";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import {
  isPeriodActive,
  nomDateIsTodayOrAfter,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type {
  OrgtilknytningState,
  UpdatePrimaryFysiskBrukerstedForRessurserRequest,
  Visningsnavn,
} from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import { capitalizeNameFysiskBrukersted, formatVisningsnavn } from "~/utils";

export function MasseflyttingFysiskBrukersted() {
  return (
    <>
      <PageBanner text={"Flytt fysisk brukersted"} />

      <ContentWrapper>
        <ModalProvider>
          <FysiskBrukerstedFlyttingTable />

          <ModalComponent />
        </ModalProvider>
      </ContentWrapper>
    </>
  );
}

function FysiskBrukerstedFlyttingTable() {
  const fysiskeBrukerstederQuery = useQuery(getFysiskeBrukersteder);
  const fysiskeBrukersteder = fysiskeBrukerstederQuery.data || [];

  const [fysiskBrukerstedFra, setFysiskBrukerstedFra] = useState("");
  const [selectedFysiskBrukerstedFra, setSelectedFysiskBrukerstedFra] =
    useState("");

  const [fysiskBrukerstedTil, setFysiskBrukerstedTil] = useState("");

  const [sektorFilter, setSektorFilter] = useState({
    statlig: true,
    kommunal: true,
    ekstern: true,
  });

  const [valgteRessurser, setValgteRessurser] = useState<string[]>([]);

  const getFysiskBrukerstedRessurserQuery = useQuery({
    queryKey: getRessurserForFysiskBrukersted.queryKey(fysiskBrukerstedFra),
    queryFn: () => getRessurserForFysiskBrukersted.queryFn(fysiskBrukerstedFra),
    enabled: !!fysiskBrukerstedFra,
  });

  const hentedeRessurser = getFysiskBrukerstedRessurserQuery.data;

  const ressurser =
    hentedeRessurser?.filter((ressurs) =>
      ressurs.startdato
        ? isPeriodActive(ressurs.startdato, ressurs.sluttdato)
        : nomDateIsTodayOrAfter(ressurs.sluttdato),
    ) ?? [];

  const navidentListe = ressurser.map((ressurs) => ressurs.navident);

  const orgtilknytningerQuery = useQuery({
    queryKey: getOrgtilknytningerByNavidentList.queryKey(navidentListe),
    queryFn: () => getOrgtilknytningerByNavidentList.queryFn(navidentListe),
    enabled: navidentListe.length > 0,
  });

  const hentedeOrgtilknytninger = orgtilknytningerQuery.data;

  const orgtilknytninger: OrgtilknytningState[] = hentedeOrgtilknytninger
    ? navidentListe
        .map(
          (navident) =>
            hentedeOrgtilknytninger[navident]?.find(
              (tilknytning: OrgtilknytningState) =>
                tilknytning.erDagligOppfolging &&
                isPeriodActive(tilknytning.gyldigFom, tilknytning.gyldigTom),
            ) ?? null,
        )
        .filter(
          (tilknytning): tilknytning is OrgtilknytningState =>
            tilknytning !== null,
        )
    : [];

  const orgEnhetIder = [
    ...new Set(orgtilknytninger.map((tilknytning) => tilknytning.orgEnhetId)),
  ];

  const orgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(orgEnhetIder),
    queryFn: () => getOrgEnhetList.queryFn(orgEnhetIder),
    enabled: orgEnhetIder.length > 0,
  });

  const orgEnheter = orgEnheterQuery?.data;

  const isLoading =
    getFysiskBrukerstedRessurserQuery.isLoading ||
    orgtilknytningerQuery.isLoading ||
    orgEnheterQuery.isLoading;

  const customRessurser: CustomRessurs[] = isLoading
    ? []
    : ressurser.map((ressurs) => {
        const ressursOrgtilknytning = orgtilknytninger.find(
          (tilknytning) => tilknytning?.navident === ressurs.navident,
        );

        const customOrgEnhet = ressursOrgtilknytning
          ? {
              id: ressursOrgtilknytning.orgEnhetId,
              name:
                orgEnheter?.find(
                  (orgEnhet) =>
                    orgEnhet.id === ressursOrgtilknytning.orgEnhetId,
                )?.navn ?? "",
            }
          : null;

        return {
          navident: ressurs.navident,
          visningsnavn: ressurs.visningsnavn,
          sektor: ressurs.sektor,
          ...(customOrgEnhet && { orgEnhet: customOrgEnhet }),
        };
      });

  const filteredRessurser = customRessurser.filter((ressurs) => {
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

  function handleFysiskBrukerstedFraChange(value: string | null) {
    setFysiskBrukerstedFra(value ?? "");

    setValgteRessurser([]);
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

  const navigate = useNavigate();

  const masseflyttingFysiskBrukerstedMutation = useMutation<
    unknown,
    AxiosError<Error>,
    UpdatePrimaryFysiskBrukerstedForRessurserRequest
  >({
    mutationFn: async (request) => {
      return updateFysiskBrukerstedForRessurser(request);
    },
    onSuccess: async () => {
      navigate({
        to: "/",
      }).then();
    },
  });

  const { showModal } = useModal();

  async function handleClick() {
    if (valgteRessurser.length > 0 && !!fysiskBrukerstedTil) {
      await showModal(
        <MasseflyttingFysiskBrukerstedBekreftModal
          ressurser={sortedRessurser}
          tilFysiskBrukersted={fysiskeBrukersteder.find(
            (fysiskBrukersted) =>
              fysiskBrukersted.brukerstedNr === fysiskBrukerstedTil,
          )}
          valgteRessurser={valgteRessurser}
        />,
      )
        .then(() => {
          masseflyttingFysiskBrukerstedMutation.mutate(
            lagFysiskBrukerstedMasseFlyttingRequest(
              valgteRessurser,
              fysiskBrukerstedTil,
            ),
          );
        })
        .catch(() => {});
    }
  }

  function toggleValgtRessurs(value: string, state: boolean) {
    setValgteRessurser((liste) =>
      state
        ? [...liste, value]
        : liste.filter((navident) => navident !== value),
    );
  }

  if (fysiskeBrukerstederQuery.isLoading) {
    return (
      <div
        css={css`
          justify-self: center;
        `}
      >
        <Loader size="3xlarge" variant="neutral" />
      </div>
    );
  }

  const brukersted = fysiskeBrukersteder?.find(
    (fysiskBrukersted) =>
      fysiskBrukersted?.brukerstedNr === fysiskBrukerstedFra,
  );

  return (
    <VStack gap="4">
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
          Flytt fra
        </Heading>

        {!fysiskBrukerstedFra && (
          <div>
            <FysiskBrukerstedSelect
              description="Velg hvilket fysisk brukersted medarbeiderne skal flyttes fra"
              fysiskeBrukersteder={fysiskeBrukersteder}
              onChange={(value) => setSelectedFysiskBrukerstedFra(value ?? "")}
              value={selectedFysiskBrukerstedFra}
            />

            <Button
              css={css`
                margin-top: var(--a-spacing-4);
                float: right;
              `}
              disabled={!selectedFysiskBrukerstedFra}
              onClick={() =>
                handleFysiskBrukerstedFraChange(selectedFysiskBrukerstedFra)
              }
              size="small"
            >
              Velg
            </Button>
          </div>
        )}

        {!!fysiskBrukerstedFra && (
          <div
            css={css`
              display: flex;
              justify-content: space-between;
            `}
          >
            <BodyShort
              css={css`
                font-size: var(--a-font-size-large);
              `}
            >
              {brukersted
                ? `${brukersted.brukerstedNr}: ${capitalizeNameFysiskBrukersted(brukersted.brukerstedNavn)}`
                : ""}
            </BodyShort>

            <Button
              onClick={() => setFysiskBrukerstedFra("")}
              size="small"
              variant="secondary"
            >
              Endre
            </Button>
          </div>
        )}
      </VStack>

      {!!fysiskBrukerstedFra && (
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
              Flytt til
            </Heading>

            <FysiskBrukerstedSelect
              description="Velg hvilket fysisk brukersted medarbeiderne skal flyttes til"
              fysiskeBrukersteder={fysiskeBrukersteder.filter(
                (fysiskBrukersted) =>
                  fysiskBrukersted.brukerstedNr !== fysiskBrukerstedFra,
              )}
              onChange={(value) => setFysiskBrukerstedTil(value ?? "")}
              value={fysiskBrukerstedTil}
            />

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
                disabled={
                  valgteRessurser.length === 0 ||
                  !fysiskBrukerstedTil ||
                  fysiskBrukerstedTil === fysiskBrukerstedFra
                }
                onClick={handleClick}
                size="small"
              >
                Flytt medarbeidere
              </Button>
            </div>
          </VStack>

          <div
            css={css`
              margin-left: var(--a-spacing-2);
              display: flex;
            `}
          >
            <div>
              <Heading
                css={css`
                  margin-top: var(--a-spacing-2);
                  margin-bottom: var(--a-spacing-4);
                `}
                size="small"
              >
                {`Medarbeidere i ${
                  brukersted
                    ? `${brukersted.brukerstedNr}: ${capitalizeNameFysiskBrukersted(brukersted.brukerstedNavn)}:`
                    : ""
                }`}
              </Heading>

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
                    toggleSektorFilter(
                      Sektor.NAV_KOMMUNAL,
                      event.target.checked,
                    )
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
                <Table.ColumnHeader scope="col" sortKey="orgEnhet.id" sortable>
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
                    Alle
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
                >
                  <Table.DataCell>
                    <SmartLink
                      href={`/ressurs/${ressurs.navident}`}
                      target="_blank"
                    >
                      {ressurs.navident}
                    </SmartLink>
                  </Table.DataCell>
                  <Table.DataCell>
                    <SmartLink
                      href={`/ressurs/${ressurs.navident}`}
                      target="_blank"
                    >
                      {formatVisningsnavn(ressurs.visningsnavn)}
                    </SmartLink>
                  </Table.DataCell>
                  <Table.DataCell>
                    {SektorNameMapping[ressurs.sektor]}
                  </Table.DataCell>
                  <Table.DataCell>
                    {ressurs.orgEnhet && (
                      <SmartLink
                        href={`/org/${ressurs.orgEnhet.id}`}
                        target="_blank"
                      >
                        {`${ressurs.orgEnhet.name} (${ressurs.orgEnhet.id})`}
                      </SmartLink>
                    )}
                  </Table.DataCell>
                  <Table.DataCell>
                    <Checkbox
                      checked={valgteRessurser.includes(ressurs.navident)}
                      hideLabel
                      onChange={(event) =>
                        toggleValgtRessurs(
                          ressurs.navident,
                          event.target.checked,
                        )
                      }
                    >
                      Velg
                    </Checkbox>
                  </Table.DataCell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table>

          {isLoading && (
            <div
              css={css`
                margin: auto;
                justify-content: center;
              `}
            >
              <Loader size="3xlarge" variant="neutral" />
            </div>
          )}
        </>
      )}
    </VStack>
  );
}

function lagFysiskBrukerstedMasseFlyttingRequest(
  valgteRessurser: string[],
  tilFysiskBrukersted: string,
): UpdatePrimaryFysiskBrukerstedForRessurserRequest {
  return {
    navidenter: valgteRessurser,
    newPrimaryFysiskBrukersted: tilFysiskBrukersted,
  };
}

function MasseflyttingFysiskBrukerstedBekreftModal({
  onCancel,
  onConfirm,
  valgteRessurser,
  ressurser,
  tilFysiskBrukersted,
}: {
  onCancel?: () => void;
  onConfirm?: () => void;
  valgteRessurser: string[];
  ressurser: CustomRessurs[];
  tilFysiskBrukersted: FysiskBrukersted | undefined;
}) {
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
        blir flyttet til fysisk brukersted{" "}
        <span
          css={css`
            flex-wrap: nowrap;
            display: flex;
          `}
        >
          <b>
            {tilFysiskBrukersted
              ? `${capitalizeNameFysiskBrukersted(tilFysiskBrukersted.brukerstedNavn)} (${tilFysiskBrukersted.brukerstedNr})`
              : ""}
          </b>
          :
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

interface CustomRessurs {
  navident: string;
  visningsnavn: Visningsnavn;
  sektor: Sektor;
  orgEnhet?: CustomOrgEnhet;
}

interface CustomOrgEnhet {
  id: string;
  name: string;
}
