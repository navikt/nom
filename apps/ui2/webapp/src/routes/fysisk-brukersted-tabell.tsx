import { createFileRoute } from "@tanstack/react-router";

import { FysiskBrukerstedTabellPage } from "~/pages/ResourcePage/TableView/FysiskBrukerstedTabellPage";

export const Route = createFileRoute("/fysisk-brukersted-tabell")({
  component: FysiskBrukerstedTabellPage,
});
