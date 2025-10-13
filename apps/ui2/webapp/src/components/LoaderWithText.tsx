import { css } from "@emotion/react";
import { BodyShort, Loader } from "@navikt/ds-react";

export function LoaderWithText({ children }: { children?: string }) {
  return (
    <div
      css={css`
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--a-spacing-2);
      `}
    >
      <Loader size="3xlarge" />
      <BodyShort size="large">{children}</BodyShort>
    </div>
  );
}
