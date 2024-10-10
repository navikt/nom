import { Alert, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { AntallOrgenheter } from "~/sanitychecks/AntallOrgenheter.tsx";
import { EnheterOgOrganisering } from "~/sanitychecks/EnheterOgOrganisering.tsx";

export const Route = createFileRoute("/sanity-checks/orgenhet")({
  component: () => {
    return (
      <>
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgEnhetSanityChecks />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function ListSelector(props: { selected: string }) {
  switch (props.selected) {
    case "Alle enheter": {
      return <AntallOrgenheter />;
    }
    case "Enheter og Organisering": {
      return <EnheterOgOrganisering />;
    }
    default: {
      return <></>;
    }
  }
}

function OrgEnhetSanityChecks() {
  const [handleChange, setHandleChange] = useState("Alle enheter");
  return (
    <div style={{ paddingTop: "20px" }}>
      <Select
        defaultValue="Alle enheter"
        label="Velg sanity check"
        onChange={(e) => setHandleChange(e.target.value)}
        style={{ width: "300px", paddingBottom: "20px" }}
      >
        <option value="Alle enheter">Alle enheter</option>
        <option value="Enheter og Organisering">Enheter og Organisering</option>
      </Select>
      <ListSelector selected={handleChange} />
    </div>
  );
}
