import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Link } from "@navikt/ds-react";

export function ExternalLink({
  href,
  children,
  title,
  rel = "noopener noreferrer",
  target = "_blank",
  inline = false,
  externalIcon = false,
  className,
}: {
  href: string;
  children: any;
  title?: string;
  rel?: string;
  target?: string;
  inline?: boolean;
  externalIcon?: boolean;
  className?: string;
}) {
  return (
    <Link
      className={className}
      href={href}
      inlineText={inline}
      rel={rel}
      target={target}
      title={title}
    >
      {children}
      {(externalIcon || inline) && <ExternalLinkIcon />}
    </Link>
  );
}
