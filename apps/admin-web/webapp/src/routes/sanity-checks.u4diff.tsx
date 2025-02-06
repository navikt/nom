import { FileExcelIcon } from "@navikt/aksel-icons";
import { Alert, Button, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import {
  alleDifferanserExcelExportOptions,
  identerUtenStillingIU4ExcelExportOptions,
  ikkeKlassifiserteDifferanserExcelExportOptions,
  sektorDifferansePaaRessursExcelExportOptions,
  ulikeEnheterExcelExportOptions,
  ulikeSluttDatoerExcelExportOptions,
  ulikeStartDatoerExcelExportOptions,
} from "~/api/apiQueries.ts";
import { BigLoader } from "~/components/BigLoader.tsx";
import { AlleDifferanser } from "~/sanitychecks/U4/AlleDifferanser.tsx";
import { IdenterUtenStilling } from "~/sanitychecks/U4/IdenterUtenStilling.tsx";
import { IkkeKlassefiserteDifferanser } from "~/sanitychecks/U4/IkkeKlassefiserteDifferanser.tsx";
import { SektorDiffPaaRessurs } from "~/sanitychecks/U4/SektorDiffPaaRessurs.tsx";
import { UlikeEnheter } from "~/sanitychecks/U4/UlikeEnheter.tsx";
import { UlikeSluttDatoer } from "~/sanitychecks/U4/UlikeSluttDatoer.tsx";
import { UlikeStartDatoer } from "~/sanitychecks/U4/UlikeStartDatoer.tsx";

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
    case "sektorDiffPaaRessurs": {
      return <SektorDiffPaaRessurs />;
    }
    case "ulikeEnheter": {
      return <UlikeEnheter />;
    }
    case "ulikeStartDatoer": {
      return <UlikeStartDatoer />;
    }
    case "ulikeSluttDatoer": {
      return <UlikeSluttDatoer />;
    }
    case "ikkeKlassifiserteDifferanser": {
      return <IkkeKlassefiserteDifferanser />;
    }
    case "alleDifferanser": {
      return <AlleDifferanser />;
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
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          justifyContent: "space-between",
          padding: "0px 20px",
        }}
      >
        <Select
          defaultValue="IdenterUtenStilling"
          label="Velg U4 check"
          onChange={(e) => setSelected(e.target.value)}
          style={{ width: "650px", paddingBottom: "20px" }}
        >
          <option value="IdenterUtenStilling">
            Statlige Identer uten stilling i Unit 4
          </option>
          <option value="sektorDiffPaaRessurs">
            Sektor differanse på ressurs
          </option>
          <option value="ulikeEnheter">Ulike enheter mellom NOM og U4</option>
          <option value="ulikeStartDatoer">
            Ulike startdatoer mellom NOM og U4
          </option>
          <option value="ulikeSluttDatoer">
            Ulike sluttdatoer mellom NOM og U4
          </option>
          <option value="ikkeKlassifiserteDifferanser">
            Ikke klassifiserte differanser mellom NOM og U4
          </option>
          <option value="alleDifferanser">
            Alle differanser mellom NOM og U4
          </option>
        </Select>
        <Button
          icon={<FileExcelIcon />}
          onClick={() => {
            switch (selected) {
              case "IdenterUtenStilling": {
                identerUtenStillingIU4ExcelExportOptions();
                break;
              }
              case "sektorDiffPaaRessurs": {
                sektorDifferansePaaRessursExcelExportOptions();
                break;
              }
              case "ulikeEnheter": {
                ulikeEnheterExcelExportOptions();
                break;
              }
              case "ulikeStartDatoer": {
                ulikeStartDatoerExcelExportOptions();
                break;
              }
              case "ulikeSluttDatoer": {
                ulikeSluttDatoerExcelExportOptions();
                break;
              }
              case "ikkeKlassifiserteDifferanser": {
                ikkeKlassifiserteDifferanserExcelExportOptions();
                break;
              }
              case "alleDifferanser": {
                alleDifferanserExcelExportOptions();
                break;
              }
              default: {
                break;
              }
            }
          }}
          style={{ height: "fit-content", alignSelf: "center" }}
          variant="secondary"
        >
          Til Excel
        </Button>
      </div>
      <ListSelector selected={selected} />
    </div>
  );
}
