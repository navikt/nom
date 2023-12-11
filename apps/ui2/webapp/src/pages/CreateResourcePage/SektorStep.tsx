import { Alert, Radio, RadioGroup } from "@navikt/ds-react";
import { useNavigate } from "@tanstack/react-router";
import { useState } from "react";

import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { createRessursRoute } from "~/reactRouterRoutes";
import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";
import { Sektor } from "~/types/api-models";

export const SektorNameMapping = {
  [Sektor.EKSTERN]: "Ekstern",
  [Sektor.NAV_KOMMUNAL]: "Kommunal",
  [Sektor.NAV_STATLIG]: "Statlig",
};

export function SektorStep() {
  const [selectedSektor, setSelectedSektor] = useState(Sektor.EKSTERN);
  const navigate = useNavigate();

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
            to: createRessursRoute.to,
            params: { step: "2" },
            search: (oldSearch) => ({ ...oldSearch, sektor: selectedSektor }),
          });
        }}
      >
        <RadioGroup
          legend="Sektor"
          name="sektor"
          onChange={(value) => setSelectedSektor(value)}
          value={selectedSektor}
        >
          <Radio value={Sektor.EKSTERN}>
            {SektorNameMapping[Sektor.EKSTERN]}
          </Radio>
          <Radio disabled value={Sektor.NAV_STATLIG}>
            {SektorNameMapping[Sektor.NAV_STATLIG]}
          </Radio>
          <Radio disabled value={Sektor.NAV_KOMMUNAL}>
            {SektorNameMapping[Sektor.NAV_KOMMUNAL]}
          </Radio>
        </RadioGroup>
        <NavigationButtons />
      </form>
    </ContainerWithHighlightedTop>
  );
}
