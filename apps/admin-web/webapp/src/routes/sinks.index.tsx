import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/sinks/")({
  loader: () => {
    throw redirect({
      to: "/sinks/ressurs",
      replace: true,
    });
  },
});
