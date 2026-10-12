import { useQuery } from "@tanstack/react-query";

import { getRessursOptions } from "~/api/apiQueries.ts";
import { useGetSession } from "~/api/useGetSession";
import { isInDevelopment } from "~/utils.ts";

export function TestBanner() {
  const inDevelopment = isInDevelopment();

  const session = useGetSession();

  const navident = session.data?.navident;

  const ressursQuery = useQuery({
    ...getRessursOptions(navident ?? ""),
    enabled: !!navident,
  });

  const ressurs = ressursQuery.data;

  if (inDevelopment) {
    return (
      <div
        style={{
          width: "100%",
          boxSizing: "border-box",
          background: "var(--a-orange-300)",
          textAlign: "center",
          padding: "0.5rem",
          position: "sticky",
          top: 0,
          zIndex: 1000,
          fontSize: "24px",
        }}
      >
        {`TEST - ${ressurs ? ressurs.epost : `${session.data?.name} (${navident})`}`}
      </div>
    );
  }

  return;
}
