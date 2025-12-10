import { createFileRoute } from "@tanstack/react-router";

import { KontaktpersonerTabellPage } from "~/pages/Parorendeinfo/KontaktpersonerTabellPage";

export const Route = createFileRoute("/kontaktpersoner-tabell/$id")({
  component: KontaktpersonerTabellPage,
});
