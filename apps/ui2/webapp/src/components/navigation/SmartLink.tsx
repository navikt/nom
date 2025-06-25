import { ExternalLink } from "./ExternalLink";
import { InternalLink } from "./InternalLink";

export function SmartLink({
  href,
  children,
  title,
  rel,
  target,
  inline,
  externalIcon,
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
  const isInternal = href.startsWith("/");

  if (isInternal) {
    return (
      <InternalLink
        className={className}
        rel={rel}
        target={target}
        title={title}
        to={href}
      >
        {children}
      </InternalLink>
    );
  }

  return (
    <ExternalLink
      className={className}
      externalIcon={externalIcon}
      href={href}
      inline={inline}
      rel={rel}
      target={target}
      title={title}
    >
      {children}
    </ExternalLink>
  );
}
