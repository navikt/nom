import { css } from "@emotion/react";
import { Alert, Radio, RadioGroup, Select } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { useState } from "react";

import { getOrgEnhet, getOrgTilknytninger } from "~/api/react-query-endpoints";
import { useGetSession, useHasAdminAccess } from "~/api/useGetSession";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import opprettRessursIconRoundedBackground from "~/resources/opprettRessursIconRoundedBakground.svg";
import { OrgEnhetsType, Sektor, Tilknytningtype } from "~/types/api-models";
import { isGyldighetsPeriodeActive } from "~/utils";

export const SektorNameMapping = {
  [Sektor.EKSTERN]: "Ekstern",
  [Sektor.NAV_KOMMUNAL]: "Kommunal",
  [Sektor.NAV_STATLIG]: "Statlig",
};

export const TilknytningtypeNameMapping = {
  [Tilknytningtype.FAST]: "Fast",
  [Tilknytningtype.MIDLERTIDIG]: "Midlertidig",
  [Tilknytningtype.PENSJONIST]: "Pensjonist",
  [Tilknytningtype.LAERLING]: "Lærling",
  [Tilknytningtype.KONSULENT]: "Konsulent",
  [Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER]: "Ekstern samarbeidspartner",
  [Tilknytningtype.ARBEIDSTRENING]: "Arbeidstrening",
  [Tilknytningtype.STUDENT]: "Student",
  [Tilknytningtype.JOBBSPESIALIST]: "Jobbspesialist",
  [Tilknytningtype.ANNET]: "Annet",
  [Tilknytningtype.INGEN]: "Ingen",
};

export const tilknytningstyperStatlige = [
  Tilknytningtype.FAST,
  Tilknytningtype.MIDLERTIDIG,
  Tilknytningtype.LAERLING,
  Tilknytningtype.PENSJONIST,
];

export const tilknytningstyperEksterne = [
  Tilknytningtype.KONSULENT,
  Tilknytningtype.STUDENT,
  Tilknytningtype.ARBEIDSTRENING,
  Tilknytningtype.JOBBSPESIALIST,
  Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER,
  Tilknytningtype.ANNET,
];

export function SektorStep() {
  const { sektor, tilknytningtype } = useSearch({
    from: "/ressurs/create/$step",
  });
  const navident = useGetSession().data?.navident;

  const navigate = useNavigate();

  const hasAdminAccess = useHasAdminAccess();

  // TODO: Forbedre logikk; sjekke om navident har redigeringstilgang til enhet i direktoratet
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
    sektor ?? Sektor.NAV_STATLIG,
  );

  const [selectedTilknytningtype, setSelectedTilknytningtype] = useState(
    tilknytningtype ?? sektor === Sektor.NAV_KOMMUNAL
      ? Tilknytningtype.INGEN
      : (sektor === Sektor.NAV_STATLIG
          ? tilknytningstyperStatlige
          : tilknytningstyperEksterne)[0],
  );

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

  const handleSektorChange = (value: Sektor) => {
    setSelectedSektor(value);

    if (value === Sektor.NAV_STATLIG) {
      setSelectedTilknytningtype(tilknytningstyperStatlige[0]);
    } else if (value === Sektor.EKSTERN) {
      setSelectedTilknytningtype(tilknytningstyperEksterne[0]);
    }
  };

  return (
    <ContainerWithHighlightedTop
      icon={opprettRessursIconRoundedBackground}
      title="Sektor"
    >
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
          onChange={handleSektorChange}
          value={selectedSektor}
        >
          <Radio value={Sektor.NAV_STATLIG}>
            {SektorNameMapping[Sektor.NAV_STATLIG]}
          </Radio>
          <Radio value={Sektor.NAV_KOMMUNAL}>
            {SektorNameMapping[Sektor.NAV_KOMMUNAL]}
          </Radio>
          <Radio value={Sektor.EKSTERN}>
            {SektorNameMapping[Sektor.EKSTERN]}
          </Radio>
        </RadioGroup>

        {selectedSektor !== Sektor.NAV_KOMMUNAL && (
          <Select
            label="Velg hvilken kategori medarbeideren tilhører innenfor denne sektoren"
            name="tilknytningtype"
            onChange={(event) =>
              setSelectedTilknytningtype(event.target.value as Tilknytningtype)
            }
            value={selectedTilknytningtype}
          >
            {(selectedSektor === Sektor.EKSTERN
              ? tilknytningstyperEksterne
              : tilknytningstyperStatlige
            ).map((tilknytningstype) => {
              if (
                tilknytningstype ===
                  Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER &&
                !brukerJobberIDirektoratet &&
                !hasAdminAccess
              ) {
                return;
              }

              return (
                <option key={tilknytningstype} value={tilknytningstype}>
                  {`${TilknytningtypeNameMapping[tilknytningstype]}${tilknytningstype === Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER ? " (f.eks. Helfo, Skatteetaten, osv.)" : ""}`}
                </option>
              );
            })}
          </Select>
        )}

        <NavigationButtons goBackCallback={() => saveValuesAndNavigate("1")} />
      </form>
    </ContainerWithHighlightedTop>
  );
}
