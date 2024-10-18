import { Alert, Select, Switch } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { Ressurser } from "~/sanitychecks/Ressurs/Ressurser.tsx";
import { RessursMedOTUtenAktivEnhet } from "~/sanitychecks/Ressurs/RessursMedOTUtenAktivEnhet.tsx";

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
  const [inkluderEksterne, setInkluderEksterne] = useState(false);
  const [inkluderHelfo, setInkluderHelfo] = useState(false);
  switch (props.selected) {
    case "RessurserOT": {
      return <Ressurser />;
    }
    case "RessursMedOTUtenAktivEnhet": {
      return (
        <>
          <div style={{ display: "flex", justifyContent: "left" }}>
            <div style={{ paddingRight: "20px" }}>
              <Switch onChange={(e) => setInkluderEksterne(e.target.checked)}>
                Inkluder Eksterne
              </Switch>
            </div>
            <div style={{ paddingRight: "20px" }}>
              <Switch onChange={(e) => setInkluderHelfo(e.target.checked)}>
                Inkluder HELFO
              </Switch>
            </div>
          </div>
          <RessursMedOTUtenAktivEnhet
            inkluderEksterne={inkluderEksterne}
            inkluderHelfo={inkluderHelfo}
          />
        </>
      );
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
        <option value="RessursMedOTUtenAktivEnhet">
          Ressurs med aktiv OT som er inaktiv eller uten organisering
        </option>
      </Select>
      <ListSelector selected={selected} />
    </div>
  );
}
