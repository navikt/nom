import { createFileRoute } from "@tanstack/react-router";

import { Masseflytting } from "~/pages/ResourcePage/Masseflytting/Masseflytting";

export const Route = createFileRoute("/masseflytting/$id")({
  component: Masseflytting,
});
