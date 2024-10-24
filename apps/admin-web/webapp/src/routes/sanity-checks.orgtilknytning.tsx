import { Alert, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { OrgtilknytningMedOrgEnhetTabell } from "~/sanitychecks/Orgtilknytning/OrgtilknytningMedOrgEnhetTabell.tsx";

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
    case "OrgtilknytningTabell": {
      return <OrgtilknytningMedOrgEnhetTabell />;
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
        style={{ width: "600px", paddingBottom: "20px" }}
      >
        <option value="OrgtilknytningTabell">
          List tilknytninger med gyldighet utover enhetens gyldighetsperiode
        </option>
      </Select>
      <ListSelector selected={selected} />
    </div>
  );
}
