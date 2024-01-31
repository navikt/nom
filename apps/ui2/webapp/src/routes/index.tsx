import { createFileRoute, Navigate } from "@tanstack/react-router";
import React from "react";

import { isInDevelopment } from "~/utils";

const rootOrgEnhet = isInDevelopment() ? "sa312u" : "py437s";

export const Route = createFileRoute("/")({
  component: () => (
    <Navigate from="/" params={{ id: rootOrgEnhet }} to={"/org/$id"} />
  ),
});
