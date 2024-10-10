import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Link } from "@navikt/ds-react";
import type { ReactNode } from "react";

export function NomLink({
  path,
  children,
}: {
  path: string;
  children: ReactNode;
}) {
  const prodEnv = window.location.href.includes("nom-admin.ansatt.nav.no");

  return (
    <Link
      href={`https://${prodEnv ? "nom.nav.no" : "nom.ekstern.dev.nav.no"}/${path}`}
      target="_blank"
    >
      {children}
      <ExternalLinkIcon />
    </Link>
  );
}
