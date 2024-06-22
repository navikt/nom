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
  return (
    <Link href={`http://nom.nav.no/${path}`} target="_blank">
      {children}
      <ExternalLinkIcon />
    </Link>
  );
}
