import { Alert, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { RemedyCompareDiffPaaAktiv } from "~/sanitychecks/Remedy/RemedyCompareDiffPaaAktiv.tsx";
import { RemedyCompareDiffPaaNavn } from "~/sanitychecks/Remedy/RemedyCompareDiffPaaNavn.tsx";
import { RemedyCompareDiffPaaRemedyEnhet } from "~/sanitychecks/Remedy/RemedyCompareDiffPaaRemedyEnhet.tsx";
import { RemedyCompareDiffPaaSektor } from "~/sanitychecks/Remedy/RemedyCompareDiffPaaSektor.tsx";
import { RemedyCompareDiffPaaSluttdato } from "~/sanitychecks/Remedy/RemedyCompareDiffPaaSluttdato.tsx";
import { RemedyCompareDiffPaaUtlopsdato } from "~/sanitychecks/Remedy/RemedyCompareDiffPaaUtlopsdato.tsx";
import { RemedyCompareRemedyUserNotInNom } from "~/sanitychecks/Remedy/RemedyCompareRemedyUserNotInNom.tsx";
import { RemedyCompareRessurserNotInRemedy } from "~/sanitychecks/Remedy/RemedyCompareRessurserNotInRemedy.tsx";
import { RemedyErrorState } from "~/sanitychecks/Remedy/RemedyErrorState.tsx";

export const Route = createFileRoute("/sanity-checks/remedy")({
  component: () => {
    return (
      <>
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <RemedySanityChecks />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function ListSelector(props: { selected: string }) {
  switch (props.selected) {
    case "errorState": {
      return <RemedyErrorState />;
    }
    case "diffPaaNavn": {
      return <RemedyCompareDiffPaaNavn />;
    }
    case "diffPaaSektor": {
      return <RemedyCompareDiffPaaSektor />;
    }
    case "diffPaaSluttdato": {
      return <RemedyCompareDiffPaaSluttdato />;
    }
    case "diffPaaUtlopsdato": {
      return <RemedyCompareDiffPaaUtlopsdato />;
    }
    case "diffPaaAktiv": {
      return <RemedyCompareDiffPaaAktiv />;
    }
    case "diffPaaRemedyEnhet": {
      return <RemedyCompareDiffPaaRemedyEnhet />;
    }
    case "ressurserNotInRemedy": {
      return <RemedyCompareRessurserNotInRemedy />;
    }
    case "remedyUserNotInNom": {
      return <RemedyCompareRemedyUserNotInNom />;
    }
    default: {
      return <></>;
    }
  }
}

function RemedySanityChecks() {
  const [selected, setSelected] = useState("errorState");
  return (
    <div style={{ paddingTop: "20px" }}>
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          justifyContent: "space-between",
          padding: "0px 20px",
        }}
      >
        <Select
          defaultValue="errorState"
          label="Velg check"
          onChange={(e) => setSelected(e.target.value)}
          style={{ width: "650px", paddingBottom: "20px" }}
        >
          <option value="errorState">
            Error state i NOM-Remedy-Integrasjonen
          </option>
          <option value="diffPaaNavn">Forskjeller i navn</option>
          <option value="diffPaaSektor">Forskjeller i sektor</option>
          <option value="diffPaaSluttdato">Forskjeller i sluttdato</option>
          <option value="diffPaaUtlopsdato">Forskjeller i utløpsdato</option>
          <option value="diffPaaAktiv">Forskjeller i aktiv</option>
          <option value="diffPaaRemedyEnhet">Forskjeller i remedy enhet</option>
          <option value="ressurserNotInRemedy">
            NOM ressurser som ikke finnes i Remedy
          </option>
          <option value="remedyUserNotInNom">
            Remedy bruker som ikke finnes i NOM
          </option>
        </Select>
      </div>
      {selected !== "errorState" && (
        <>
          Alle Remedy dataene er fra{" "}
          <a href="https://nav-it.slack.com/archives/C040RV7KCH5/p1741705426131959?thread_ts=1741699518.939969&cid=C040RV7KCH5">
            SQL dump
          </a>{" "}
          statisk fil lastet til{" "}
          <a href="https://console.cloud.google.com/storage/browser/no-nav-org-prod-nom-admin-storage-bucket;tab=objects?forceOnBucketsSortingFiltering=true&authuser=1&inv=1&invt=Abs4dg&project=org-prod-1016&prefix=&forceOnObjectsSortingFiltering=false">
            GCP bucket
          </a>
          .
        </>
      )}
      <ListSelector selected={selected} />
    </div>
  );
}
