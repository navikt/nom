import { css } from "@emotion/react";
import { Alert, Radio, RadioGroup, Select } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { useState } from "react";

import { getOrgEnhet, getOrgTilknytninger } from "~/api/react-query-endpoints";
import { useGetSession, useHasAdminAccess } from "~/api/useGetSession";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";
import { OrgEnhetsType, Sektor, Tilknytningtype } from "~/types/api-models";
import { isGyldighetsPeriodeActive, isInDevelopment } from "~/utils";

export const SektorNameMapping = {
  [Sektor.EKSTERN]: "Ekstern",
  [Sektor.NAV_KOMMUNAL]: "Kommunal",
  [Sektor.NAV_STATLIG]: "Statlig",
};

export const TilknytningtypeNameMapping = {
  [Tilknytningtype.KONSULENT]: "Konsulent (standard)",
  [Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER]:
    "Ekstern samarbeidspartner (For eksempel ansatte i Helfo, Skatteetaten og Riksrevisjonen)",
  [Tilknytningtype.INGEN]: "Ingen",
};

export function SektorStep() {
  const { sektor, tilknytningtype } = useSearch({
    from: "/ressurs/create/$step",
  });
  const isDevelopment = isInDevelopment();
  const hasAdminAccess = useHasAdminAccess();
  const navident = useGetSession().data?.navident;
  const dinOrgenhetId = useQuery({
    queryKey: getOrgTilknytninger.queryKey(navident as string),
    queryFn: () => getOrgTilknytninger.queryFn(navident as string),
    select: (organiseringer) =>
      organiseringer
        .filter((organisering) =>
          isGyldighetsPeriodeActive(
            organisering.gyldigFom,
            organisering.gyldigTom,
          ),
        )
        .find((organisering) => organisering.erDagligOppfolging)?.orgEnhetId,
  }).data;

  const brukerJobberIDirektoratet = useQuery({
    queryKey: getOrgEnhet.queryKey(dinOrgenhetId as string),
    queryFn: () => getOrgEnhet.queryFn(dinOrgenhetId as string),
    enabled: !!dinOrgenhetId,
    select: (orgEnhet) => orgEnhet.orgEnhetsType === OrgEnhetsType.DIREKTORAT,
  }).data;

  const [selectedSektor, setSelectedSektor] = useState(
    sektor ?? Sektor.EKSTERN,
  );
  const [selectedTilknytningtype, setSelectedTilknytningtype] = useState(
    tilknytningtype ?? Tilknytningtype.KONSULENT,
  );
  const navigate = useNavigate();

  const saveValuesAndNavigate = (step: string) => {
    // TilknytningsType gir ikke mening for kommunale, derfor vises ikke dette som et valg i gui, og i state lagrer vi dette som "INGEN"
    const tilknytningtype =
      selectedSektor === Sektor.NAV_KOMMUNAL
        ? Tilknytningtype.INGEN
        : selectedTilknytningtype;

    navigate({
      to: "/ressurs/create/$step",
      params: { step },
      search: (oldSearch) => ({
        ...oldSearch,
        sektor: selectedSektor,
        tilknytningtype,
      }),
    }).then();
  };
  const canEditStatlige = hasAdminAccess && isDevelopment;
  return (
    <ContainerWithHighlightedTop icon={opprettRessursIcon} title="Sektor">
      <Alert variant="info">
        For øyeblikket kan du kun bruke NOM til å opprette kommunalt ansatte og
        eksterne konsulenter. Eksterne konsulenter er ansatte i konsulentselskap
        som selger tjenester til NAV. Studenter, personer i arbeidstrening og
        andre eksterne opprettes som tidligere. Statlige ansatte opprettes i
        Identrutina og Unit4 som tidligere.
      </Alert>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          saveValuesAndNavigate("3");
        }}
      >
        <RadioGroup
          css={css`
            margin-bottom: var(--a-spacing-4);
          `}
          data-cy="sektor-radio"
          legend="Velg hvilken sektor medarbeideren skal tilhøre"
          name="sektor"
          onChange={(value) => setSelectedSektor(value)}
          value={selectedSektor}
        >
          <Radio disabled={!canEditStatlige} value={Sektor.NAV_STATLIG}>
            {canEditStatlige
              ? SektorNameMapping[Sektor.NAV_STATLIG]
              : `${SektorNameMapping[Sektor.NAV_STATLIG]} (Ikke tilgjengelig ennå)`}
          </Radio>
          <Radio value={Sektor.NAV_KOMMUNAL}>
            {SektorNameMapping[Sektor.NAV_KOMMUNAL]}
          </Radio>
          <Radio value={Sektor.EKSTERN}>
            {SektorNameMapping[Sektor.EKSTERN]}
          </Radio>
        </RadioGroup>
        {selectedSektor === Sektor.NAV_KOMMUNAL ||
        selectedSektor === Sektor.NAV_STATLIG ? undefined : (
          <Select
            label="Velg hvilken kategori medarbeideren tilhører innenfor denne sektoren"
            name="tilknytningtype"
            onChange={(event) =>
              setSelectedTilknytningtype(event.target.value as Tilknytningtype)
            }
            value={selectedTilknytningtype}
          >
            <option value={Tilknytningtype.KONSULENT}>
              {TilknytningtypeNameMapping[Tilknytningtype.KONSULENT]}
            </option>
            {brukerJobberIDirektoratet && (
              <option value={Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER}>
                {
                  TilknytningtypeNameMapping[
                    Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER
                  ]
                }
              </option>
            )}
          </Select>
        )}
        <NavigationButtons goBackCallback={() => saveValuesAndNavigate("1")} />
      </form>
    </ContainerWithHighlightedTop>
  );
}
