import { css } from "@emotion/react";
import { useQuery } from "@tanstack/react-query";

import { getRessurs } from "~/api/react-query-endpoints";
import { useGetSession } from "~/api/useGetSession";

export function TestBanner() {
  const session = useGetSession();

  const navident = session.data?.navident;

  const ressurs = navident
    ? useQuery({
        queryKey: getRessurs.queryKey(navident),
        queryFn: () => getRessurs.queryFn(navident),
      }).data
    : undefined;

  if (globalThis.location.href.includes(".dev.nav.no")) {
    return (
      <>
        <div
          css={css`
            width: 100%;
            background: var(--a-orange-300);
            text-align: center;
            padding: 0.5rem;
            position: fixed;
            z-index: 1;
            font-size: 24px;
          `}
        >
          TEST -{" "}
          {ressurs
            ? ressurs.epost
            : session.data?.name + " (" + session.data?.navident + ")"}
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
