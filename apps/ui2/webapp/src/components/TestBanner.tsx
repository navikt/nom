import { css } from "@emotion/react";

import { useGetSession } from "~/api/useGetSession";

export function TestBanner() {
  const session = useGetSession();

  if (window.location.href.includes("nom.ekstern.dev.nav.no")) {
    return (
      <>
        <div
          css={css`
            width: 100%;
            background: var(--a-orange-300);
            text-align: center;
            padding: 0.5rem;
            position: fixed;
            z-index: 10;
          `}
        >
          TEST - {session.data?.navident}
        </div>
        <div
          css={css`
            height: 40px;
          `}
        />
      </>
    );
  }

  return;
}
