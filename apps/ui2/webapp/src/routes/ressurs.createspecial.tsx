import { createFileRoute } from "@tanstack/react-router";

import { CreateSpecialResourcePage } from "~/pages/CreateResourcePage/CreateSpecialResourcePage";

export const Route = createFileRoute("/ressurs/createspecial")({
  component: CreateSpecialResourcePage,
});
