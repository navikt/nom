import { css } from "@emotion/react";
import { Alert, Radio, RadioGroup, Select } from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { useState } from "react";

import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";
import { Sektor, Tilknytningtype } from "~/types/api-models";

export const SektorNameMapping = {
  [Sektor.EKSTERN]: "Ekstern",
  [Sektor.NAV_KOMMUNAL]: "Kommunal",
  [Sektor.NAV_STATLIG]: "Statlig",
};

export const TilknytningtypeNameMapping = {
  [Tilknytningtype.KONSULENT]: "Konsulent (standard)",
  [Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER]: "Ekstern samarbeidspartner",
  [Tilknytningtype.INGEN]: "Ingen",
};

export function SektorStep() {
  const { sektor, tilknytningtype } = useSearch({
    from: "/ressurs/create/$step",
  });
  const [selectedSektor, setSelectedSektor] = useState(
    sektor ?? Sektor.EKSTERN,
  );
  const [selectedTilknytningtype, setSelectedTilknytningtype] = useState(
    tilknytningtype ?? Tilknytningtype.KONSULENT,
  );
  const navigate = useNavigate();

  return (
    <ContainerWithHighlightedTop icon={opprettRessursIcon} title="Sektor">
      <Alert variant="info">
        For øyeblikket kan du kun bruke NOM til å opprette eksterne konsulenter
        og kommunalt ansatte. Statlig ansatte opprettes i Remedy som tidligere.
        Dette gjelder også roboter, Vikafossen og utenlandske konsulenter.
      </Alert>

      <form
        onSubmit={(event) => {
          event.preventDefault();

          // TilknytningsType gir ikke mening for kommunale, derfor vises ikke dette som et valg i gui, og i state lagrer vi dette som "INGEN"
          const tilknytningtype =
            selectedSektor === Sektor.NAV_KOMMUNAL
              ? Tilknytningtype.INGEN
              : selectedTilknytningtype;

          navigate({
            to: "/ressurs/create/$step",
            params: { step: "3" },
            search: (oldSearch) => ({
              ...oldSearch,
              sektor: selectedSektor,
              tilknytningtype,
            }),
          });
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
          <Radio value={Sektor.EKSTERN}>
            {SektorNameMapping[Sektor.EKSTERN]}
          </Radio>
          <Radio value={Sektor.NAV_KOMMUNAL}>
            {SektorNameMapping[Sektor.NAV_KOMMUNAL]}
          </Radio>
          <Radio disabled value={Sektor.NAV_STATLIG}>
            {`${SektorNameMapping[Sektor.NAV_STATLIG]} (Ikke tilgjengelig ennå)`}
          </Radio>
        </RadioGroup>
        {selectedSektor === Sektor.NAV_KOMMUNAL ? undefined : (
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
            <option value={Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER}>
              {
                TilknytningtypeNameMapping[
                  Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER
                ]
              }
            </option>
          </Select>
        )}
        <NavigationButtons />
      </form>
    </ContainerWithHighlightedTop>
  );
}
