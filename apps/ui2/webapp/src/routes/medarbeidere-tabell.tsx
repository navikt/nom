import { createFileRoute } from "@tanstack/react-router";

import { MedarbeidereTabell } from "~/pages/ResourcePage/TableView/MedarbeidereTabell";

export const Route = createFileRoute("/medarbeidere-tabell")({
  component: MedarbeidereTabell,
});
