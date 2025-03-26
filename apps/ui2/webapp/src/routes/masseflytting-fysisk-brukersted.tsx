import { createFileRoute } from "@tanstack/react-router";

import { MasseflyttingFysiskBrukersted } from "~/pages/ResourcePage/Masseflytting/MasseflyttingFysiskBrukersted";

export const Route = createFileRoute("/masseflytting-fysisk-brukersted")({
  component: MasseflyttingFysiskBrukersted,
});
