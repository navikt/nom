import { TerminalIcon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";
import type { ReactNode } from "react";

export function Trigger({
  onClick,
  children,
}: {
  children: ReactNode;
  onClick: () => void;
}) {
  return (
    <Button icon={<TerminalIcon />} onClick={onClick} variant="secondary">
      {children}
    </Button>
  );
}
