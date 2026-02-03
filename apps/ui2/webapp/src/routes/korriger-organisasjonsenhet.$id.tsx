import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/korriger-organisasjonsenhet/$id")({
  component: RouteComponent,
});

function RouteComponent() {
  return <div>Hello "/korriger-organisasjonsenhet/$id"!</div>;
}
