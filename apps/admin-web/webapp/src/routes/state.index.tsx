import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/state/")({
  loader: () => {
    throw redirect({
      to: "/state/ressurs",
      replace: true,
    });
  },
});
