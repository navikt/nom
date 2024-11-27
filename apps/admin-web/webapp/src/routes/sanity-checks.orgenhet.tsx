import { Alert, Select } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import {
  getBunEnheterUtenEksakt1ArbeidsomradeIHierarkiet,
  getEnheterUtenOverenheter,
  getEnheterUtenOverenheterMedUnderEnheter,
} from "~/api/queries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { AntallOrgenheter } from "~/sanitychecks/OrgEnhet/AntallOrgenheter.tsx";
import { DuplikateEnheter } from "~/sanitychecks/OrgEnhet/DuplikateEnheter.tsx";
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
    case "DuplikateEnheter": {
      return <DuplikateEnheter />;
    }
    case "Bunn enheter som ikke har eksakt 1 arbeidsområde hierarkiet i stier": {
      const orgEnhetOrganisering = useQuery(
        getBunEnheterUtenEksakt1ArbeidsomradeIHierarkiet(),
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
        style={{ width: "600px", paddingBottom: "20px" }}
      >
        <option value="Alle enheter">Alle enheter</option>
        <option value="Enheter og Organisering">Enheter og Organisering</option>
        <option value="Enheter uten over enheter">
          Enheter uten over enheter
        </option>
        <option value="Enheter uten over med under">
          Enheter uten over enheter, men med under enheter
        </option>
        <option value={"DuplikateEnheter"}>
          List Duplikate enheter på samme unike agressoId
        </option>
      </Select>
      <ListSelector selected={handleChange} />
    </div>
  );
}
