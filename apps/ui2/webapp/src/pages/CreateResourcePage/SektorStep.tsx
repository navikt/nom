import { Alert, Radio, RadioGroup } from "@navikt/ds-react";
import { useNavigate } from "@tanstack/react-router";
import { useState } from "react";

import { Sektor } from "~/api/generated/graphql_generated";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { createRessursRoute } from "~/reactRouterRoutes";

export const SektorNameMapping = {
  [Sektor.Ekstern]: "Ekstern",
  [Sektor.NavKommunal]: "Kommunal",
  [Sektor.NavStatlig]: "Statlig",
};

export function SektorStep() {
  const [selectedSektor, setSelectedSektor] = useState(Sektor.Ekstern);
  const navigate = useNavigate();

  return (
    <ContainerWithHighlightedTop title="Sektor">
      <Alert variant="info">
        For øyeblikket kan du kun bruke NOM til å opprette eksterne konsulenter.
        Statlig og kommunalt ansatte opprettes i Remedy. Dette gjelder også
        roboter, Vikafossen og utenlandske konsulenter opprettes i Remedy.
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
          <Radio value={Sektor.Ekstern}>
            {SektorNameMapping[Sektor.Ekstern]}
          </Radio>
          <Radio disabled value={Sektor.NavStatlig}>
            {SektorNameMapping[Sektor.NavStatlig]}
          </Radio>
          <Radio disabled value={Sektor.NavKommunal}>
            {SektorNameMapping[Sektor.NavKommunal]}
          </Radio>
        </RadioGroup>
        <NavigationButtons />
      </form>
    </ContainerWithHighlightedTop>
  );
}
