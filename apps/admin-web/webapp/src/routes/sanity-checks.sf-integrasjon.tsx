import { Alert, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { SfOutboxAvvist } from "~/sanitychecks/SfIntegrasjon/SfOutboxAvvist.tsx";
import { SfOutboxFeilet } from "~/sanitychecks/SfIntegrasjon/SfOutboxFeilet.tsx";

export const Route = createFileRoute("/sanity-checks/sf-integrasjon")({
  component: () => {
    return (
      <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
        <Suspense fallback={<BigLoader />}>
          <SfIntegrasjonSanityChecks />
        </Suspense>
      </ErrorBoundary>
    );
  },
});

function ListSelector(props: { selected: string }) {
  switch (props.selected) {
    case "avvist": {
      return <SfOutboxAvvist />;
    }
    case "feilet": {
      return <SfOutboxFeilet />;
    }
    default: {
      return <></>;
    }
  }
}

function SfIntegrasjonSanityChecks() {
  const [selected, setSelected] = useState("avvist");
  return (
    <div style={{ paddingTop: "20px" }}>
      <Select
        defaultValue="avvist"
        label="Velg check"
        onChange={(e) => setSelected(e.target.value)}
        style={{ width: "650px", paddingBottom: "20px" }}
      >
        <option value="avvist">Avviste rader i outboxen til SF</option>
        <option value="feilet">Feilede rader i outboxen til SF</option>
      </Select>
      <ListSelector selected={selected} />
    </div>
  );
}
