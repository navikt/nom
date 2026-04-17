import { Alert, Select, Switch } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { DuplikateRessursHendelser } from "~/sanitychecks/Hendelser/DuplikateRessursHendelser.tsx";
import { RessursDelayedEventComparison } from "~/sanitychecks/Ressurs/RessursDelayedEventComparison.tsx";
import { RessursDuplikat } from "~/sanitychecks/Ressurs/RessursDuplikat.tsx";
import { Ressurser } from "~/sanitychecks/Ressurs/Ressurser.tsx";
import { RessurserMedOverlappendeDOL } from "~/sanitychecks/Ressurs/RessurserMedOverlappendeDOL.tsx";
import { RessursMedBlankMellomnavn } from "~/sanitychecks/Ressurs/RessursMedBlankMellomnavn.tsx";
import { RessursMedOTUtenAktivEnhet } from "~/sanitychecks/Ressurs/RessursMedOTUtenAktivEnhet.tsx";
import { RessursRoboter } from "~/sanitychecks/Ressurs/RessursRoboter.tsx";
import { RessursSluttetLederForOrgenhet } from "~/sanitychecks/Ressurs/RessursSluttetLederForOrgenhet.tsx";
import { RessursSluttetMedAktivOT } from "~/sanitychecks/Ressurs/RessursSluttetMedAktivOT.tsx";
import { RessursTekniskSlettet } from "~/sanitychecks/Ressurs/RessursTekniskSlettet.tsx";
import { RessursUlikSluttdatoOrgtilknytning } from "~/sanitychecks/Ressurs/RessursUlikSluttdatoOrgtilknytning.tsx";
import { RessursUtenOrgtilknytning } from "~/sanitychecks/Ressurs/RessursUtenOrgtilknytning.tsx";

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
            exportTypeNavn="listRessurserTilIkkeAktivEnhet"
            inkluderEksterne={inkluderEksterne}
            inkluderHelfo={inkluderHelfo}
          />
        </>
      );
    }
    case "RessursSluttetMedAktivOT": {
      return <RessursSluttetMedAktivOT />;
    }
    case "RessursMedBlankMellomnavn": {
      return <RessursMedBlankMellomnavn />;
    }
    case "RessursDuplikat": {
      return <RessursDuplikat />;
    }
    case "UlikSluttdatoRessursOrgtilknytning": {
      return <RessursUlikSluttdatoOrgtilknytning />;
    }
    case "RessurserMedOverlappendeDOL": {
      return <RessurserMedOverlappendeDOL />;
    }
    case "RessursUtenOrgtilknytning": {
      return <RessursUtenOrgtilknytning />;
    }
    case "RessursRoboter": {
      return <RessursRoboter />;
    }
    case "RessursDelayedEventComparison": {
      return <RessursDelayedEventComparison />;
    }
    case "RessursSluttetLederForOrgenhet": {
      return <RessursSluttetLederForOrgenhet />;
    }
    case "DuplikateRessurshendelser": {
      return <DuplikateRessursHendelser />;
    }
    case "RessursTekniskSlettet": {
      return <RessursTekniskSlettet />;
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
        style={{ width: "600px", paddingBottom: "20px" }}
      >
        <option value="RessurserOT">Ressurser</option>
        <option value="RessursMedOTUtenAktivEnhet">
          Ressurs med aktiv OT som er inaktiv eller uten organisering
        </option>
        <option value="RessursSluttetMedAktivOT">
          List tilknytninger med gyldighet utover ressurens gyldighetsperiode
        </option>
        <option value="RessursMedBlankMellomnavn">
          Ressurs med blankt mellomnavn
        </option>
        <option value="RessursDuplikat">
          Duplikate Ressurser på samme navident
        </option>
        <option value="UlikSluttdatoRessursOrgtilknytning">
          Ulik sluttdato på ressurs og orgtilknytning
        </option>
        <option value="RessurserMedOverlappendeDOL">
          Ressurser med overlappende DOL
        </option>
        <option value="RessursUtenOrgtilknytning">
          Ressurser uten orgtilknytninger
        </option>
        <option value="RessursRoboter">Robot-ressurser</option>
        <option value="RessursDelayedEventComparison">
          Ressurser med fremtidig endring som ikke er i DelayedEvent
        </option>
        <option value="RessursSluttetLederForOrgenhet">
          Ressurser som har sluttet, men fortsatt står som leder for orgEnhet
        </option>
        <option value="DuplikateRessurshendelser">
          Duplikate ressurs-hendelser
        </option>
      </Select>
      <ListSelector selected={selected} />
    </div>
  );
}
