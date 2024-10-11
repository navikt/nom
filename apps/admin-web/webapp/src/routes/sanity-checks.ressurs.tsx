import { Alert, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { Ressurser } from "~/sanitychecks/Ressurser.tsx";

export const Route = createFileRoute("/sanity-checks/ressurs")({
  component: () => {
    return (
      <>
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <RessursSanityChecks />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function ListSelector(props: { selected: string }) {
  switch (props.selected) {
    case "RessurserOT": {
      return <Ressurser />;
    }
    default: {
      return <></>;
    }
  }
}

function RessursSanityChecks() {
  const [selected, setSelected] = useState("RessurserOT");
  return (
    <div style={{ paddingTop: "20px" }}>
      <Select
        defaultValue="RessurserOT"
        label="Velg sanity check"
        onChange={(e) => setSelected(e.target.value)}
        style={{ width: "300px", paddingBottom: "20px" }}
      >
        <option value="RessurserOT">Ressurser</option>
      </Select>
      <ListSelector selected={selected} />
    </div>
  );
}
