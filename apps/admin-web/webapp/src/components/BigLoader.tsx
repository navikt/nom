import { Loader } from "@navikt/ds-react";

export function BigLoader() {
  return (
    <div className="flex items-center p-8 flex-col">
      <Loader size="3xlarge" />
      <span>Henter data</span>
    </div>
  );
}
