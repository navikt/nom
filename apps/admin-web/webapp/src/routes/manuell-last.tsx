import { Alert, Button } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import type { KyResponse } from "ky";

import {
  lastOrganiseringerOgLedere,
  lastOrgenheter,
  lastOrgtilknytninger,
  lastOrgtilknytningerForEksterne,
  lastRessurser,
  lastSkyggeOrgtilknytningerForKommunale,
  lastTyperForOrgenheter,
  reberegnNomnivaa,
} from "~/api/apiQueries.ts";

export const Route = createFileRoute("/manuell-last")({
  component: ManuellLastPage,
});

const LASTER = [
  { text: "Last orgenheter", fn: lastOrgenheter },
  { text: "Last ressurser", fn: lastRessurser },
  { text: "Last organiseringer og ledere", fn: lastOrganiseringerOgLedere },
  { text: "Last orgtilknytninger", fn: lastOrgtilknytninger },
  {
    text: "Last eksterne orgtilknytninger",
    fn: lastOrgtilknytningerForEksterne,
  },
  {
    text: "Last skyggeorgtilknytninger for kommunale",
    fn: lastSkyggeOrgtilknytningerForKommunale,
  },
  { text: "Last typer for orgenheter", fn: lastTyperForOrgenheter },
  { text: "Beregn NOM-nivå på nytt", fn: reberegnNomnivaa },
];
function ManuellLastPage() {
  return (
    <div className="grid grid-cols-2 gap-4">
      {LASTER.map(({ fn, text }) => (
        <KjørLast fn={fn} key={text} text={text} />
      ))}
    </div>
  );
}

function KjørLast({
  fn,
  text,
}: {
  fn: () => Promise<KyResponse>;
  text: string;
}) {
  const { status, isPending, error, mutate } = useMutation({
    mutationFn: fn,
  });

  return (
    <>
      <Button loading={isPending} onClick={() => mutate()}>
        {text}
      </Button>
      {(() => {
        switch (status) {
          case "pending":
          case "idle": {
            return <span />;
          }
          case "success": {
            return <Alert variant="success">Kjøring vellykket!</Alert>;
          }
          case "error": {
            return <Alert variant="error">{error.message}</Alert>;
          }
        }
      })()}
    </>
  );
}
