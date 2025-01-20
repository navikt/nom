import { Alert, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { IdenterUtenStilling } from "~/sanitychecks/U4/IdenterUtenStilling.tsx";

export const Route = createFileRoute("/sanity-checks/u4diff")({
  component: () => {
    return (
      <>
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <U4DiffSanityChecks />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function ListSelector(props: { selected: string }) {
  switch (props.selected) {
    case "IdenterUtenStilling": {
      return <IdenterUtenStilling />;
    }
    default: {
      return <></>;
    }
  }
}

function U4DiffSanityChecks() {
  const [selected, setSelected] = useState("IdenterUtenStilling");
  return (
    <div style={{ paddingTop: "20px" }}>
      <Select
        defaultValue="IdenterUtenStilling"
        label="Velg U4 check"
        onChange={(e) => setSelected(e.target.value)}
        style={{ width: "650px", paddingBottom: "20px" }}
      >
        <option value="IdenterUtenStilling">Identer uten stilling</option>
      </Select>
      <ListSelector selected={selected} />
    </div>
  );
}
