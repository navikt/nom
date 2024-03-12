import { css } from "@emotion/react";
import { Alert, Radio, RadioGroup } from "@navikt/ds-react";
import { useNavigate } from "@tanstack/react-router";
import { useState } from "react";

import {
  BasicSelect,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
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
};

export function SektorStep() {
  const [selectedSektor, setSelectedSektor] = useState(Sektor.EKSTERN);
  const [selectedTilknytningtype, setSelectedTilknytningtype] = useState(
    Tilknytningtype.KONSULENT,
  );
  const navigate = useNavigate();

  const tilknytningtypeOptions = Object.values(Tilknytningtype).map(
    (tilknytningtype) => ({
      label: TilknytningtypeNameMapping[tilknytningtype],
      value: tilknytningtype,
    }),
  );

  return (
    <ContainerWithHighlightedTop icon={opprettRessursIcon} title="Sektor">
      <Alert variant="info">
        For øyeblikket kan du kun bruke NOM til å opprette eksterne konsulenter.
        Statlig og kommunalt ansatte opprettes i Remedy. Dette gjelder også
        roboter, Vikafossen og utenlandske konsulenter.
      </Alert>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          navigate({
            to: "/ressurs/create/$step",
            params: { step: "2" },
            search: (oldSearch) => ({
              ...oldSearch,
              sektor: selectedSektor,
              tilknytningtype: selectedTilknytningtype,
            }),
          });
        }}
      >
        <RadioGroup
          css={css`
            margin-bottom: var(--a-spacing-4);
          `}
          legend="Velg hvilken sektor medarbeideren skal tilhøre"
          name="sektor"
          onChange={(value) => setSelectedSektor(value)}
          value={selectedSektor}
        >
          <Radio value={Sektor.EKSTERN}>
            {SektorNameMapping[Sektor.EKSTERN]}
          </Radio>
          <Radio disabled value={Sektor.NAV_STATLIG}>
            {`${SektorNameMapping[Sektor.NAV_STATLIG]} (Ikke tilgjengelig ennå)`}
          </Radio>
          <Radio disabled value={Sektor.NAV_KOMMUNAL}>
            {`${SektorNameMapping[Sektor.NAV_KOMMUNAL]} (Ikke tilgjengelig ennå)`}
          </Radio>
        </RadioGroup>
        <SelectLayoutWrapper
          htmlFor="tilknytningtype"
          label="Velg hvilken kategori medarbeideren tilhører innenfor denne sektoren"
        >
          <BasicSelect
            inputId="tilknytningtype"
            isClearable={false}
            onChange={(option) =>
              option && setSelectedTilknytningtype(option.value)
            }
            options={tilknytningtypeOptions}
            value={tilknytningtypeOptions.find(
              (option) => option.value === selectedTilknytningtype,
            )}
          />
        </SelectLayoutWrapper>
        <NavigationButtons />
      </form>
    </ContainerWithHighlightedTop>
  );
}
