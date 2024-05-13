import { css } from "@emotion/react";

export function TestBanner() {
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
          TEST
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
