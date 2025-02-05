import { Alert, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { OrgtilknytningDuplikat } from "~/sanitychecks/Orgtilknytning/OrgtilknytningDuplikat.tsx";
import { OrgtilknytningMedFlereDOLSammePeriode } from "~/sanitychecks/Orgtilknytning/OrgtilknytningMedFlereDOLSammePeriode.tsx";
import { OrgtilknytningMedTilknytningtypeUtenSektor } from "~/sanitychecks/Orgtilknytning/OrgtilknytningMedTilknytningtypeUtenSektor.tsx";
import { OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon } from "~/sanitychecks/Orgtilknytning/OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon.tsx";
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
    case "OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon": {
      return <OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon />;
    }
    case "OrgtilknytningMedTilknytningtypeUtenSektor": {
      return <OrgtilknytningMedTilknytningtypeUtenSektor />;
    }
    case "OrgtilknytningDuplikat": {
      return <OrgtilknytningDuplikat />;
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
        <option value="OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon">
          List orgtilknytninger med ugyldig sektor-tilknytningtype kombinasjon
        </option>
        <option value="OrgtilknytningMedTilknytningtypeUtenSektor">
          List alle orgtilknytninger med tilknytningtype, men uten sektor
        </option>
        <option value="OrgtilknytningDuplikat">
          List duplikate orgtilknytninger
        </option>
      </Select>
      <ListSelector selected={selected} />
    </div>
  );
}
