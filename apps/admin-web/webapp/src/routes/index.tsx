import {
  BooksIcon,
  CogRotationIcon,
  DatabaseIcon,
  HeadHeartIcon,
  TerminalIcon,
} from "@navikt/aksel-icons";
import { createFileRoute, Link } from "@tanstack/react-router";
import type { ReactNode } from "react";

import { useHasAdminAccess } from "~/api/useGetSession.tsx";

export const Route = createFileRoute("/")({
  component: () => {
    const hasAdminAccess = useHasAdminAccess();

    return (
      <div className="flex items-center gap-2 justify-center flex-wrap">
        <Link to="/sinks">
          <CardWrapper>
            <BooksIcon fontSize={80} />
            <b>Kafka meldinger</b>
          </CardWrapper>
        </Link>
        <Link to="/state">
          <CardWrapper>
            <DatabaseIcon fontSize={80} />
            <b>State</b>
          </CardWrapper>
        </Link>
        {hasAdminAccess && (
          <>
            <Link to="/manuell-last">
              <CardWrapper>
                <CogRotationIcon fontSize={80} />
                <b>Manuell last</b>
              </CardWrapper>
            </Link>
            <Link to="/manuelle-meldinger">
              <CardWrapper>
                <TerminalIcon fontSize={80} />
                <b>Send event</b>
              </CardWrapper>
            </Link>
            <Link to="/sanity-checks/orgenhet">
              <CardWrapper>
                <HeadHeartIcon fontSize={80} />
                <b>Sanity checks</b>
              </CardWrapper>
            </Link>
          </>
        )}
      </div>
    );
  },
});

function CardWrapper({ children }: { children: ReactNode }) {
  return (
    <div
      className="
    justify-center 
    h-52 w-52
    flex items-center flex-col
    text-2xl
    p-4
    bg-lightblue-100 border-lightblue-600 border-2 rounded-sm
    gap-0 transition-all duration-200 hover:gap-2 hover:text-orange-500 hover:border-orange-500
    "
    >
      {children}
    </div>
  );
}
