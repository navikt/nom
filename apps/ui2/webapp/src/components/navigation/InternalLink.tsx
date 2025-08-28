import { css } from "@emotion/react";
import { Link } from "@tanstack/react-router";

export function InternalLink({
  to,
  children,
  title,
  rel = "noopener noreferrer",
  target,
  className,
}: {
  to: string;
  children: any;
  title?: string;
  rel?: string;
  target?: string;
  className?: string;
}) {
  return (
    <Link
      className={className}
      css={css`
        :hover {
          text-decoration: none;
          width: fit-content;
        }
      `}
      rel={rel}
      target={target ?? undefined}
      title={title}
      to={to}
    >
      {children}
    </Link>
  );
}
