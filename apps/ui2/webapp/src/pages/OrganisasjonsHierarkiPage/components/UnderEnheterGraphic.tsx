import { css } from "@emotion/react";

import type { OrgEnhet } from "~/types/api-models";

export function UnderEnheterGraphic({ valgtEnhet }: { valgtEnhet: OrgEnhet }) {
  return (
    <div
      css={css`
        display: flex;
        align-items: flex-end;
        gap: var(--a-spacing-2);
        margin-left: 65px;
        font-weight: var(--a-font-weight-bold);
        margin-bottom: var(--a-spacing-2);
      `}
    >
      <div
        css={css`
          width: 24px;
          height: 40px;
          margin-bottom: 10px;
          border-left: 6px solid var(--nom-primary);
          border-bottom: 6px solid var(--nom-primary);
        `}
      />
      Organisasjonsenheter under {valgtEnhet.navn}
    </div>
  );
}
