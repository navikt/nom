import { css } from "@emotion/react";
import { BodyLong } from "@navikt/ds-react";

export function OrgEnhetIkkeFunnet() {
  return (
    <BodyLong
      css={css`
        font-size: var(--a-font-size-xlarge);
      `}
    >
      Organisasjonsenheten ble ikke funnet
    </BodyLong>
  );
}

export function IkkeTilgangRedigering() {
  return (
    <BodyLong
      css={css`
        font-size: var(--a-font-size-xlarge);
      `}
    >
      Du har ikke tilgang til å redigere denne organisasjonsenheten
    </BodyLong>
  );
}
