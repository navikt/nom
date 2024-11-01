import { Alert, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { OrgtilknytningMedFlereDOLSammePeriode } from "~/sanitychecks/Orgtilknytning/OrgtilknytningMedFlereDOLSammePeriode.tsx";
import { OrgtilknytningNegativPeriode } from "~/sanitychecks/Orgtilknytning/OrgtilknytningNegativPeriode.tsx";
import { OrgtilknytningOrgGyldighet } from "~/sanitychecks/Orgtilknytning/OrgtilknytningOrgGyldighet.tsx";

export const Route = createFileRoute("/sanity-checks/orgtilknytning")({
  component: () => {
    return (
      <>
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgtilknytningSanityChecks />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function ListSelector(props: { selected: string }) {
  switch (props.selected) {
    case "OrgtilknytningOrgGyldighet": {
      return <OrgtilknytningOrgGyldighet />;
    }
    case "OrgtilknytningNegativPeriode": {
      return <OrgtilknytningNegativPeriode />;
    }
    case "OrgtilknytningMedFlereDOLSammePeriode": {
      return <OrgtilknytningMedFlereDOLSammePeriode />;
    }
    default: {
      return <></>;
    }
  }
}

function OrgtilknytningSanityChecks() {
  const [selected, setSelected] = useState("OrgtilknytningTabell");
  return (
    <div style={{ paddingTop: "20px" }}>
      <Select
        defaultValue="OrgtilknytningTabell"
        label="Velg sanity check"
        onChange={(e) => setSelected(e.target.value)}
        style={{ width: "650px", paddingBottom: "20px" }}
      >
        <option value="OrgtilknytningOrgGyldighet">
          List tilknytninger med gyldighet utover enhetens gyldighetsperiode
        </option>
        <option value="OrgtilknytningNegativPeriode">
          List orgtilknytninger med negativ periode (gyldigTom tidligere enn
          gyldigFom)
        </option>
        <option value="OrgtilknytningMedFlereDOLSammePeriode">
          List orgtilknytninger med flere dagligOppfolging i nåværende periode
        </option>
      </Select>
      <ListSelector selected={selected} />
    </div>
  );
}
