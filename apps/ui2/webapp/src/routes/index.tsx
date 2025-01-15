import { createFileRoute } from "@tanstack/react-router";

import { FrontPage } from "~/pages/FrontPage/FrontPage";

export const Route = createFileRoute("/")({
  component: FrontPage,
});
