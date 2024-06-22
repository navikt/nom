import { BooksIcon, CogRotationIcon, DatabaseIcon } from "@navikt/aksel-icons";
import { createFileRoute, Link } from "@tanstack/react-router";
import type { ReactNode } from "react";
export const Route = createFileRoute("/")({
  component: () => {
    return (
      <div className="flex items-center gap-2 justify-center">
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
        <Link to="/manuell-last">
          <CardWrapper>
            <CogRotationIcon fontSize={80} />
            <b>Manuell last</b>
          </CardWrapper>
        </Link>
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
