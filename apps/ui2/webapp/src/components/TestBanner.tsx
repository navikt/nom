import { css } from "@emotion/react";
import { useQuery } from "@tanstack/react-query";

import { getRessurs } from "~/api/react-query-endpoints";
import { useGetSession } from "~/api/useGetSession";
import { isInDevelopment } from "~/utils";

export function TestBanner() {
  const inDevelopment = isInDevelopment();

  const session = useGetSession();

  const navident = session.data?.navident;

  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(navident ?? ""),
    queryFn: () => getRessurs.queryFn(navident ?? ""),
    enabled: !!navident,
  });

  const ressurs = ressursQuery.data;

  if (inDevelopment) {
    return (
      <div
        css={css`
          width: 100%;
          box-sizing: border-box;
          background: var(--a-orange-300);
          text-align: center;
          padding: 0.5rem;
          position: sticky;
          top: 0;
          z-index: 1000;
          font-size: 24px;
        `}
      >
        {`TEST - ${ressurs ? ressurs.epost : `${session.data?.name} (${navident})`}`}
      </div>
    );
  }

  return;
}
