import { Alert, Select } from "@navikt/ds-react";
import { createFileRoute } from "@tanstack/react-router";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { BigLoader } from "~/components/BigLoader.tsx";
import { OrgtilknytningDuplikat } from "~/sanitychecks/Orgtilknytning/OrgtilknytningDuplikat.tsx";
import { OrgtilknytningerUtenforHierarki } from "~/sanitychecks/Orgtilknytning/OrgtilknytningerUtenforHierarki.tsx";
import { OrgtilknytningMedFlereDOLSammePeriode } from "~/sanitychecks/Orgtilknytning/OrgtilknytningMedFlereDOLSammePeriode.tsx";
import { OrgtilknytningMedTilknytningtypeUtenSektor } from "~/sanitychecks/Orgtilknytning/OrgtilknytningMedTilknytningtypeUtenSektor.tsx";
import { OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon } from "~/sanitychecks/Orgtilknytning/OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon.tsx";
import { OrgtilknytningNegativPeriode } from "~/sanitychecks/Orgtilknytning/OrgtilknytningNegativPeriode.tsx";
import { OrgtilknytningOrgGyldighet } from "~/sanitychecks/Orgtilknytning/OrgtilknytningOrgGyldighet.tsx";
import { OrgtilknytningOverlapp } from "~/sanitychecks/Orgtilknytning/OrgtilknytningOverlapp.tsx";
import { OrgtilknytningSluttdatoForstdagIManed } from "~/sanitychecks/Orgtilknytning/OrgtilknytningSluttdatoForstdagIManed.tsx";

export const Route = createFileRoute("/sanity-checks/orgtilknytning")({
  component: () => {
    return (
      <>
        <ErrorBoundary fallback={<Alert variant="error">Noe gikk galt</Alert>}>
          <Suspense fallback={<BigLoader />}>
            <OrgtilknytningSanityChecks />
          </Suspense>
        </ErrorBoundary>
      </>
    );
  },
});

function ListSelector(props: { selected: string }) {
  switch (props.selected) {
    case "OrgtilknytningSluttdatoForstdagIManed": {
      return <OrgtilknytningSluttdatoForstdagIManed />;
    }
    case "OrgtilknytningOrgGyldighet": {
      return <OrgtilknytningOrgGyldighet />;
    }
    case "OrgtilknytningNegativPeriode": {
      return <OrgtilknytningNegativPeriode />;
    }
    case "OrgtilknytningMedFlereDOLSammePeriode": {
      return <OrgtilknytningMedFlereDOLSammePeriode />;
    }
    case "OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon": {
      return <OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon />;
    }
    case "OrgtilknytningMedTilknytningtypeUtenSektor": {
      return <OrgtilknytningMedTilknytningtypeUtenSektor />;
    }
    case "OrgtilknytningDuplikat": {
      return <OrgtilknytningDuplikat />;
    }
    case "OrgtilknytningOverlapp": {
      return <OrgtilknytningOverlapp />;
    }
    case "OrgtilknytningerUtenforHierarki": {
      return <OrgtilknytningerUtenforHierarki />;
    }
    default: {
      return <></>;
    }
  }
}

function OrgtilknytningSanityChecks() {
  const [selected, setSelected] = useState("OrgtilknytningTabell");
  return (
    <div style={{ paddingTop: "20px" }}>
      <Select
        defaultValue="OrgtilknytningTabell"
        label="Velg sanity check"
        onChange={(e) => setSelected(e.target.value)}
        style={{ width: "650px", paddingBottom: "20px" }}
      >
        <option value="OrgtilknytningSluttdatoForstdagIManed">
          List tilknytninger med sluttdato på først dag i måneden
        </option>
        <option value="OrgtilknytningOrgGyldighet">
          List tilknytninger med gyldighet utover enhetens gyldighetsperiode
        </option>
        <option value="OrgtilknytningNegativPeriode">
          List orgtilknytninger med negativ periode (gyldigTom tidligere enn
          gyldigFom)
        </option>
        <option value="OrgtilknytningMedFlereDOLSammePeriode">
          List orgtilknytninger med flere dagligOppfolging i nåværende periode
        </option>
        <option value="OrgtilknytningMedUgyldigSektorTilknytningtypeKombinasjon">
          List orgtilknytninger med ugyldig sektor-tilknytningtype kombinasjon
        </option>
        <option value="OrgtilknytningMedTilknytningtypeUtenSektor">
          List alle orgtilknytninger med tilknytningtype, men uten sektor
        </option>
        <option value="OrgtilknytningDuplikat">
          List duplikate orgtilknytninger på samme orgenhet og navident
        </option>
        <option value="OrgtilknytningOverlapp">
          List orgtilknytninger med overlappende DOL i aktiv eller fremtidig
          periode
        </option>
        <option value="OrgtilknytningerUtenforHierarki">
          List orgtilknytninger som henger på enhet utenfor hierarki
        </option>
      </Select>
      <ListSelector selected={selected} />
    </div>
  );
}
