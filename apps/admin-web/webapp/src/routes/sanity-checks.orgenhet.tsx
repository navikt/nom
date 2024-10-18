import { Alert, Select } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import {
  getEnheterUtenOverenheter,
  getEnheterUtenOverenheterMedUnderEnheter,
} from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { AntallOrgenheter } from "~/sanitychecks/OrgEnhet/AntallOrgenheter.tsx";
import { EnheterOgOrganisering } from "~/sanitychecks/OrgEnhet/EnheterOgOrganisering.tsx";
import { OrgEnhetOrganiseringTabell } from "~/sanitychecks/OrgEnhet/OrgEnhetOrganiseringTabell.tsx";

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
    case "Enheter uten over enheter": {
      const orgEnhetOrganisering = useQuery(getEnheterUtenOverenheter());
      return (
        <OrgEnhetOrganiseringTabell
          orgEnhetOrganisering={orgEnhetOrganisering}
        />
      );
    }
    case "Enheter uten over med under": {
      const orgEnhetOrganisering = useQuery(
        getEnheterUtenOverenheterMedUnderEnheter(),
      );
      return (
        <OrgEnhetOrganiseringTabell
          orgEnhetOrganisering={orgEnhetOrganisering}
        />
      );
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
        <option value="Enheter uten over enheter">
          Enheter uten over enheter
        </option>
        <option value="Enheter uten over med under">
          Enheter uten over enheter, men med under enheter
        </option>
      </Select>
      <ListSelector selected={handleChange} />
    </div>
  );
}
